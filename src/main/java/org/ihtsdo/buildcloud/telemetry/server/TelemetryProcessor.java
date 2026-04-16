package org.ihtsdo.buildcloud.telemetry.server;

import org.apache.activemq.transport.TransportDisposedIOException;
import org.apache.commons.lang3.NotImplementedException;
import org.ihtsdo.buildcloud.telemetry.core.Constants;
import org.ihtsdo.otf.resourcemanager.ManualResourceConfiguration;
import org.ihtsdo.otf.resourcemanager.ResourceConfiguration;
import org.ihtsdo.otf.resourcemanager.ResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.jms.IllegalStateException;
import jakarta.jms.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HashMap;
import java.util.Map;
import org.ihtsdo.otf.dao.s3.S3Client;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Service
@ConditionalOnProperty(name = "srs.build.offlineMode", havingValue = "false")
public class TelemetryProcessor {

	private static final Logger LOGGER = LoggerFactory.getLogger(TelemetryProcessor.class);

	private static final int ONE_SECOND = 1000;
	private static final long S3_UPLOAD_INTERVAL_MILLIS = 10_000L;

	private static final String TEMP_DIRECTORY_PATH = "/tmp/telemetry-tmp";

	private final Map<String, BufferedWriter> streamWriters;
	private final Map<String, Runnable> activeS3UploadTasks;
	private final Map<String, Long> activeS3LastUploadMillis;

	private boolean shutdown;
	private final boolean isOffline;

	private final MessageConsumer consumer;

	private final Session jmsSession;

	private final ResourceLoader resourceLoader;
	private final S3Client s3Client;

	@Autowired
	public TelemetryProcessor(final Session jmsSession, final ResourceLoader resourceLoader,
			@Value("${srs.build.offlineMode}") final boolean isOffLine,
			final S3Client s3Client) throws JMSException {
		this.streamWriters = new ConcurrentHashMap<>();
		this.activeS3UploadTasks = new ConcurrentHashMap<>();
		this.activeS3LastUploadMillis = new ConcurrentHashMap<>();
		this.jmsSession = jmsSession;
		this.consumer = jmsSession.createConsumer(jmsSession.createQueue(Constants.QUEUE_RELEASE_EVENTS));
		this.resourceLoader = resourceLoader;
		this.isOffline = isOffLine;
		this.s3Client = s3Client;
		new File(TEMP_DIRECTORY_PATH).mkdirs();
	}

	@PostConstruct
	public void startup() {
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			shutdown = true;
			closeOpenStreams();
		}, "telemetry-processor-shutdown"));
		new Thread(this::doStartUp).start();
	}

	private void doStartUp() {
		boolean printedWaiting = false;
		LOGGER.info("Telemetry server starting up.");
		while (!shutdown) {
			printedWaiting = doStartUp(printedWaiting);
		}
		closeOpenStreams();
	}

	private boolean doStartUp(boolean printedWaiting) {
		try {
			printedWaiting = waitForMessage(printedWaiting);
		} catch (IllegalStateException e) {
			LOGGER.info("Connection closed. Shutting down telemetry consumer.");
			shutdown = true;
			closeOpenStreams();
		} catch (JMSException e) {
			processJMSException(e);
		} catch (IOException e) {
			LOGGER.error("Error occurred while trying to obtain the output stream.", e);
		}
		return printedWaiting;
	}

	private boolean waitForMessage(boolean printedWaiting) throws JMSException, IOException {
		if (!printedWaiting) {
			LOGGER.debug("Waiting for message");
			printedWaiting = true;
		}
		return processMessage(printedWaiting, (TextMessage) consumer.receive(ONE_SECOND));
	}

	private boolean processMessage(boolean printedWaiting, final TextMessage message) throws JMSException, IOException {
		if (message != null) {
			printedWaiting = false;
			LOGGER.debug("Got message '{}', correlationID {}", message.getText(), message.getJMSCorrelationID());
			processCorrelationID(message, message.getJMSCorrelationID());
		}
		return printedWaiting;
	}

	private void processCorrelationID(final TextMessage message, final String correlationID) throws JMSException, IOException {
		if (correlationID != null) {
			processMessageText(message, correlationID, message.getText());
		}
	}

	private void processMessageText(final TextMessage message, final String correlationID, final String text) throws JMSException, IOException {
		if (Constants.START_STREAM.equals(text)) {
			startStream(message, correlationID);
		} else if (Constants.FINISH_STREAM.equals(text)) {
			finishStream(correlationID);
		} else {
			writeMessageTextAndFlush(message, correlationID, text);
		}
	}

	private void writeMessageTextAndFlush(final TextMessage message, final String correlationID, final String text) throws IOException, JMSException {
		final BufferedWriter writer = streamWriters.get(correlationID);
		if (writer != null) {
			writeMessageText(message, text, writer);
			// We need output to disk to be up to the minute, so flush each line
			writer.flush();
			pushS3StreamIfDue(correlationID);
		} else {
			LOGGER.error("Attempting to write to stream but no open stream for correlationID {}.", correlationID);
		}
	}

	private void writeMessageText(final TextMessage message, final String text, final BufferedWriter writer) throws IOException, JMSException {
		writer.write(text);
		final String exception = message.getStringProperty(Constants.EXCEPTION);
		if (exception != null) {
			writer.write(exception);
			writer.write(Constants.LINE_BREAK);
		}
	}

	private void finishStream(final String correlationID) throws IOException {
		final BufferedWriter writer = streamWriters.get(correlationID);
		if (writer != null) {
			writer.close();
			streamWriters.remove(correlationID);
			activeS3UploadTasks.remove(correlationID);
			activeS3LastUploadMillis.remove(correlationID);
		} else {
			LOGGER.error("Attempting to close stream but no open stream for correlationID {}", correlationID);
		}
	}

	private void startStream(final TextMessage message, final String correlationID) throws JMSException, IOException {
		final String streamUri = message.getStringProperty(Constants.STREAM_URI);
		if (streamUri != null) {
			streamWriters.put(correlationID, createStreamWriter(correlationID, streamUri));
		}
	}

	private BufferedWriter createStreamWriter(final String correlationID, final String streamUri) throws IOException {
		final String[] split = streamUri.split("://", 2);
		final String protocol = split[0];
		final String path = split[1];

		if (Constants.FILE.equals(protocol)) {
			// Keep existing log content and append new telemetry entries.
			return new BufferedWriter(new FileWriter(path, true));
		} else if (Constants.s3.equals(protocol)) {
			return createS3StreamWriter(correlationID, path);
		} else {
			throw new NotImplementedException("Unrecognised stream URI protocol: " + protocol);
		}
	}

	private BufferedWriterTaskOnClose createS3StreamWriter(final String correlationID, final String path) throws IOException {
		final String[] split1 = path.split("/", 2);
		final String bucketName = split1[0];
		final String objectKey = split1[1];

		final ResourceManager resourceManager =
				new ResourceManager(new ManualResourceConfiguration(false, true,
				new ResourceConfiguration.Local(), new ResourceConfiguration.Cloud(bucketName, objectKey)),
				resourceLoader);

		final File temporaryFile = new File(TEMP_DIRECTORY_PATH + Constants.SLASH + correlationID);
		prepareTemporaryS3FileForAppend(bucketName, objectKey, temporaryFile);

		activeS3UploadTasks.put(correlationID, () -> uploadTemporaryFile(resourceManager, temporaryFile, false));
		activeS3LastUploadMillis.put(correlationID, System.currentTimeMillis());

		return new BufferedWriterTaskOnClose(new FileWriter(temporaryFile, true), () -> {
			if (!isOffline) {
				uploadTemporaryFile(resourceManager, temporaryFile, true);
			}
		});
	}

	private void prepareTemporaryS3FileForAppend(final String bucketName, final String objectKey, final File temporaryFile) throws IOException {
		try (InputStream existingObjectInputStream = s3Client.getObject(bucketName, objectKey)) {
			if (existingObjectInputStream != null) {
				Files.copy(existingObjectInputStream, temporaryFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
				return;
			}
		} catch (S3Exception e) {
			if (e.statusCode() != 404) {
				throw e;
			}
		}
		Files.deleteIfExists(temporaryFile.toPath());
	}

	private void pushS3StreamIfDue(final String correlationID) {
		final Runnable uploadTask = activeS3UploadTasks.get(correlationID);
		if (uploadTask == null || isOffline) {
			return;
		}
		final long now = System.currentTimeMillis();
		final long lastUpload = activeS3LastUploadMillis.getOrDefault(correlationID, 0L);
		if (now - lastUpload < S3_UPLOAD_INTERVAL_MILLIS) {
			return;
		}
		uploadTask.run();
		activeS3LastUploadMillis.put(correlationID, now);
	}

	private void uploadTemporaryFile(final ResourceManager resourceManager, final File temporaryFile, final boolean deleteAfterUpload) {
		try {
			resourceManager.writeResource("", temporaryFile.toURI().toURL().openStream());
			if (deleteAfterUpload) {
				Files.deleteIfExists(temporaryFile.toPath());
			}
		} catch (IOException e) {
			LOGGER.error("Error occurred while trying to upload the file to S3.", e);
		}
	}

	private void processJMSException(final JMSException e) {
		final Exception linkedException = e.getLinkedException();
		if (linkedException != null && linkedException.getClass().equals(TransportDisposedIOException.class)) {
			LOGGER.info("Transport disposed. Shutting down telemetry consumer.");
			shutdown = true;
			closeOpenStreams();
		} else {
			LOGGER.error("Error occurred while trying to receive the message.", e);
		}
	}

	@PreDestroy
	public final void shutdown() throws JMSException {
		shutdown = true;
		closeOpenStreams();
		consumer.close();
		jmsSession.close();
	}

	private void closeOpenStreams() {
		final Map<String, BufferedWriter> activeWriters;
		synchronized (streamWriters) {
			if (streamWriters.isEmpty()) {
				return;
			}
			activeWriters = new HashMap<>(streamWriters);
			streamWriters.clear();
			activeS3UploadTasks.clear();
			activeS3LastUploadMillis.clear();
		}
		activeWriters.forEach((correlationId, writer) -> {
			try {
				writer.close();
			} catch (IOException e) {
				LOGGER.error("Failed to close stream for correlationID {} during shutdown.", correlationId, e);
			}
		});
	}
}

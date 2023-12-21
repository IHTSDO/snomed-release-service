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
import jakarta.jms.IllegalStateException;
import jakarta.jms.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
@ConditionalOnProperty(name = "srs.build.offlineMode", havingValue = "false")
public class TelemetryProcessor {

	private static final Logger LOGGER = LoggerFactory.getLogger(TelemetryProcessor.class);

	private static final int ONE_SECOND = 1000;

	private static final String TEMP_DIRECTORY_PATH = "/tmp/telemetry-tmp";

	private final Map<String, BufferedWriter> streamWriters;

	private boolean shutdown;
	private final boolean isOffline;

	private final MessageConsumer consumer;

	private final Session jmsSession;

	private final ResourceLoader resourceLoader;

	@Autowired
	public TelemetryProcessor(final Session jmsSession, final ResourceLoader resourceLoader,
			@Value("${srs.build.offlineMode}") final boolean isOffLine) throws JMSException {
		this.streamWriters = new HashMap<>();
		this.jmsSession = jmsSession;
		this.consumer = jmsSession.createConsumer(jmsSession.createQueue(Constants.QUEUE_RELEASE_EVENTS));
		this.resourceLoader = resourceLoader;
		this.isOffline = isOffLine;
		new File(TEMP_DIRECTORY_PATH).mkdirs();
	}

	@PostConstruct
	public void startup() {
		new Thread(this::doStartUp).start();
	}

	private void doStartUp() {
		boolean printedWaiting = false;
		LOGGER.info("Telemetry server starting up.");
		while (!shutdown) {
			printedWaiting = doStartUp(printedWaiting);
		}
	}

	private boolean doStartUp(boolean printedWaiting) {
		try {
			printedWaiting = waitForMessage(printedWaiting);
		} catch (IllegalStateException e) {
			LOGGER.info("Connection closed. Shutting down telemetry consumer.");
			shutdown = true;
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
			return new BufferedWriter(new FileWriter(path));
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

		final File temporaryFile = new File(TEMP_DIRECTORY_PATH + "/" + correlationID);

		return new BufferedWriterTaskOnClose(new FileWriter(temporaryFile), () -> {
			if (!isOffline) {
				try {
					resourceManager.writeResource("", temporaryFile.toURI().toURL().openStream());
					temporaryFile.delete();
				} catch (IOException e) {
					LOGGER.error("Error occurred while trying to upload the file to S3.", e);
				}
			}
		});
	}

	private void processJMSException(final JMSException e) {
		final Exception linkedException = e.getLinkedException();
		if (linkedException != null && linkedException.getClass().equals(TransportDisposedIOException.class)) {
			LOGGER.info("Transport disposed. Shutting down telemetry consumer.");
			shutdown = true;
		} else {
			LOGGER.error("Error occurred while trying to receive the message.", e);
		}
	}

	public final void shutdown() throws JMSException {
		shutdown = true;
		consumer.close();
		jmsSession.close();
	}
}

package org.ihtsdo.buildcloud.telemetry.server;

import org.apache.activemq.transport.TransportDisposedIOException;
import org.ihtsdo.buildcloud.telemetry.core.Constants;
import org.ihtsdo.otf.resourcemanager.ResourceConfiguration;
import org.ihtsdo.otf.resourcemanager.ResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.jms.IllegalStateException;
import javax.jms.*;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Service
public class TelemetryProcessor {

	private static final Logger LOGGER = LoggerFactory.getLogger(TelemetryProcessor.class);

	private static final int ONE_SECOND = 1000;

	private final Map<String, OutputStream> streamWriters;

	private boolean shutdown;

	private final MessageConsumer consumer;

	private final ResourceManager resourceManager;

	private final Session jmsSession;

	@Autowired
	public TelemetryProcessor(final Session jmsSession, final ResourceConfiguration resourceConfiguration, final ResourceLoader resourceLoader) throws JMSException {
		this.streamWriters = new HashMap<>();
		this.resourceManager = new ResourceManager(resourceConfiguration, resourceLoader);
		this.jmsSession = jmsSession;
		this.consumer = jmsSession.createConsumer(jmsSession.createQueue(Constants.QUEUE_RELEASE_EVENTS));
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
		final OutputStream writer = streamWriters.get(correlationID);
		if (writer != null) {
			writeMessageText(message, text, writer);
			// We need output to disk to be up to the minute, so flush each line
			writer.flush();
		} else {
			LOGGER.error("Attempting to write to stream but no open stream for correlationID {}.", correlationID);
		}
	}

	private void writeMessageText(final TextMessage message, final String text, final OutputStream writer) throws IOException, JMSException {
		writer.write(getBytes(text));
		final String exception = message.getStringProperty(Constants.EXCEPTION);
		if (exception != null) {
			writer.write(getBytes(exception));
			writer.write(getBytes(Constants.LINE_BREAK));
		}
	}

	private void finishStream(final String correlationID) throws IOException {
		final OutputStream writer = streamWriters.get(correlationID);
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
			streamWriters.put(correlationID, resourceManager.openWritableResourceStream(streamUri));
		}
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

	private byte[] getBytes(final String content) {
		return content.getBytes(StandardCharsets.UTF_8);
	}

	public final void shutdown() throws JMSException {
		shutdown = true;
		consumer.close();
		jmsSession.close();
	}
}

package org.ihtsdo.buildcloud.telemetry.server;

import org.apache.activemq.transport.TransportDisposedIOException;
import org.ihtsdo.buildcloud.telemetry.core.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.jms.IllegalStateException;
import javax.jms.*;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
public class TelemetryProcessor {

	private static final Logger LOGGER = LoggerFactory.getLogger(TelemetryProcessor.class);
	
	private static final int ONE_SECOND = 1000;

	@Autowired
	private final Session jmsSession;

	private final Map<String, BufferedWriter> streamWriters;
	private Boolean shutdown;
	private final MessageConsumer consumer;
	@Autowired
	private final StreamFactory streamFactory;

	@Autowired
	public TelemetryProcessor(Session jmsSession, final StreamFactory streamFactory) throws JMSException {
		this.shutdown = false;
		this.streamWriters = new HashMap<>();
		this.streamFactory = streamFactory;
		this.jmsSession = jmsSession;
		this.consumer = jmsSession.createConsumer(jmsSession.createQueue(Constants.QUEUE_RELEASE_EVENTS));
	}

	@PostConstruct
	public void startup() {
		Thread messageConsumerThread = new Thread(new Runnable() {
			@Override
			public void run() {
				boolean printedWaiting = false;
				LOGGER.info("Telemetry server starting up.");
				while (!shutdown) {
					try {
						if (!printedWaiting) {
							LOGGER.debug("Waiting for message");
							printedWaiting = true;
						}
						TextMessage message = (TextMessage) consumer.receive(ONE_SECOND);

						if (message != null) {
							printedWaiting = false;
							LOGGER.debug("Got message '{}', correlationID {}", message.getText(), message.getJMSCorrelationID());
							String text = message.getText();
							String correlationID = message.getJMSCorrelationID();
							if (correlationID != null) {
								if (Constants.START_STREAM.equals(text)) {
									// Start new stream
									String streamUri = message.getStringProperty(Constants.STREAM_URI);
									BufferedWriter streamWriter = streamFactory.createStreamWriter(correlationID, streamUri);
									streamWriters.put(correlationID, streamWriter);
								} else if (Constants.FINISH_STREAM.equals(text)) {
									BufferedWriter writer = streamWriters.get(correlationID);
									if (writer != null) {
										writer.close();
										streamWriters.remove(correlationID);
									} else {
										LOGGER.error("Attempting to close stream but no open stream for correlationID {}", correlationID);
									}
								} else {
									BufferedWriter writer = streamWriters.get(correlationID);
									if (writer != null) {
										writer.write(text);
										String exception = message.getStringProperty(Constants.EXCEPTION);
										if (exception != null) {
											writer.write(exception);
											writer.write(Constants.LINE_BREAK);
										}
										// We need output to disk to be up to the minute, so flush each line
										writer.flush();
									} else {
										LOGGER.error("Attempting to write to stream but no open stream for correlationID {}", correlationID);
									}
								}
							}
						}
					} catch (IllegalStateException e) {
						LOGGER.info("Connection closed. Shutting down telemetry consumer.");
						shutdown = true;
					} catch (JMSException e) {
						Exception linkedException = e.getLinkedException();
						if (linkedException != null && linkedException.getClass().equals(TransportDisposedIOException.class)) {
							LOGGER.info("Transport disposed. Shutting down telemetry consumer.");
							shutdown = true;
						} else {
							LOGGER.error("JMSException", e);
						}
					} catch (IOException e) {
						LOGGER.error("Problem with output writer.", e);
					}
				}
			}
		});
		messageConsumerThread.start();
	}

	public void shutdown() throws InterruptedException, JMSException {
		this.shutdown = true;
		consumer.close();
		this.jmsSession.close();
	}
}

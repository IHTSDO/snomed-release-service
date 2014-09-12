package org.ihtsdo.telemetry.server;

import org.apache.activemq.transport.TransportDisposedIOException;
import org.ihtsdo.telemetry.core.Constants;
import org.ihtsdo.telemetry.core.JmsFactory;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import javax.jms.TextMessage;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class TelemetryProcessor {

	private final Map<String, BufferedWriter> streamWriters;
	private final Session session;

	public TelemetryProcessor() throws JMSException {
		streamWriters = new HashMap<>();
		session = new JmsFactory().createSession();

		final MessageConsumer consumer = session.createConsumer(session.createQueue(Constants.QUEUE_RELEASE_EVENTS));

		Thread messageConsumerThread = new Thread(new Runnable() {
			@Override
			public void run() {
				boolean connectionOpen = true;
				while (connectionOpen) {
					try {
						System.out.println("Waiting for message");
						TextMessage message = (TextMessage) consumer.receive();

						System.out.println("Got message " + message);
						if (message != null) {
							String text = message.getText();
							String correlationID = message.getJMSCorrelationID();
							if (Constants.START_STREAM.equals(text)) {
								// Start new stream
								String streamUri = message.getStringProperty(Constants.STREAM_URI);
								BufferedWriter streamWriter = new StreamFactory().createStreamWriter(streamUri);
								streamWriters.put(correlationID, streamWriter);
							} else if (Constants.FINISH_STREAM.equals(text)) {
								BufferedWriter writer = streamWriters.get(correlationID);
								if (writer != null) {
									writer.close();
									streamWriters.remove(correlationID);
								} else {
									// TODO: log error
								}
							} else {
								BufferedWriter writer = streamWriters.get(correlationID);
								if (writer != null) {
									writer.write(text);
									writer.newLine();
								} else {
									// TODO: log error
								}
							}
						}
					} catch (JMSException e) {
						// TODO: log error
						Exception linkedException = e.getLinkedException();
						if (linkedException != null && linkedException.getClass().equals(TransportDisposedIOException.class)) {
							connectionOpen = false;
						}
						e.printStackTrace();
					} catch (IOException e) {
						// TODO: log error
						e.printStackTrace();
					}
				}
			}
		});
		messageConsumerThread.start();
	}



}

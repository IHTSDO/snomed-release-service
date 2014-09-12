package org.ihtsdo.telemetry;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.log4j.MDC;
import org.apache.log4j.WriterAppender;
import org.apache.log4j.spi.LoggingEvent;

import javax.jms.*;
import java.util.Hashtable;

public class TelemetryEventAppender extends WriterAppender {

	public static final String SYS_PROP_BROKER_URL = "org.ihtsdo.telemetry.brokerurl";
	public static final String QUEUE_RELEASE_EVENTS = "release.events";
	private MessageProducer producer;
	private Session session;

	private MessageProducer createProducer() {
		try {
			String brokerUrl = System.getProperty(SYS_PROP_BROKER_URL, "tcp://localhost:61616");
			ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(brokerUrl);
			Connection connection = connectionFactory.createConnection();
			session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
			Queue queue = session.createQueue(QUEUE_RELEASE_EVENTS);
			return session.createProducer(queue);
		} catch (JMSException e) {
			throw new RuntimeException("Failed to create TelemetryEventAppender.", e);
		}
	}

	@Override
	public void append(LoggingEvent event) {
		if (producer == null) {
			producer = createProducer();
		}
		try {
			TextMessage message = createMessage(event);
			System.out.println("Sending message" + message.toString());
			producer.send(message);
		} catch (JMSException e) {
			throw new RuntimeException("Failed to create event message.", e);
		}
	}

	@Override
	public synchronized void close() {
		try {
			session.close();
		} catch (JMSException e) {
			throw new RuntimeException("Failed to close session of TelemetryEventAppender.", e);
		}
	}

	private TextMessage createMessage(LoggingEvent event) throws JMSException {
		TextMessage message = session.createTextMessage();
		message.setText(event.getRenderedMessage());
		message.setLongProperty("timeStamp", event.getTimeStamp());
		message.setStringProperty("level", event.getLevel().toString());
		Hashtable<String, Object> context = MDC.getContext();
		for (String key : context.keySet()) {
			message.setStringProperty("context." + key, MDC.get(key).toString());
		}
		return message;
	}

}

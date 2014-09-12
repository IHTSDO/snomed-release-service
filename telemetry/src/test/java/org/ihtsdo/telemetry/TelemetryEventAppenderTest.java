package org.ihtsdo.telemetry;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.*;
import java.util.ArrayList;
import java.util.List;

public class TelemetryEventAppenderTest {

	private Connection connection;
	private MessageConsumer consumer;

	@Before
	public void setup() throws JMSException {
		ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory("vm://localhost?broker.persistent=false");
		connection = connectionFactory.createConnection();
		Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
		consumer = session.createConsumer(session.createQueue(TelemetryEventAppender.QUEUE_RELEASE_EVENTS));
		connection.start();

		// Set system property to override log4j appender default broker url
		System.setProperty(TelemetryEventAppender.SYS_PROP_BROKER_URL, "vm://localhost?create=false");
	}

	@Test
	public void testLogInfoEvent() throws JMSException, InterruptedException {
		// Creaate logger
		Logger logger = LoggerFactory.getLogger(getClass());

		// Listen for messages
		final List<Message> messages = new ArrayList<>();
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Message receive = consumer.receive();
					if (receive != null) {
						messages.add(receive);
					}
				} catch (JMSException e) {
					e.printStackTrace();
				}
			}
		}).start();

		// Log an event
		logger.info("test event");
		// Wait for jms message to come through
		Thread.sleep(1000l);

		// Assert message received
		Assert.assertEquals(1, messages.size());
		System.out.println(messages.get(0).getClass());
		TextMessage actual = (TextMessage) messages.get(0);
		Assert.assertEquals("test event", actual.getText());
		Assert.assertEquals("INFO", actual.getStringProperty("level"));
	}

	@After
	public void after() throws JMSException {
		connection.close();
	}

}

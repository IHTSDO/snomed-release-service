package org.ihtsdo.buildcloud;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.ihtsdo.buildcloud.telemetry.core.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.jms.*;
import java.util.ArrayList;
import java.util.List;

public class TestBroker {

	private final Connection connection;
	private final List<Message> messages;
	private final Session session;
	private final Logger logger = LoggerFactory.getLogger(getClass());

	public TestBroker(String brokerUrl) throws JMSException {
		ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(brokerUrl);
		connection = connectionFactory.createConnection();
		session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
		connection.start();

		messages = new ArrayList<>();

		// Set the test broker url as the default for other components within this JVM.
		System.setProperty(Constants.SYS_PROP_BROKER_URL, brokerUrl);
	}


	public void consumeMessages() throws JMSException {
		Queue queue = session.createQueue(Constants.QUEUE_RELEASE_EVENTS);
		final MessageConsumer consumer = session.createConsumer(queue);
		new Thread(() -> {
            try {
                Message receive = consumer.receive();
                if (receive != null) {
                    messages.add(receive);
                }
            } catch (JMSException e) {
                logger.error("JMSException", e);
            }
        }).start();
	}

	public void close() throws JMSException {
		connection.close();
	}

	public List<Message> getMessages() {
		return messages;
	}

	public Session getSession() {
		return session;
	}
}

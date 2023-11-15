package org.ihtsdo.buildcloud.telemetry.core;

import org.apache.activemq.ActiveMQConnectionFactory;

import jakarta.jms.Connection;
import jakarta.jms.JMSException;
import jakarta.jms.Session;

public class JmsFactory {

	public Session createSession() throws JMSException {
		String brokerUrl = System.getProperty(Constants.SYS_PROP_BROKER_URL, "tcp://localhost:61616");
		ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(brokerUrl);
		Connection connection = connectionFactory.createConnection();
		Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
		connection.start();
		return session;
	}
}

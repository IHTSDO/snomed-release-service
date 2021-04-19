package org.ihtsdo.telemetry.core;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Session;

@Service
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

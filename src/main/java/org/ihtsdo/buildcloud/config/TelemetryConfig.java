package org.ihtsdo.buildcloud.config;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;
import org.ihtsdo.buildcloud.telemetry.core.Constants;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Session;

@Configuration
@ConditionalOnProperty(name = "srs.build.offlineMode", havingValue = "false")
public class TelemetryConfig {

	@Bean
	public BrokerService broker() throws Exception {
		BrokerService broker = new BrokerService();
		broker.addConnector("tcp://localhost:61616");
		broker.setBrokerName("TelemetryJMSBroker");
		broker.setSystemExitOnShutdown(true);
		broker.setUseShutdownHook(false);
		broker.start();
		return broker;
	}

	@Bean
	@DependsOn("broker")
	public Session jmsSession() throws JMSException {
		String brokerUrl = System.getProperty(Constants.SYS_PROP_BROKER_URL, "tcp://localhost:61616");
		ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(brokerUrl);
		Connection connection = connectionFactory.createConnection();
		Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
		connection.start();
		return session;
	}

}

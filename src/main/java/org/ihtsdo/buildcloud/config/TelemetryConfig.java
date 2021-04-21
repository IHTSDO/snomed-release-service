package org.ihtsdo.buildcloud.config;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.transfer.TransferManager;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;
import org.ihtsdo.buildcloud.telemetry.core.Constants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Session;

@Configuration
public class TelemetryConfig {

	@Bean
	public TransferManager transferManager(@Value("${aws.key}") final String accessKey,
			@Value("${aws.privateKey}") final String secretKey) {
		return new TransferManager(new BasicAWSCredentials(accessKey, secretKey));
	}

	@Bean
	public BrokerService broker() throws Exception {
		BrokerService broker = new BrokerService();
		broker.addConnector("tcp://localhost:61616");
		broker.setBrokerName("TelemetryJMSBroker");
		broker.setSystemExitOnShutdown(true);
		broker.setUseShutdownHook(false);
		if (!broker.isStarted()) {
			broker.start();
		}
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

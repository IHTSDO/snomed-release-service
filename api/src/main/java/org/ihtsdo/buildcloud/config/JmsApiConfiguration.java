package org.ihtsdo.buildcloud.config;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.ihtsdo.buildcloud.JMSBrokerManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.SimpleJmsListenerContainerFactory;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.core.JmsTemplate;

import javax.jms.ConnectionFactory;

@Configuration
@EnableJms
@ConfigurationProperties
public class JmsApiConfiguration {

	@Bean
	public SimpleJmsListenerContainerFactory jmsListenerContainerFactory(@Value("${orchestration.jms.url}") final String brokerUrl,
			@Value("${orchestration.jms.username}") final String username, @Value("${orchestration.jms.password}") final String password) {
		final SimpleJmsListenerContainerFactory simpleJmsListenerContainerFactory =
				new SimpleJmsListenerContainerFactory();
		simpleJmsListenerContainerFactory.setConnectionFactory(new ActiveMQConnectionFactory(username, password, brokerUrl));
		return simpleJmsListenerContainerFactory;
	}

	@Bean
	public ActiveMQConnectionFactory jmsConnectionFactory(@Value("${orchestration.jms.url}") final String brokerUrl,
			@Value("${orchestration.jms.username}") final String username, @Value("${orchestration.jms.password}") final String password) {
		return new ActiveMQConnectionFactory(username, password, brokerUrl);
	}

	@Bean
	public ConnectionFactory connectionFactory(@Value("${orchestration.jms.url}") final String brokerUrl,
			@Value("${orchestration.jms.username}") final String username, @Value("${orchestration.jms.password}") final String password) {
		return new CachingConnectionFactory(jmsConnectionFactory(brokerUrl, username, password));
	}

	@Bean
	public JmsTemplate jmsTemplate(@Autowired ConnectionFactory connectionFactory) {
		return new JmsTemplate(connectionFactory);
	}

	@Bean
	public JMSBrokerManager jmsBrokerManager() throws Exception {
		return new JMSBrokerManager();
	}
}

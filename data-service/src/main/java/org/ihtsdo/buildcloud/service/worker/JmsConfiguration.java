package org.ihtsdo.buildcloud.service.worker;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQQueue;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.ihtsdo.otf.jms.MessagingHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.config.SimpleJmsListenerContainerFactory;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessageType;

import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Queue;

@Configuration
@ConfigurationProperties
public class JmsConfiguration {

	@Bean
	public Queue srsQueue(@Value("${srs.jms.job.queue}") final String queue) {
		return new ActiveMQQueue(queue);
	}

	@Bean
	public Queue buildStatusQueue(final String queue) {
		return new ActiveMQQueue(queue);
	}

	@Bean
	public ActiveMQTextMessage buildStatusTextMessage(@Value("${build.status.jms.job.queue}") final String queue) throws JMSException {
		final ActiveMQTextMessage activeMQTextMessage = new ActiveMQTextMessage();
		activeMQTextMessage.setJMSReplyTo(buildStatusQueue(queue));
		return activeMQTextMessage;
	}

	@Bean
	public MessageConverter jacksonJmsMessageConverter() {
		MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
		converter.setTargetType(MessageType.TEXT);
		converter.setTypeIdPropertyName("_type");
		return converter;
	}

	@Bean
	public MessagingHelper messagingHelper() {
		return new MessagingHelper();
	}

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
}

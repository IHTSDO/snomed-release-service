package org.ihtsdo.buildcloud.service.worker;

import org.apache.activemq.command.ActiveMQQueue;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.ihtsdo.otf.jms.MessagingHelper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessageType;

import javax.jms.JMSException;
import javax.jms.Queue;

@Configuration
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
}

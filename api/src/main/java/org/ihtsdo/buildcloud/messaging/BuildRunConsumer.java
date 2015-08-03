package org.ihtsdo.buildcloud.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.service.BuildService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.TextMessage;

@SuppressWarnings("unused")
@Component
public class BuildRunConsumer {

	@Autowired
	private ConnectionFactory connectionFactory;

	@Autowired
	private BuildService buildService;

	@Autowired
	private ObjectMapper objectMapper;

	private Logger logger = LoggerFactory.getLogger(getClass());

	@JmsListener(destination = "release/build")
	public void triggerBuild(TextMessage message) {
		logger.info("Received message {}", message);
		Assert.notNull(connectionFactory);
		final JmsTemplate jmsTemplate = new JmsTemplate(connectionFactory);
		Destination replyToDestination = MessagingHelper.getReplyToDestination(message);
		try {
			final String releaseCenterKey = message.getStringProperty("releaseCenterKey");
			Assert.notNull(releaseCenterKey, "releaseCenterKey property is required");

			final String productKey = message.getStringProperty("productKey");
			final String buildId = message.getStringProperty("buildId");

			final Build build = buildService.triggerBuild(releaseCenterKey, productKey, buildId);

			logger.info("Sending response {}", build);
			jmsTemplate.convertAndSend(replyToDestination, objectMapper.writeValueAsString(build));
		} catch (Exception e) {
			logger.info("Sending error response {}", e);
			jmsTemplate.convertAndSend(replyToDestination, e);
		}
	}

	public void setConnectionFactory(ConnectionFactory connectionFactory) {
		this.connectionFactory = connectionFactory;
	}
}

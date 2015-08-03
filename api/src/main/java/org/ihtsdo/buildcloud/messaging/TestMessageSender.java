package org.ihtsdo.buildcloud.messaging;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.jms.core.MessagePostProcessor;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.Message;

@Component
public class TestMessageSender implements ApplicationListener {

	@Autowired
	private MessagingHelper messagingHelper;

	@Override
	public void onApplicationEvent(ApplicationEvent applicationEvent) {
		if (applicationEvent instanceof ContextRefreshedEvent) {
			final String destinationName = "release/build";
			messagingHelper.getJmsTemplate().convertAndSend(destinationName, "", new MessagePostProcessor() {
				@Override
				public Message postProcessMessage(Message message) throws JMSException {
					message.setStringProperty(MessagingHelper.AUTHENTICATION_TOKEN, "");
					message.setStringProperty(BuildRunConsumer.RELEASE_CENTER_KEY, "rc");
					message.setStringProperty(BuildRunConsumer.PRODUCT_KEY, "prod");
					message.setStringProperty(BuildRunConsumer.BUILD_ID, "build");
					return message;
				}
			});
		}
	}
}

package org.ihtsdo.buildcloud.builder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

public class BuildMessageListener implements MessageListener {

	@Autowired
	private BuilderService builderService;

	private static final Logger LOGGER = LoggerFactory.getLogger(BuildMessageListener.class);

	@Override
	public void onMessage(Message message) {
		try {
			if (message instanceof TextMessage) {
				TextMessage textMessage = (TextMessage) message;
				String executionUrl = textMessage.getText();
				LOGGER.info("Consuming executionUrl:{}", executionUrl);
				builderService.buildExecution(executionUrl);
			} else {
				throw new IllegalArgumentException("Message must be of type TextMessage");
			}
		} catch (Exception e) {
			LOGGER.error(e.getLocalizedMessage(), e);
		}
	}

}

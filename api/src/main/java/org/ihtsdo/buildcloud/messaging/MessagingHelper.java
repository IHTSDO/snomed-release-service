package org.ihtsdo.buildcloud.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.activemq.command.ActiveMQQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessagePostProcessor;

import javax.jms.*;

public class MessagingHelper {

	public static final String RESPONSE_POSTFIX = ".response";
	public static final String DEAD_LETTER_QUEUE = "dead-letter-queue";
	public static final String AUTHENTICATION_TOKEN = "authenticationToken";

	private Logger logger = LoggerFactory.getLogger(getClass());

	public static final MessagePostProcessor postProcessorSetErrorFlag = new MessagePostProcessor() {
		@Override
		public Message postProcessMessage(Message message) throws JMSException {
			message.setBooleanProperty("error", true);
			return message;
		}
	};

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private ConnectionFactory connectionFactory;

	public void sendResponse(TextMessage incomingMessage, Object responseObject) {
		logger.info("Sending response {}", responseObject);
		try {
			final String message = objectMapper.writeValueAsString(responseObject);
			getJmsTemplate().convertAndSend(getReplyToDestination(incomingMessage), message);
		} catch (JsonProcessingException e) {
			sendErrorResponse(incomingMessage, e);
		}
	}

	public void sendErrorResponse(TextMessage incomingMessage, Exception e) {
		logger.info("Sending error response {}", e);
		String errorMessageString = null;
		try {
			errorMessageString = objectMapper.writeValueAsString(e);
		} catch (JsonProcessingException e1) {
			logger.error("Failed to serialize error {}", e, e1);
		}
		getJmsTemplate().convertAndSend(getReplyToDestination(incomingMessage), errorMessageString, postProcessorSetErrorFlag);
	}

	public JmsTemplate getJmsTemplate() {
		return new JmsTemplate(connectionFactory);
	}

	/**
	 * Uses message properties to retrieve the reply-to address or builds one based on the name of the original destination.
	 * @param message
	 * @return
	 */
	public static Destination getReplyToDestination(Message message) {
		try {
			Destination replyTo = message.getJMSReplyTo();
			if (replyTo == null) {
				final Destination jmsDestination = message.getJMSDestination();
				if (jmsDestination instanceof Queue) {
					Queue q = (Queue) jmsDestination;
					final String queueName = q.getQueueName();
					replyTo = new ActiveMQQueue(queueName + RESPONSE_POSTFIX);
				} else {
					throw new UnsupportedOperationException("Support of this destination type has not been implemented.");
				}
			}
			return replyTo;
		} catch(JMSException e) {
			return getDeadLetterQueue();
		}
	}

	public static Destination getDeadLetterQueue() {
		return new ActiveMQQueue(DEAD_LETTER_QUEUE);
	}
}

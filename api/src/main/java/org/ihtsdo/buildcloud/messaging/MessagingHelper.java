package org.ihtsdo.buildcloud.messaging;

import org.apache.activemq.command.ActiveMQQueue;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Queue;

public class MessagingHelper {

	public static final String RESPONSE_POSTFIX = ".response";
	public static final String DEAD_LETTER_QUEUE = "dead-letter-queue";

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

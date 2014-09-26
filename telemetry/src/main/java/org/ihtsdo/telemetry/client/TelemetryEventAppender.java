package org.ihtsdo.telemetry.client;

import org.apache.log4j.Layout;
import org.apache.log4j.MDC;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.WriterAppender;
import org.apache.log4j.spi.LoggingEvent;
import org.ihtsdo.telemetry.core.Constants;
import org.ihtsdo.telemetry.core.JmsFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.*;
import java.util.Hashtable;
import java.util.UUID;

public class TelemetryEventAppender extends WriterAppender {

	private static final String PATTERN = "%d{yyyyMMdd_HH:mm:ss} %-5p %C.%M - %m%n";
	private MessageProducer producer;
	private Session session;
	private Logger logger = LoggerFactory.getLogger(TelemetryEventAppender.class);

	public TelemetryEventAppender() {
		// Hardcoding pattern because should not be under host application control
		super.setLayout(new PatternLayout(PATTERN));
	}

	@Override
	public void append(LoggingEvent event) {
		if (producer == null) {
			producer = createProducer();
		}
		if (producer != null) {
			try {
				TextMessage message;
				if (MDC.get(Constants.START_STREAM) != null) {
					message = createStartStreamMessage();
				} else if (MDC.get(Constants.FINISH_STREAM) != null) {
					message = createFinishStreamMessage();
				} else {
					message = createEventMessage(event);
				}
				logger.debug("Sending message '{}'", message.getText());
				producer.send(message);
			} catch (JMSException e) {
				throw new RuntimeException("Failed to create event message.", e);
			}
		}
	}

	@Override
	public void setLayout(Layout layout) {
		logger.warn("Ignoring layout from log4j config.");
	}

	private TextMessage createStartStreamMessage() throws JMSException {
		String streamId = UUID.randomUUID().toString();
		MDC.put(Constants.STREAM_ID, streamId);
		TextMessage message = createMessage(Constants.START_STREAM);
		message.setStringProperty(Constants.STREAM_URI, (String) MDC.get(Constants.START_STREAM));
		MDC.remove(Constants.START_STREAM);
		return message;
	}

	private TextMessage createFinishStreamMessage() throws JMSException {
		TextMessage message;
		message = createMessage(Constants.FINISH_STREAM);
		MDC.remove(Constants.FINISH_STREAM);
		MDC.remove(Constants.STREAM_ID);
		return message;
	}

	private TextMessage createEventMessage(LoggingEvent event) throws JMSException {
		TextMessage message = createMessage(this.layout.format(event));
		message.setStringProperty(Constants.LEVEL, event.getLevel().toString());
		message.setLongProperty(Constants.TIME_STAMP, event.getTimeStamp());
		Hashtable<String, Object> context = MDC.getContext();
		if (context != null) {
			for (String key : context.keySet()) {
				message.setStringProperty("context." + key, MDC.get(key).toString());
			}
		}
		return message;
	}

	private TextMessage createMessage(String text) throws JMSException {
		TextMessage message = session.createTextMessage();
		String streamId = (String) MDC.get(Constants.STREAM_ID);
		if (streamId != null) {
			message.setJMSCorrelationID(streamId);
		}
		message.setText(text);
		return message;
	}

	private MessageProducer createProducer() {
		try {
			session = new JmsFactory().createSession();
			return session.createProducer(session.createQueue(Constants.QUEUE_RELEASE_EVENTS));
		} catch (JMSException e) {
			logger.warn("Can't connect to message broker.");
			return null;
		}
	}

}

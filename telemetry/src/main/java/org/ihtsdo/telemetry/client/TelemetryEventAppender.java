package org.ihtsdo.telemetry.client;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.MDC;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.ihtsdo.telemetry.core.Constants;
import org.ihtsdo.telemetry.core.JmsFactory;
import org.ihtsdo.telemetry.core.TelemetryRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import java.util.Map;
import java.util.UUID;

@Plugin(name = "TelemetryEventAppender", category = Core.CATEGORY_NAME, elementType = Appender.ELEMENT_TYPE)
@Service
public class TelemetryEventAppender extends AbstractAppender {

	private static final Logger LOGGER = LoggerFactory.getLogger(TelemetryEventAppender.class);

	private static final String PATTERN = "%d{yyyyMMdd_HH:mm:ss} %-5p %C.%M - %m%n";

	private MessageProducer producer;

	private Session session;

	private String service;

	private String environment;

	public TelemetryEventAppender(final String name, final Filter filter, final String service, final String environment) {
		super(name, filter, PatternLayout.newBuilder().withPattern(PATTERN).build());
		this.service = service == null ? "SRS" : service;
		this.environment = environment == null ? "LocalHost" : environment;
		LOGGER.info("Telemetry Service: {}", service);
		LOGGER.info("Telemetry Environment: {}", environment);
	}

	@PluginFactory
	public static TelemetryEventAppender createAppender(@PluginAttribute("name") final String name,
			@PluginElement("Filter") final Filter filter, @PluginAttribute("env") final String environment) {
		return new TelemetryEventAppender(name, filter, "SRS", environment);
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

	private TextMessage createEventMessage(LogEvent event, String service, String environment) throws JMSException {
		TextMessage message = createMessage(event.getMessage().getFormattedMessage());
		message.setStringProperty(Constants.LEVEL, event.getLevel().toString());
		message.setLongProperty(Constants.TIME_STAMP, event.getNanoTime());
		message.setStringProperty(Constants.EXCEPTION, StringUtils.join(event.getThrown().getMessage(), "\n"));
		message.setStringProperty(Constants.SERVICE, service);
		message.setStringProperty(Constants.ENVIRONMENT, environment);
		@SuppressWarnings("unchecked")
		Map<String, Object> context = MDC.getContext();
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
			LOGGER.warn("Can't connect to message broker.");
			return null;
		}
	}

	public String getService() {
		return service;
	}

	public String getEnvironment() {
		return environment;
	}

	public void setService(String service) {
		this.service = service;
	}

	public void setEnvironment(String environment) {
		if (environment != null && !environment.isEmpty()) {
			this.environment = environment;
		}
	}

	@Override
	public void append(LogEvent logEvent) {
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
					message = createEventMessage(logEvent, this.service, this.environment);
				}
				LOGGER.debug("Sending message '{}'", message.getText());
				producer.send(message);
			} catch (JMSException e) {
				throw new TelemetryRuntimeException("Failed to create event message.", e);
			}
		}
	}
}

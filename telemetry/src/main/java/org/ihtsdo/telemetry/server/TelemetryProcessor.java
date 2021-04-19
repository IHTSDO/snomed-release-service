package org.ihtsdo.telemetry.server;

import org.apache.activemq.transport.TransportDisposedIOException;
import org.apache.commons.mail.EmailException;
import org.ihtsdo.commons.email.EmailRequest;
import org.ihtsdo.commons.email.EmailSender;
import org.ihtsdo.telemetry.core.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.jms.IllegalStateException;
import javax.jms.*;

@Service
public class TelemetryProcessor {

	private static final Logger LOGGER = LoggerFactory.getLogger(TelemetryProcessor.class);
	
	private static final int ONE_SECOND = 1000;

	private final Session jmsSession;

	private final Map<String, BufferedWriter> streamWriters;
	private Boolean shutdown;
	private final MessageConsumer consumer;
	private final StreamFactory streamFactory;
	private static String defaultEmailToAddr;
	private static String emailFromAddr;
	private static EmailSender emailSender;

	@Autowired
	public TelemetryProcessor(@Autowired Session jmsSession,
			@Autowired final StreamFactory streamFactory,
			@Value("${telemetry.email.address.to.default}") final String defaultEmailToAddr,
			@Value("${telemetry.email.address.from}") final String emailFromAddr,
			@Value("${telemetry.smtp.username}") final String smtpUsername,
			@Value("${telemetry.smtp.password}") final String smtpPassword,
			@Value("${telemetry.smtp.host}") final String smtpHost,
			@Value("${telemetry.smtp.port}") final Integer smtpPort,
			@Value("${telemetry.smtp.ssl}") final Boolean smtpSsl) throws JMSException {
		this(jmsSession, streamFactory, defaultEmailToAddr, emailFromAddr);
		if (smtpHost != null && smtpUsername != null && defaultEmailToAddr != null) {
			TelemetryProcessor.emailSender = new EmailSender(smtpHost, smtpPort, smtpUsername, smtpPassword, smtpSsl);
			LOGGER.info("Telemetry server configured to use SMTP " + TelemetryProcessor.emailSender.toString());
		} else {
			LOGGER.info("Telemetry server has not been given SMTP connection details.  Email connectivity disabled.");
			TelemetryProcessor.emailSender = null;
		}
	}

	public TelemetryProcessor(final Session jmsSession, final StreamFactory streamFactory, final String defaultEmailToAddr, final String emailFromAddr,
			EmailSender emailSender) throws JMSException {
		this(jmsSession, streamFactory, defaultEmailToAddr, emailFromAddr);
		LOGGER.info("Telemetry server using pre-configured " + emailSender.toString());

		TelemetryProcessor.emailSender = emailSender;
	}

	// Common constructor
	private TelemetryProcessor(final Session jmsSession, final StreamFactory streamFactory, final String defaultEmailToAddr, final String emailFromAddr)
			throws JMSException {
		this.shutdown = false;
		this.streamWriters = new HashMap<>();
		this.streamFactory = streamFactory;
		this.jmsSession = jmsSession;
		this.consumer = jmsSession.createConsumer(jmsSession.createQueue(Constants.QUEUE_RELEASE_EVENTS));

		TelemetryProcessor.defaultEmailToAddr = defaultEmailToAddr;
		TelemetryProcessor.emailFromAddr = emailFromAddr;
	}

	@PostConstruct
	public void startup() {
		Thread messageConsumerThread = new Thread(new Runnable() {
			@Override
			public void run() {
				boolean printedWaiting = false;
				LOGGER.info("Telemetry server starting up.");
				while (!shutdown) {
					try {
						if (!printedWaiting) {
							LOGGER.debug("Waiting for message");
							printedWaiting = true;
						}
						TextMessage message = (TextMessage) consumer.receive(ONE_SECOND);

						if (message != null) {
							printedWaiting = false;
							LOGGER.debug("Got message '{}', correlationID {}", message.getText(), message.getJMSCorrelationID());
							String text = message.getText();
							String correlationID = message.getJMSCorrelationID();
							if (correlationID != null) {
								if (Constants.START_STREAM.equals(text)) {
									// Start new stream
									String streamUri = message.getStringProperty(Constants.STREAM_URI);
									BufferedWriter streamWriter = streamFactory.createStreamWriter(correlationID, streamUri);
									streamWriters.put(correlationID, streamWriter);
								} else if (Constants.FINISH_STREAM.equals(text)) {
									BufferedWriter writer = streamWriters.get(correlationID);
									if (writer != null) {
										writer.close();
										streamWriters.remove(correlationID);
									} else {
										LOGGER.error("Attempting to close stream but no open stream for correlationID {}", correlationID);
									}
								} else {
									BufferedWriter writer = streamWriters.get(correlationID);
									if (writer != null) {
										writer.write(text);
										String exception = message.getStringProperty(Constants.EXCEPTION);
										if (exception != null) {
											writer.write(exception);
											writer.write(Constants.LINE_BREAK);
										}
										// We need output to disk to be up to the minute, so flush each line
										writer.flush();
									} else {
										LOGGER.error("Attempting to write to stream but no open stream for correlationID {}", correlationID);
									}
								}
							}

							// As well as logging the message and even if we're outside of an event stream, if an exception is detected then
							// route it to email
							if (message.getStringProperty("level") != null && message.getStringProperty("level").equals("ERROR")) {
								try {
									sendEmailAlert(message);
								} catch (Exception e) {
									String msg = message.getText();
									if (message.propertyExists(Constants.EXCEPTION)) {
										msg += "\n" + message.getStringProperty(Constants.EXCEPTION);
									}
									LOGGER.error("Unable to send email alert to report: " + msg, e);
								}
							}
						}
					} catch (IllegalStateException e) {
						LOGGER.info("Connection closed. Shutting down telemetry consumer.");
						shutdown = true;
					} catch (JMSException e) {
						Exception linkedException = e.getLinkedException();
						if (linkedException != null && linkedException.getClass().equals(TransportDisposedIOException.class)) {
							LOGGER.info("Transport disposed. Shutting down telemetry consumer.");
							shutdown = true;
						} else {
							LOGGER.error("JMSException", e);
						}
					} catch (IOException e) {
						LOGGER.error("Problem with output writer.", e);
					}
				}
			}

			private void sendEmailAlert(TextMessage message) throws MalformedURLException, EmailException, JMSException {
				// Do we have an EmailSender configured?
				if (TelemetryProcessor.emailSender == null || TelemetryProcessor.defaultEmailToAddr == null
						|| TelemetryProcessor.defaultEmailToAddr.isEmpty()) {
					LOGGER.info("EmailSender not configured.  Unable to report error message: " + message.getText());
					return;
				}
				EmailRequest emailRequest = new EmailRequest();
				emailRequest.setToEmail(TelemetryProcessor.defaultEmailToAddr);
				emailRequest.setFromEmail(TelemetryProcessor.emailFromAddr);
				// TODO Add this string via config.
				String subject = String.format("IHTSDO Telemetry - %s service error detected in %s.",
						message.getStringProperty(Constants.SERVICE), message.getStringProperty(Constants.ENVIRONMENT));
				emailRequest.setSubject(subject);
				String msg = message.getText();
				if (message.propertyExists(Constants.EXCEPTION)) {
					msg += "\n" + message.getStringProperty(Constants.EXCEPTION);
				}
				emailRequest.setTextBody("IHTSO Telemetry Server has received the following error message: " + msg);
				// TODO Check this for thread safety since we're using a class variable here.
				TelemetryProcessor.emailSender.send(emailRequest);
			}
		});
		messageConsumerThread.start();
	}

	public void shutdown() throws InterruptedException, JMSException {
		this.shutdown = true;
		consumer.close();
		this.jmsSession.close();
	}
}

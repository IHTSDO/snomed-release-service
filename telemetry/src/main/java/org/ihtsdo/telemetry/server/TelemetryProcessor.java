package org.ihtsdo.telemetry.server;

import org.apache.activemq.transport.TransportDisposedIOException;
import org.apache.commons.mail.EmailException;
import org.ihtsdo.commons.email.EmailRequest;
import org.ihtsdo.commons.email.EmailSender;
import org.ihtsdo.telemetry.core.Constants;
import org.ihtsdo.telemetry.core.JmsFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;
import javax.jms.IllegalStateException;
import javax.jms.*;

public class TelemetryProcessor {

	private static final int ONE_SECOND = 1000;

	private final Session jmsSession;
	private final Map<String, BufferedWriter> streamWriters;
	private Boolean shutdown;
	private final MessageConsumer consumer;
	private Logger logger = LoggerFactory.getLogger(TelemetryProcessor.class);
	private final StreamFactory streamFactory;
	static private String defaultEmailToAddr;
	static private String emailFromAddr;
	static private EmailSender emailSender;


	@Autowired
	public TelemetryProcessor(final StreamFactory streamFactory, final String defaultEmailToAddr, final String emailFromAddr,
			final String smtpUsername,
			final String smtpPassword, final String smtpHost, final Integer smtpPort, final Boolean smtpSsl) throws JMSException {

		this(streamFactory, defaultEmailToAddr, emailFromAddr);
		if (smtpHost != null && smtpUsername != null && defaultEmailToAddr != null) {
			TelemetryProcessor.emailSender = new EmailSender(smtpHost, smtpPort.intValue(), smtpUsername, smtpPassword,
					smtpSsl.booleanValue());
			logger.info("Telemetry server configured to use SMTP " + TelemetryProcessor.emailSender.toString());

		} else {
			logger.info("Telemetry server has not been given SMTP connection details.  Email connectivity disabled.");
			TelemetryProcessor.emailSender = null;
		}

	}

	public TelemetryProcessor(final StreamFactory streamFactory, final String defaultEmailToAddr, final String emailFromAddr,
			EmailSender emailSender) throws JMSException {
		this(streamFactory, defaultEmailToAddr, emailFromAddr);
		logger.info("Telemetry server using pre-configured " + emailSender.toString());

		TelemetryProcessor.emailSender = emailSender;
	}

	// Common constructor
	private TelemetryProcessor(final StreamFactory streamFactory, final String defaultEmailToAddr, final String emailFromAddr)
			throws JMSException {
		this.shutdown = false;
		this.streamWriters = new HashMap<>();
		this.streamFactory = streamFactory;
		this.jmsSession = new JmsFactory().createSession();
		this.consumer = jmsSession.createConsumer(jmsSession.createQueue(Constants.QUEUE_RELEASE_EVENTS));

		TelemetryProcessor.defaultEmailToAddr = defaultEmailToAddr;
		TelemetryProcessor.emailFromAddr = emailFromAddr;
	}

	public void startup() {

		Thread messageConsumerThread = new Thread(new Runnable() {
			@Override
			public void run() {
				boolean printedWaiting = false;
				logger.info("Telemetry server starting up.");
				while (!shutdown) {
					try {
						if (!printedWaiting) {
							logger.debug("Waiting for message");
							printedWaiting = true;
						}
						TextMessage message = (TextMessage) consumer.receive(ONE_SECOND);

						if (message != null) {
							printedWaiting = false;
							logger.debug("Got message '{}', correlationID {}", message.getText(), message.getJMSCorrelationID());
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
										logger.error("Attempting to close stream but no open stream for correlationID {}", correlationID);
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
									} else {
										logger.error("Attempting to write to stream but no open stream for correlationID {}", correlationID);
									}
								}
							}

							// As well as logging the message and even if we're outside of an event stream, if an exception is detected then
							// route it to email
							if (message.getStringProperty("level") != null && message.getStringProperty("level").equals("ERROR")) {
								try {
									sendEmailAlert(message);
								} catch (Exception e) {
									logger.error("Unable to send email alert to report: " + message.getText(), e);
								}
							}
						}
					} catch (IllegalStateException e) {
						logger.info("Connection closed. Shutting down telemetry consumer.");
						shutdown = true;
					} catch (JMSException e) {
						Exception linkedException = e.getLinkedException();
						if (linkedException != null && linkedException.getClass().equals(TransportDisposedIOException.class)) {
							logger.info("Transport disposed. Shutting down telemetry consumer.");
							shutdown = true;
						} else {
							logger.error("JMSException", e);
						}
					} catch (IOException e) {
						logger.error("Problem with output writer.", e);
					}
				}
			}

			private void sendEmailAlert(TextMessage message) throws MalformedURLException, EmailException, JMSException {
				// Do we have an EmailSender configured?
				if (TelemetryProcessor.emailSender == null || TelemetryProcessor.defaultEmailToAddr == null
						|| TelemetryProcessor.defaultEmailToAddr.isEmpty()) {
					logger.info("EmailSender not configured.  Unable to report error message: " + message.getText());
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

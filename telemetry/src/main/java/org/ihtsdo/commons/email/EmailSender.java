package org.ihtsdo.commons.email;

import java.net.MalformedURLException;

import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;

public class EmailSender {

	private final String smtpHost;
	private final int smtpPort;
	private final String smtpUsername;
	private final String smtpPassword;
	private final boolean smtpSsl;

	public EmailSender(final String smtpHost, final int smtpPort, final String smtpUsername, final String smtpPassword,
			final boolean smtpSsl) {
		this.smtpHost = smtpHost;
		this.smtpPort = smtpPort;
		this.smtpUsername = smtpUsername;
		this.smtpPassword = smtpPassword;
		this.smtpSsl = smtpSsl;
	}

	public void send(EmailRequest request) throws EmailException, MalformedURLException {

		SimpleEmail email = new SimpleEmail();
		email.setFrom(request.getFromEmail(), request.getFromName());
		email.setSubject(request.getSubject());
		email.addTo(request.getToEmail());
		email.setMsg(request.getTextBody());

		addStandardDetails(email);
		email.send();
	}

	private void addStandardDetails(Email email) throws EmailException {
		email.setHostName(smtpHost);
		email.setSmtpPort(smtpPort);
		email.setAuthenticator(new DefaultAuthenticator(smtpUsername, smtpPassword));
		email.setSSLOnConnect(smtpSsl);
		// If we're using SSL, then the port specified will be the port to use for sSmtp also
		if (smtpSsl) {
			email.setSslSmtpPort(Integer.toString(smtpPort));
		}
	}

	public String toString() {
		return smtpUsername + " via " + smtpHost + ":" + smtpPort + (smtpSsl ? " with ssl" : " without ssl");
	}
}

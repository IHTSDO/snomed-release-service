package org.ihtsdo.buildcloud.service.exception;

public class BadConfigurationException extends Exception {

	public BadConfigurationException(String message) {
		super(message);
	}

	public BadConfigurationException(String message, Throwable cause) {
		super(message, cause);
	}

}

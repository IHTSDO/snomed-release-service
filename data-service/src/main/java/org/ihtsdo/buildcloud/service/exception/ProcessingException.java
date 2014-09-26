package org.ihtsdo.buildcloud.service.exception;

public class ProcessingException extends Exception {

	public ProcessingException(String message, Throwable cause) {
		super(message, cause);
	}

	public ProcessingException(String message) {
		super(message);
	}
}

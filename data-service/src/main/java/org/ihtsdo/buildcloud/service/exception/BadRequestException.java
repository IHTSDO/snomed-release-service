package org.ihtsdo.buildcloud.service.exception;

public class BadRequestException extends Exception {

	public BadRequestException(String message) {
		super(message);
	}

	public BadRequestException(String message, Throwable cause) {
		super(message, cause);
	}

}

package org.ihtsdo.buildcloud.service.exception;

public class BadRequestException extends BusinessServiceException {

	public BadRequestException(String message) {
		super(message);
	}

	public BadRequestException(String message, Throwable cause) {
		super(message, cause);
	}

}

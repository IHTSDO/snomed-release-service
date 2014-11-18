package org.ihtsdo.buildcloud.service.exception;

public class BusinessServiceException extends Exception {

	public BusinessServiceException(String message) {
		super(message);
	}

	public BusinessServiceException(Throwable cause) {
		super(cause);
	}

	public BusinessServiceException(String message, Throwable cause) {
		super(message, cause);
	}
}

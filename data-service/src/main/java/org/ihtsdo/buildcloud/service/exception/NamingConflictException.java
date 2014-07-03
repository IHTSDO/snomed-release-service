package org.ihtsdo.buildcloud.service.exception;

public class NamingConflictException extends Exception {

	public NamingConflictException(String message) {
		super(message);
	}

	public NamingConflictException(String message, Throwable cause) {
		super(message, cause);
	}

}

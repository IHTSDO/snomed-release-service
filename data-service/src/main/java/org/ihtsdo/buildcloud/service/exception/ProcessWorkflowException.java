package org.ihtsdo.buildcloud.service.exception;

public class ProcessWorkflowException extends Exception {

	public ProcessWorkflowException(String message) {
		super(message);
	}

	public ProcessWorkflowException(String message, Throwable cause) {
		super(message, cause);
	}

}

package org.ihtsdo.buildcloud.service.exception;

public class ResourceNotFoundException extends BusinessServiceException {

	private static final long serialVersionUID = -4281549626769059242L;

	public ResourceNotFoundException(String message) {
		super(message);
	}

	public ResourceNotFoundException(Throwable cause) {
		super(cause);
	}

	public ResourceNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}

}

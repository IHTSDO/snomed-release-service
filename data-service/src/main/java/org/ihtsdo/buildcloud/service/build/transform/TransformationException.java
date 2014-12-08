package org.ihtsdo.buildcloud.service.build.transform;

public class TransformationException extends Exception {

	public TransformationException(String message) {
		super(message);
	}

	public TransformationException(String message, Throwable cause) {
		super(message, cause);
	}

}

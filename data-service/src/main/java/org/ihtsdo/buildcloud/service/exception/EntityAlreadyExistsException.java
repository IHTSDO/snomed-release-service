package org.ihtsdo.buildcloud.service.exception;

public class EntityAlreadyExistsException extends Exception {

	private static final long serialVersionUID = -652015419944747839L;

	public EntityAlreadyExistsException(String message) {
		super(message);
	}
}

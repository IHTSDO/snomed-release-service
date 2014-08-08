package org.ihtsdo.buildcloud.service.exception;

public class EntityAlreadyExistsException extends Exception {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	public EntityAlreadyExistsException() {
		super("Entity already exists.");
	}

	public EntityAlreadyExistsException(String string) {
		super("Entity already exists.");
	}
}

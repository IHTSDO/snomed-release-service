package org.ihtsdo.buildcloud.service.exception;

public class EntityAlreadyExistsException extends Exception {

	public EntityAlreadyExistsException() {
		super("Entity already exists.");
	}
}

package org.ihtsdo.buildcloud.service.execution;

/**
 * Exception occurred during release file generation.
 */
public class ReleaseFileGenerationException extends Exception {

	/**
	 * @param errorMsg error message
	 * @param cause    The cause of the exception
	 */
	public ReleaseFileGenerationException(String errorMsg, Throwable cause) {
		super(errorMsg, cause);
	}

	/**
	 * @param errorMsg error message.
	 */
	public ReleaseFileGenerationException(String errorMsg) {
		super(errorMsg);
	}

}

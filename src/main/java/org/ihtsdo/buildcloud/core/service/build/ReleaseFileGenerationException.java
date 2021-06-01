package org.ihtsdo.buildcloud.core.service.build;

import org.ihtsdo.otf.rest.exception.BusinessServiceException;

/**
 * Exception occurred during release file generation.
 */
public class ReleaseFileGenerationException extends BusinessServiceException {

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

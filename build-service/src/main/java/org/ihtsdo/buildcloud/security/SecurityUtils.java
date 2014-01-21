package org.ihtsdo.buildcloud.security;

import javax.servlet.ServletRequest;

/**
 * Dummy security utils to get us started.
 */
public class SecurityUtils {

	public static final String AUTHENTICATED_ID = "org.ihtsdo.buildcloud.security.AuthenticatedId";

	/**
	 * Dummy implementation - accepts any id.
	 *
	 * @param credentials
	 * @param request
	 * @return
	 */
	public String authenticate(String[] credentials, ServletRequest request) {
		if (credentials != null && credentials.length > 0) {
			String authenticatedId = credentials[0];
			request.setAttribute(AUTHENTICATED_ID, authenticatedId);
			return authenticatedId;
		} else {
			return null;
		}
	}

	public String getAuthenticatedId(ServletRequest request) {
		return (String) request.getAttribute(AUTHENTICATED_ID);
	}

}

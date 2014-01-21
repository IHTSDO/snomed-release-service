package org.ihtsdo.buildcloud.security;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.DatatypeConverter;
import java.io.IOException;

public class SecurityFilter implements Filter {

	public void init(FilterConfig config) throws ServletException {
	}

	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws ServletException, IOException {
		SecurityHelper.clearSubject();

		final HttpServletRequest httpRequest = (HttpServletRequest) request;
		final HttpServletResponse httpResponse = (HttpServletResponse) response;
		final String auth = httpRequest.getHeader("Authorization");

		String authenticatedId = null;
		if ( auth != null) {
			final int index = auth.indexOf(' ');
			if ( index > 0 ) {

			}
			String credsString = new String(DatatypeConverter.parseBase64Binary(auth.substring(index)));
			String[] credentials = credsString.split(":");

			// Todo: implement real login here
			if (credentials != null && credentials.length > 0) {
				authenticatedId = credentials[0];
			}
		}

		if (authenticatedId != null) {
			try {
				// Bind authenticated subject/user to thread
				SecurityHelper.setSubject(authenticatedId);

				chain.doFilter(request, response);
			} finally {
				SecurityHelper.clearSubject();
			}
		} else {
			httpResponse.setHeader("WWW-Authenticate", "Basic realm=\"API\"");
			httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED);
		}
	}

	public void destroy() {
	}

}

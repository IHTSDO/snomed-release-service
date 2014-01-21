package org.ihtsdo.buildcloud.security;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.DatatypeConverter;
import java.io.IOException;

public class SecurityFilter implements Filter {

	private SecurityUtils securityUtils;

	public void init(FilterConfig config) throws ServletException {
		securityUtils = new SecurityUtils();
	}

	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws ServletException, IOException {
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
			authenticatedId = securityUtils.authenticate(credentials, request);
		}
		if (authenticatedId != null) {
			chain.doFilter(request, response);
		} else {
			httpResponse.setHeader("WWW-Authenticate", "Basic realm=\"API\"");
			httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED);
		}
	}

	public void destroy() {
	}

}

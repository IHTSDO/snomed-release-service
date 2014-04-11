package org.ihtsdo.buildcloud.security;

import org.ihtsdo.buildcloud.entity.User;
import org.ihtsdo.buildcloud.service.AuthenticationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.DatatypeConverter;
import java.io.IOException;

public class SecurityFilter implements Filter {

	private AuthenticationService authenticationService;
	private static final Logger LOGGER = LoggerFactory.getLogger(SecurityFilter.class);

	public void init(FilterConfig config) throws ServletException {
		ApplicationContext applicationContext = WebApplicationContextUtils.getWebApplicationContext(config.getServletContext());
		authenticationService = applicationContext.getBean(AuthenticationService.class);
	}

	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws ServletException, IOException {
		SecurityHelper.clearSubject();

		final HttpServletRequest httpRequest = (HttpServletRequest) request;

		String pathInfo = httpRequest.getPathInfo();
		LOGGER.info("pathInfo: '{}'", pathInfo);
		if (pathInfo.equals("/") || pathInfo.startsWith("/login")) {
			// Trying to log in. Pass through to login controller.
			chain.doFilter(request, response);
		} else {
			final HttpServletResponse httpResponse = (HttpServletResponse) response;
			final String auth = httpRequest.getHeader("Authorization");

			User authenticatedSubject = null;
			if (auth != null) {
				final int index = auth.indexOf(' ');
				if (index > 0) {
					String credsString = new String(DatatypeConverter.parseBase64Binary(auth.substring(index)));
					String[] credentials = credsString.split(":");

					if (credentials != null && credentials.length > 0) {
						String authenticationToken = credentials[0];
						authenticatedSubject = authenticationService.getAuthenticatedSubject(authenticationToken);
					}
				}
			}

			if (authenticatedSubject != null) {
				try {
					// Bind authenticated subject/user to thread
					SecurityHelper.setSubject(authenticatedSubject);

					chain.doFilter(request, response);
				} finally {
					SecurityHelper.clearSubject();
				}
			} else {
				httpResponse.setHeader("WWW-Authenticate", "Basic realm=\"API\"");
				httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED);
			}
		}
	}

	public void destroy() {
	}

}

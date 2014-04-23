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
		final HttpServletResponse httpResponse = (HttpServletResponse) response;
		final String authHeader = httpRequest.getHeader("Authorization");

		String pathInfo = httpRequest.getPathInfo();
		String requestMethod = httpRequest.getMethod();
		LOGGER.debug("pathInfo: '{}'", pathInfo);

		if (pathInfo.startsWith("/login")) {
			// Trying to log in
			chain.doFilter(request, response);
		} else if (authHeader == null && requestMethod.equals("GET")) {
			// An anonymous GET
			User anonymousSubject = authenticationService.getAnonymousSubject();
			SecurityHelper.setSubject(anonymousSubject);
			chain.doFilter(request, response);
		} else if (authHeader != null){
			User authenticatedSubject = null;
			if (authHeader != null) {
				final int index = authHeader.indexOf(' ');
				if (index > 0) {
					String credsString = new String(DatatypeConverter.parseBase64Binary(authHeader.substring(index)));
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
			}
			httpResponse.setHeader("WWW-Authenticate", "Basic realm=\"API Authentication Token\"");
			httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		} else {
			httpResponse.setHeader("WWW-Authenticate", "Basic realm=\"API Authentication Token\"");
			httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		}
	}

	public void destroy() {
	}

}

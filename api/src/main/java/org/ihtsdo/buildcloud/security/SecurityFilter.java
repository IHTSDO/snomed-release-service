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
	
	private static final String AUTH_TOKEN_NAME = "auth_token";

	public void init(FilterConfig config) throws ServletException {
		ApplicationContext applicationContext = WebApplicationContextUtils.getWebApplicationContext(config.getServletContext());
		authenticationService = applicationContext.getBean(AuthenticationService.class);
	}

	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws ServletException, IOException {
		SecurityHelper.clearSubject();

		final HttpServletRequest httpRequest = (HttpServletRequest) request;
		final HttpServletResponse httpResponse = (HttpServletResponse) response;
		final String authHeader = httpRequest.getHeader("Authorization");
		//For a file upload we're sending the auth token in a hidden input element
		final String authParameter = request.getParameter(AUTH_TOKEN_NAME);

		String pathInfo = httpRequest.getPathInfo();
		String requestMethod = httpRequest.getMethod();
		LOGGER.debug("pathInfo: '{}' from {}", pathInfo, httpRequest.getRemoteAddr());
		User validUser = null;

		if (pathInfo != null && pathInfo.startsWith("/login")) {
			// Trying to log in
			validUser = authenticationService.getAnonymousSubject();
		} else if (authHeader == null && requestMethod.equals("GET")) {
			// An anonymous GET
			validUser = authenticationService.getAnonymousSubject();
		} else if (authHeader == null && requestMethod.equals("POST") && authParameter != null) {
			String authenticationToken = new String(DatatypeConverter.parseBase64Binary(authParameter));
			validUser = authenticationService.getAuthenticatedSubject(authenticationToken);
		} else if (authHeader != null){
			final int index = authHeader.indexOf(' ');
			if (index > 0) {
				String credsString = new String(DatatypeConverter.parseBase64Binary(authHeader.substring(index)));
				String[] credentials = credsString.split(":");

				if (credentials != null && credentials.length > 0) {
					String authenticationToken = credentials[0];
					validUser = authenticationService.getAuthenticatedSubject(authenticationToken);
				}
			}
		}

		if (validUser != null) {
			try {
				// Bind authenticated subject/user to thread
				SecurityHelper.setSubject(validUser);
				chain.doFilter(request, response);
			} finally {
				SecurityHelper.clearSubject();
			}			
		} else {
			httpResponse.setHeader("WWW-Authenticate", "Basic realm=\"API Authentication Token\"");
			httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		} 
		
	}

	public void destroy() {
	}

}

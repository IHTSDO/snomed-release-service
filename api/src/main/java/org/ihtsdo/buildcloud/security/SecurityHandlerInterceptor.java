package org.ihtsdo.buildcloud.security;

import org.ihtsdo.buildcloud.entity.User;
import org.ihtsdo.buildcloud.service.AuthenticationService;
import org.ihtsdo.buildcloud.service.security.SecurityHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.DatatypeConverter;

public class SecurityHandlerInterceptor implements HandlerInterceptor {

	@Autowired
	private AuthenticationService authenticationService;

	private static final String AUTH_TOKEN_NAME = "X-AUTH-token";

	private static final Logger LOGGER = LoggerFactory.getLogger(SecurityHandlerInterceptor.class);

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
		SecurityHelper.clearUser();
		final String authHeader = request.getHeader("Authorization");
		//For a file upload we're sending the auth token in a hidden input element
		final String authParameter = request.getParameter(AUTH_TOKEN_NAME);
		
		String pathInfo = request.getPathInfo();
		String requestMethod = request.getMethod();
		LOGGER.debug("pathInfo: '{}' from {}", pathInfo, request.getRemoteAddr());
		User validUser = null;

		if (pathInfo != null && pathInfo.startsWith("/login")) {
			// Trying to log in
			validUser = authenticationService.getAnonymousSubject();
		} else if (authHeader == null && (requestMethod.equals("GET") || requestMethod.equals("HEAD"))) {
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
				if (credentials.length == 1) {
					String authenticationToken = credentials[0];
					validUser = authenticationService.getAuthenticatedSubject(authenticationToken);
				} else if (credentials.length == 2) {
					String token = authenticationService.authenticate(credentials[0], credentials[1]);
					if (token != null) {
						validUser = authenticationService.getAuthenticatedSubject(token);
					}
				}
			}
		}

		if (validUser != null) {
			// Bind authenticated subject/user to thread
			SecurityHelper.setUser(validUser);
			return true;
		} else {
			response.setHeader("WWW-Authenticate", "Basic realm=\"API Authentication Token\"");
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			return false;
		}
	}

	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
		SecurityHelper.clearUser();
	}

	@Override
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
	}

}

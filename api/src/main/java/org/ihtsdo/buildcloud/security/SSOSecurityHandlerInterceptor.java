package org.ihtsdo.buildcloud.security;

import org.ihtsdo.buildcloud.entity.User;
import org.ihtsdo.buildcloud.service.AuthenticationService;
import org.ihtsdo.buildcloud.service.security.SecurityHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class SSOSecurityHandlerInterceptor implements HandlerInterceptor {

    @Autowired
    private AuthenticationService authenticationService;

    private static final Logger LOGGER = LoggerFactory.getLogger(SSOSecurityHandlerInterceptor.class);

    private static final String TOKEN = "X-AUTH-token";


    @Override
    public boolean preHandle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object o) throws Exception {
        String pathInfo = httpServletRequest.getPathInfo();
        LOGGER.debug("pathInfo: '{}' from {}", pathInfo, httpServletRequest.getRemoteAddr());
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User validUser = null;
        if("GET".equals(httpServletRequest.getMethod()) || "HEAD".equals(httpServletRequest.getMethod()) || authentication != null ) {
            validUser = authenticationService.getAnonymousSubject();
        }
        if (validUser != null) {
            // Bind authenticated subject/user to thread
            SecurityHelper.setUser(validUser);
            return true;
        } else {
            httpServletResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }
    }

    @Override
    public void postHandle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object o, ModelAndView modelAndView) throws Exception {

    }

    @Override
    public void afterCompletion(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object o, Exception e) throws Exception {
        SecurityHelper.clearUser();
    }
}

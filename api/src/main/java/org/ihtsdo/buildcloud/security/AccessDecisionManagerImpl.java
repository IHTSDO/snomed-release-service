package org.ihtsdo.buildcloud.security;

import org.springframework.security.access.AccessDecisionManager;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;

import java.util.Collection;

public class AccessDecisionManagerImpl implements AccessDecisionManager {

	@Override
	public void decide(Authentication authentication, Object o, Collection<ConfigAttribute> configAttributes) throws AccessDeniedException, InsufficientAuthenticationException {

	}

	@Override
	public boolean supports(ConfigAttribute configAttribute) {
		return false;
	}

	@Override
	public boolean supports(Class<?> aClass) {
		return false;
	}

}

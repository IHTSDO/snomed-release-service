package org.ihtsdo.buildcloud.service;

import org.ihtsdo.buildcloud.entity.User;

public interface AuthenticationService {

	String authenticate(String username, String password);

	User getAuthenticatedSubject(String authenticationToken);

	User getAnonymousSubject();

}

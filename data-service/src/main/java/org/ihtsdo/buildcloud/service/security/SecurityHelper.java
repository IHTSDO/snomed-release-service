package org.ihtsdo.buildcloud.service.security;

import org.ihtsdo.buildcloud.entity.User;
import org.ihtsdo.otf.rest.exception.AuthenticationException;

import java.util.HashMap;
import java.util.Map;

/**
 * Binds the authenticated user id to the current thread.
 * This is the principal Apache Shiro uses.
 */
public class SecurityHelper {

	private static final Map<Thread, User> threadUsers = new HashMap<>();

	public static void clearUser() {
		synchronized (threadUsers) {
			threadUsers.remove(Thread.currentThread());
		}
	}

	public static void setUser(User authenticatedSubject) {
		synchronized (threadUsers) {
			threadUsers.put(Thread.currentThread(), authenticatedSubject);
		}
	}

	private static User getUser() {
		synchronized (threadUsers) {
			return threadUsers.get(Thread.currentThread());
		}
	}

	public static User getRequiredUser() throws AuthenticationException {
		User user = getUser();
		return user;

	}

}

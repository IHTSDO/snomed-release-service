package org.ihtsdo.buildcloud.security;

import org.ihtsdo.buildcloud.entity.User;

import java.util.HashMap;

/**
 * Binds the authenticated user id to the current thread.
 * This is the principal Apache Shiro uses.
 */
public class SecurityHelper {

	private static final HashMap<Thread, User> threadSubjects = new HashMap<>();

	public static void clearSubject() {
		synchronized (threadSubjects) {
			threadSubjects.remove(Thread.currentThread());
		}
	}

	public static void setSubject(User authenticatedSubject) {
		synchronized (threadSubjects) {
			threadSubjects.put(Thread.currentThread(), authenticatedSubject);
		}
	}

	public static User getSubject() {
		synchronized (threadSubjects) {
			return threadSubjects.get(Thread.currentThread());
		}
	}

}

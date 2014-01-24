package org.ihtsdo.buildcloud.security;

import java.util.HashMap;

/**
 * Binds the authenticated user id to the current thread.
 * This is the principal Apache Shiro uses.
 */
public class SecurityHelper {

	private static final HashMap<Thread, String> threadSubjects = new HashMap<>();

	public static void clearSubject() {
		synchronized (threadSubjects) {
			threadSubjects.remove(Thread.currentThread());
		}
	}

	public static void setSubject(String authenticatedId) {
		synchronized (threadSubjects) {
			threadSubjects.put(Thread.currentThread(), authenticatedId);
		}
	}

	public static String getSubject() {
		synchronized (threadSubjects) {
			return threadSubjects.get(Thread.currentThread());
		}
	}

}

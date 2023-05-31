package org.ihtsdo.buildcloud.core.service.monitor;

public class MonitorException extends Exception {
	public MonitorException(String message) {
		super(message);
	}

	public MonitorException(String message, Throwable cause) {
		super(message, cause);
	}
}

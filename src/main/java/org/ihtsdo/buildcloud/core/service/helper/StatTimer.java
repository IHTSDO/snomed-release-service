package org.ihtsdo.buildcloud.core.service.helper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

public class StatTimer {

	private final Class<?> aClass;
	private long start;
	private long split;
	private static final Logger LOGGER = LoggerFactory.getLogger(StatTimer.class);
	private String targetEntity;

	public StatTimer(Class<?> aClass) {
		this.aClass = aClass;
		split();
	}

	public void split() {
		split = getMilis();
	}

	public void logTimeTaken(String message) {
		long now = getMilis();
		long taken = now - split;
		split = now;
		LOGGER.info("{}{}, {} time taken: {} seconds", aClass.getName(), getTargetString(), message, getSeconds(taken));
	}

	public String getTargetEntity() {
		return targetEntity;
	}

	public void setTargetEntity(String targetEntity) {
		this.targetEntity = targetEntity;
	}

	private float getSeconds(long milis) {
		return (float) milis / 1000f;
	}

	private long getMilis() {
		return new Date().getTime();
	}

	private String getTargetString() {
		return targetEntity != null ? " with " + targetEntity : "";
	}
}

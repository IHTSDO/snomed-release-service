package org.ihtsdo.buildcloud.entity;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.FastDateFormat;

import java.util.Date;

/**
 * An Execution is a snapshot of a Build and may be used to run the release process.
 *
 * This entity is stored via S3, not Hibernate.
 */
public class Execution {

	public static final FastDateFormat DATE_FORMAT = DateFormatUtils.ISO_DATETIME_FORMAT;

	private Status status;
	private final String creationTimeString;
	private final String jsonConfiguration;
	private final Build build;

	public static enum Status {
		PRE_EXECUTION
	}

	public Execution(Date creationTime, String jsonConfiguration, Build build) {
		this.creationTimeString = DATE_FORMAT.format(creationTime);
		this.jsonConfiguration = jsonConfiguration;
		this.build = build;
		this.status = Status.PRE_EXECUTION;
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	public String getCreationTimeString() {
		return creationTimeString;
	}

	public String getJsonConfiguration() {
		return jsonConfiguration;
	}

	public Build getBuild() {
		return build;
	}

}

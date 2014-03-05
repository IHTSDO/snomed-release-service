package org.ihtsdo.buildcloud.entity;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.codehaus.jackson.annotate.JsonIgnore;

import java.util.Date;

/**
 * An Execution is a snapshot of a Build and may be used to run the release process.
 *
 * This entity is stored via S3, not Hibernate.
 */
public class Execution {

	public static final FastDateFormat DATE_FORMAT = DateFormatUtils.ISO_DATETIME_FORMAT;

	private final String creationTime;

	private Status status;

	@JsonIgnore
	private final Build build;

	public static enum Status {
		PRE_EXECUTION
	}

	public Execution(String creationTime, String statusString, Build build) {
		this.creationTime = creationTime;
		this.status = Status.valueOf(statusString);
		this.build = build;
	}

	public Execution(Date creationTime, Build build) {
		this.creationTime = DATE_FORMAT.format(creationTime);
		this.status = Status.PRE_EXECUTION;
		this.build = build;
	}

	public String getId() {
		return creationTime;
	}

	public String getCreationTime() {
		return creationTime;
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	public Build getBuild() {
		return build;
	}

}

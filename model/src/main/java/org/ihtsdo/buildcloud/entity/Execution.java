package org.ihtsdo.buildcloud.entity;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.ihtsdo.buildcloud.entity.helper.EntityHelper;

import java.util.Date;

/**
 * An Execution is a snapshot of a Build and may be used to run the release process.
 *
 * This entity is stored via S3, not Hibernate.
 */
public class Execution  implements DomainEntity{

	private final String creationTime;

	private Status status;

	@JsonIgnore
	private final Build build;

	public static enum Status {
		BEFORE_TRIGGER, QUEUED, BUILDING, BUILT
	}

	public Execution(String creationTime, String statusString, Build build) {
		this.creationTime = creationTime;
		this.status = Status.valueOf(statusString);
		this.build = build;
	}

	public Execution(Date creationTime, Build build) {
		this.creationTime = EntityHelper.formatAsIsoDateTime(creationTime);
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

	@Override
	public DomainEntity getParent() {
		return build;
	}

	@Override
	public String getCollectionName() {
		return "executions";
	}

	@Override
	public String getBusinessKey() {
		return null;
	}
}

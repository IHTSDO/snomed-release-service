package org.ihtsdo.buildcloud.entity;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.ihtsdo.buildcloud.entity.helper.EntityHelper;

import java.util.Date;
import java.util.List;

/**
 * An Execution is a snapshot of a Build and may be used to run the release process.
 *
 * This entity is stored via S3, not Hibernate.
 */
@JsonPropertyOrder({"id", "name"})
public class Execution {

	private final String creationTime;

	private Status status;

	@JsonIgnore
	private final Build build;
	
	private List<PreConditionCheckReport> preConditionCheckReports;

	ExecutionReport executionReport;

	public static enum Status {
		BEFORE_TRIGGER, FAILED_PRE_CONDITIONS, BUILDING, BUILT, UNKNOWN
	}

	public Execution(Build build, String creationTime) {
		this.status = Status.BEFORE_TRIGGER;
		this.executionReport = new ExecutionReport();
		this.build = build;
		this.creationTime = creationTime;
	}

	public Execution(String creationTime, String statusString, Build build) {
		this(build, creationTime);
		try {
			this.status = Status.valueOf(statusString);
		} catch (IllegalArgumentException e) {
			this.status = Status.UNKNOWN;
		}
	}

	public Execution(Date creationTime, Build build) {
		this(build, EntityHelper.formatAsIsoDateTime(creationTime));
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

	public String getUniqueId() {
		return getBuild().getBusinessKey() + "|" + getId();
	}

	public List<PreConditionCheckReport> getPreConditionCheckReports() {
		return preConditionCheckReports;
	}

	public void setPreConditionCheckReports(List<PreConditionCheckReport> preConditionCheckReports) {
		this.preConditionCheckReports = preConditionCheckReports;
	}

	public ExecutionReport getExecutionReport() {
		return executionReport;
	}

	public void setExecutionReport(ExecutionReport executionReport) {
		this.executionReport = executionReport;
	}
}

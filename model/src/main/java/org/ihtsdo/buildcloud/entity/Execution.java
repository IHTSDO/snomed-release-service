package org.ihtsdo.buildcloud.entity;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.ihtsdo.buildcloud.entity.helper.EntityHelper;

import java.util.Date;
import java.util.List;
import java.util.Map;

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
	
	private Map<String, List<PreConditionCheckReport>> preConditionCheckReports;

	ExecutionReport executionReport;

	public static enum Status {
		BEFORE_TRIGGER, FAILED_PRE_CONDITIONS, BUILDING, BUILT, UNKNOWN
	}

	public Execution(String creationTime, String statusString, Build build) {
		this.creationTime = creationTime;
		try {
			this.status = Status.valueOf(statusString);
		} catch (IllegalArgumentException e) {
			this.status = Status.UNKNOWN;
		}
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

	public String getUniqueId() {
		return getBuild().getCompositeKey() + "|" + getId();
	}

	public void setPreConditionCheckReports(Map<String, List<PreConditionCheckReport>> preConditionReports) {
		this.preConditionCheckReports = preConditionReports;
	}

	public Map<String, List<PreConditionCheckReport>> getPreConditionCheckReports() {
		return preConditionCheckReports;
	}

	public ExecutionReport getExecutionReport() {
		if (executionReport == null) {
			this.executionReport = new ExecutionReport();
		}
		return executionReport;
	}

	public void setExecutionReport(ExecutionReport executionReport) {
		this.executionReport = executionReport;
	}

}

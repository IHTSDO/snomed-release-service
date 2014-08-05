package org.ihtsdo.buildcloud.entity;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.ihtsdo.buildcloud.entity.helper.EntityHelper;

import java.util.Date;
import java.util.HashMap;
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

	private Map<String, Map<String, String>> executionReport;

	public static enum Status {
		BEFORE_TRIGGER, FAILED_PRE_CONDITIONS, QUEUED, BUILDING, BUILT
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

	public void setPreConditionCheckReports(Map<String, List<PreConditionCheckReport>> preConditionReports) {
		this.preConditionCheckReports = preConditionReports;
	}

	public Map<String, List<PreConditionCheckReport>> getPreConditionCheckReports() {
		return preConditionCheckReports;
	}

	public Map<String, Map<String, String>> getExecutionReport() {
		return executionReport;
	}

	public void setExecutionReport(Map<String, Map<String, String>> executionReport) {
		this.executionReport = executionReport;
	}

	public void addToExecutionReport(Package pkg, String executionStage, String result) {
		// Do we have an execution Report object
		if (this.executionReport == null) {
			this.executionReport = new HashMap<String, Map<String, String>>();
		}

		// Do we already know about this package?
		if (!this.executionReport.containsKey(pkg.getName())) {
			this.executionReport.put(pkg.getName(), (Map<String, String>) new HashMap<String, String>());
		}

		// Add/Replace this report item
		Map<String, String> reportItem = this.executionReport.get(pkg.getName());
		reportItem.put(executionStage, result);
	}

}

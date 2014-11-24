package org.ihtsdo.buildcloud.entity;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.ihtsdo.buildcloud.entity.helper.EntityHelper;

import java.util.Date;
import java.util.List;

/**
 * An Build is a snapshot of a Product and may be used to run the release process.
 *
 * This entity is stored via S3, not Hibernate.
 */
@JsonPropertyOrder({"id", "name"})
public class Build {

	private final String creationTime;

	private Status status;

	@JsonIgnore
	private final Product product;
	
	private List<PreConditionCheckReport> preConditionCheckReports;

	BuildReport buildReport;

	public static enum Status {
		BEFORE_TRIGGER, FAILED_PRE_CONDITIONS, BUILDING, BUILT, UNKNOWN
	}

	public Build(Product product, String creationTime) {
		this.status = Status.BEFORE_TRIGGER;
		this.buildReport = new BuildReport();
		this.product = product;
		this.creationTime = creationTime;
	}

	public Build(String creationTime, String statusString, Product product) {
		this(product, creationTime);
		try {
			this.status = Status.valueOf(statusString);
		} catch (IllegalArgumentException e) {
			this.status = Status.UNKNOWN;
		}
	}

	public Build(Date creationTime, Product product) {
		this(product, EntityHelper.formatAsIsoDateTime(creationTime));
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

	public Product getProduct() {
		return product;
	}

	public String getUniqueId() {
		return getProduct().getBusinessKey() + "|" + getId();
	}

	public List<PreConditionCheckReport> getPreConditionCheckReports() {
		return preConditionCheckReports;
	}

	public void setPreConditionCheckReports(List<PreConditionCheckReport> preConditionCheckReports) {
		this.preConditionCheckReports = preConditionCheckReports;
	}

	public BuildReport getBuildReport() {
		return buildReport;
	}

	public void setBuildReport(BuildReport buildReport) {
		this.buildReport = buildReport;
	}
}

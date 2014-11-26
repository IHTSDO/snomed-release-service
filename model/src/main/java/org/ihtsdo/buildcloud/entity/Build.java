package org.ihtsdo.buildcloud.entity;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.ihtsdo.buildcloud.entity.helper.EntityHelper;

import java.util.Date;
import java.util.List;
import javax.persistence.Transient;

/**
 * A Build is a snapshot of a Product and may be used to run the release process.
 *
 * This entity is stored via S3, not Hibernate.
 */
@JsonPropertyOrder({"id", "name"})
public class Build {

	private final String creationTime;

	private Status status;

	private BuildConfiguration configuration;

	private String productBusinessKey;

	private List<PreConditionCheckReport> preConditionCheckReports;

	BuildReport buildReport;

	@Transient
	private Product product;

	public static enum Status {
		BEFORE_TRIGGER, FAILED_PRE_CONDITIONS, BUILDING, BUILT, UNKNOWN
	}

	private Build(String creationTime, String productBusinessKey, BuildConfiguration configuration) {
		this.buildReport = new BuildReport();
		this.productBusinessKey = productBusinessKey;
		this.creationTime = creationTime;
		this.configuration = configuration;
	}

	public Build(String creationTime, String productBusinessKey, String statusString) {
		this(creationTime, productBusinessKey, null, statusString);
	}

	public Build(String creationTime, String productBusinessKey, BuildConfiguration configuration, String statusString) {
		this(creationTime, productBusinessKey, configuration);
		try {
			this.status = Status.valueOf(statusString);
		} catch (IllegalArgumentException e) {
			this.status = Status.UNKNOWN;
		}
	}

	public Build(Date creationTime, Product product) {
		this(EntityHelper.formatAsIsoDateTime(creationTime), product.getBusinessKey(), product.getBuildConfiguration());
		this.product = product;
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

	public String getUniqueId() {
		return productBusinessKey + "|" + getId();
	}

	@JsonIgnore // BuildConfiguration is not loaded when listing Builds for efficiency
	public BuildConfiguration getConfiguration() {
		return configuration;
	}

	public void setConfiguration(BuildConfiguration configuration) {
		this.configuration = configuration;
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

	@JsonIgnore
	public Product getProduct() {
		return product;
	}

	public void setProduct(Product product) {
		this.product = product;
	}
}

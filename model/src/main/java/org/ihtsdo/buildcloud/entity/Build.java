package org.ihtsdo.buildcloud.entity;

import java.util.Date;
import java.util.List;

import javax.persistence.Transient;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.ihtsdo.buildcloud.entity.helper.EntityHelper;

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

	private final String productBusinessKey;

	private List<PreConditionCheckReport> preConditionCheckReports;
	
	private BuildReport buildReport;
	
	@Transient
	private Product product;

	private QATestConfig qaTestConfig;

	public static enum Status {
		BEFORE_TRIGGER, FAILED_PRE_CONDITIONS, BUILDING, BUILT, UNKNOWN
	}

	private Build(final String creationTime, final String productBusinessKey, final BuildConfiguration configuration, final QATestConfig qaTestConfig) {
		this.buildReport = new BuildReport();
		this.productBusinessKey = productBusinessKey;
		this.creationTime = creationTime;
		this.configuration = configuration;
	}

	public Build(final String creationTime, final String productBusinessKey, final String statusString) {
		this(creationTime, productBusinessKey, null, null,statusString);
	}

	public Build(final String creationTime, final String productBusinessKey, final BuildConfiguration configuration, final QATestConfig qaTestConfig, final String statusString) {
		this(creationTime, productBusinessKey, configuration, qaTestConfig);
		try {
			this.status = Status.valueOf(statusString);
		} catch (final IllegalArgumentException e) {
			this.status = Status.UNKNOWN;
		}
	}

	public Build(final Date creationTime, final Product product) {
		this(EntityHelper.formatAsIsoDateTime(creationTime), product.getBusinessKey(), product.getBuildConfiguration(), product.getQaTestConfig());
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

	public void setStatus(final Status status) {
		this.status = status;
	}

	public String getUniqueId() {
		return productBusinessKey + "|" + getId();
	}

	@JsonIgnore // BuildConfiguration is not loaded when listing Builds for efficiency
	public BuildConfiguration getConfiguration() {
		return configuration;
	}

	public void setConfiguration(final BuildConfiguration configuration) {
		this.configuration = configuration;
	}

	public List<PreConditionCheckReport> getPreConditionCheckReports() {
		return preConditionCheckReports;
	}

	public void setPreConditionCheckReports(final List<PreConditionCheckReport> preConditionCheckReports) {
		this.preConditionCheckReports = preConditionCheckReports;
	}

	public BuildReport getBuildReport() {
		return buildReport;
	}

	public void setBuildReport(final BuildReport buildReport) {
		this.buildReport = buildReport;
	}

	@JsonIgnore
	public Product getProduct() {
		return product;
	}

	public void setProduct(final Product product) {
		this.product = product;
	}
	
	@JsonIgnore
	public QATestConfig getQaTestConfig() {
		return qaTestConfig;
	}

	public void setQaTestConfig(final QATestConfig assertionTestConfiguration) {
		this.qaTestConfig = assertionTestConfiguration;
	}
}

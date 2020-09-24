package org.ihtsdo.buildcloud.entity;

import java.util.Date;
import java.util.List;

import javax.persistence.Transient;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
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

	private Tag tag;

	private String buildUser;

	private String rvfURL;

	private BuildConfiguration configuration;

	private final String productBusinessKey;

	private List<PreConditionCheckReport> preConditionCheckReports;

	private BuildReport buildReport;

	@Transient
	private Product product;

	private QATestConfig qaTestConfig;

	public static enum Status {
		BEFORE_TRIGGER, FAILED_INPUT_PREPARE_REPORT_VALIDATION, FAILED_PRE_CONDITIONS, BUILDING, BUILT, UNKNOWN, CANCEL_REQUESTED, CANCELLED, FAILED, RVF_RUNNING, RELEASE_COMPLETE
	}

	public static enum Tag {
		PUBLISHED
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

	public Tag getTag() {
		return tag;
	}

	public void setTag(Tag tag) {
		this.tag = tag;
	}

	public String getUniqueId() {
		return productBusinessKey + "|" + getId();
	}

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

	public QATestConfig getQaTestConfig() {
		return qaTestConfig;
	}

	public void setQaTestConfig(final QATestConfig assertionTestConfiguration) {
		this.qaTestConfig = assertionTestConfiguration;
	}

	public String getBuildUser() {
		return buildUser;
	}

	public void setBuildUser(String buildUser) {
		this.buildUser = buildUser;
	}

	public String getRvfURL() {
		return rvfURL;
	}

	public void setRvfURL(String rvfURL) {
		this.rvfURL = rvfURL;
	}
}

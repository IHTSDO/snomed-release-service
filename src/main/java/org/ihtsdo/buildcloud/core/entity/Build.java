package org.ihtsdo.buildcloud.core.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.ihtsdo.buildcloud.core.entity.helper.EntityHelper;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.ihtsdo.buildcloud.core.entity.Build.Status.PENDING;

/**
 * A Build is a snapshot of a Product and may be used to run the release process.
 *
 * This entity is stored via S3, not Hibernate.
 */
@JsonPropertyOrder({"id", "name"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Build {

	private String creationTime;

	private Status status;

	private List<Tag> tags;

	private String buildUser;

	private List<String> userRoles;

	private String rvfURL;

	private BuildConfiguration configuration;

	private String releaseCenterKey;

	private String productKey;

	private List<PreConditionCheckReport> preConditionCheckReports;

	private BuildReport buildReport;

	private QATestConfig qaTestConfig;

	public enum Status {
		PENDING,
		QUEUED,
		BEFORE_TRIGGER,
		FAILED_INPUT_GATHER_REPORT_VALIDATION,
		FAILED_INPUT_PREPARE_REPORT_VALIDATION,
		FAILED_PRE_CONDITIONS,
		BUILDING,
		BUILT,
		UNKNOWN,
		CANCEL_REQUESTED,
		CANCELLED,
		FAILED,
		INTERRUPTED,
		RVF_QUEUED,
		RVF_RUNNING,
		RVF_FAILED,
		RELEASE_COMPLETE,
		RELEASE_COMPLETE_WITH_WARNINGS,
		FAILED_POST_CONDITIONS;

		public static Status findBuildStatus(final String text) {
			return Arrays.stream(Status.values()).filter(status -> status.name().equalsIgnoreCase(text)).findFirst().orElse(null);
		}
	}

	public enum Tag {
		ALPHA(1), BETA(2), PRE_PROD(3), PUBLISHED(4), NOT_TO_BE_USED(5);

		private final int order;

		Tag (int order) {
			this.order = order;
		}

		public int getOrder() {
			return order;
		}
	}

	public Build() {

	}

	private Build(final String creationTime, final String releaseCenterKey, final String productKey, final BuildConfiguration configuration, final QATestConfig qaTestConfig, final String statusString) {
		this.buildReport = new BuildReport();
		this.releaseCenterKey = releaseCenterKey;
		this.productKey = productKey;
		this.creationTime = creationTime;
		this.configuration = configuration;
		this.qaTestConfig = qaTestConfig;
		try {
			this.status = Status.valueOf(statusString);
		} catch (final IllegalArgumentException e) {
			this.status = Status.UNKNOWN;
		}
	}

	public Build(final Date creationTime, final String releaseCenterKey, final String productKey, final BuildConfiguration configuration, final QATestConfig qaTestConfig) {
		this(EntityHelper.formatAsIsoDateTime(creationTime), releaseCenterKey, productKey, configuration, qaTestConfig, PENDING.name());
	}

	public Build(final String creationTime, final String releaseCenterKey, final String productKey, final String statusString) {
		this(creationTime, releaseCenterKey, productKey, null, null, statusString);
	}

	@JsonProperty(value = "id")
	public String getId() {
		return creationTime;
	}

	public String getCreationTime() {
		return creationTime;
	}

	public String getReleaseCenterKey() {
		return releaseCenterKey;
	}

	public String getProductKey() {
		return productKey;
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(final Status status) {
		this.status = status;
	}

	public List<Tag> getTags() {
		return tags;
	}

	public void setTags(List<Tag> tags) {
		this.tags = tags;
	}

	public String getUniqueId() {
		return productKey + "|" + getId();
	}

	public BuildConfiguration getConfiguration() {
		return configuration;
	}

	public String getBuildName() {
		return configuration != null ? configuration.getBuildName() : null;
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

	public List<String> getUserRoles() {
		return userRoles;
	}

	public void setUserRoles(List<String> userRoles) {
		this.userRoles = userRoles;
	}

	public String getRvfURL() {
		return rvfURL;
	}

	public void setRvfURL(String rvfURL) {
		this.rvfURL = rvfURL;
	}

	@Override
	public String toString() {
		return "Build{" +
				"creationTime='" + creationTime + '\'' +
				", status=" + status +
				", tags=" + tags +
				", buildUser='" + buildUser + '\'' +
				", userRoles='" + userRoles + '\'' +
				", rvfURL='" + rvfURL + '\'' +
				", configuration=" + configuration +
				", releaseCenterKey='" + releaseCenterKey + '\'' +
				", productKey='" + productKey + '\'' +
				", preConditionCheckReports=" + preConditionCheckReports +
				", buildReport=" + buildReport +
				", qaTestConfig=" + qaTestConfig +
				'}';
	}
}

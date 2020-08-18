package org.ihtsdo.buildcloud.entity;

import javax.persistence.*;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.hibernate.annotations.Type;

@Entity
@Table(name="qa_config")
public class QATestConfig {
	@Id
	@GeneratedValue
	@JsonIgnore
	@Column(name="id")
	private long id;
	
	@OneToOne
	@JoinColumn(name="product_id")
	@JsonIgnore
	private Product product;
	
	@Column(name="assertion_group_names")
	private String assertionGroupNames;

	@Column(name="drools_rules_group_names")
	private String droolsRulesGroupNames;
	
	@Column(name="previous_int_release")
	private String previousInternationalRelease;
	
	@Column(name="previous_ext_release")
	private String previousExtensionRelease;
	
	@Column(name="extension_dependency_release")
	private String extensionDependencyRelease;
	
	@Column(name = "storage_location")
	private String storageLocation;

	@Type(type="yes_no")
	@Column(name = "enable_drools")
	private boolean enableDrools = false;

	@Type(type="yes_no")
	@Column(name = "enable_google_sheet_drool_report")
	private boolean enableGoogleSheetDroolReport = false;

	@Type(type="yes_no")
	@Column(name = "create_jira_issue")
	private boolean jiraIssueCreationFlag = false;

	@Column(name = "product_name")
	private String productName;

	@Column(name = "reporting_stage")
	private String reportingStage;

	@Type(type = "yes_no")
	@Column(name = "enable_mrcm_validation")
	private boolean enableMRCMValidation;

	@Transient
	private Integer maxFailureExport;

	public String getAssertionGroupNames() {
		return assertionGroupNames;
	}
	public void setAssertionGroupNames(final String assertionGroupNames) {
		this.assertionGroupNames = assertionGroupNames;
	}

	public String getDroolsRulesGroupNames() {
		return droolsRulesGroupNames;
	}

	public void setDroolsRulesGroupNames(String droolsRulesGroupNames) {
		this.droolsRulesGroupNames = droolsRulesGroupNames;
	}

	public String getPreviousInternationalRelease() {
		return previousInternationalRelease;
	}
	public void setPreviousInternationalRelease(final String previousIntRelease) {
		this.previousInternationalRelease = previousIntRelease;
	}
	public Product getProduct() {
		return product;
	}
	public void setProduct(final Product product) {
		this.product = product;
	}
	public String getPreviousExtensionRelease() {
		return previousExtensionRelease;
	}
	public void setPreviousExtensionRelease(final String previousExtensionRelease) {
		this.previousExtensionRelease = previousExtensionRelease;
	}
	public String getExtensionDependencyRelease() {
		return extensionDependencyRelease;
	}
	public void setExtensionDependencyRelease(final String extDependencyRelease) {
		extensionDependencyRelease = extDependencyRelease;
	}

	public boolean isEnableDrools() {
		return enableDrools;
	}

	public void setEnableDrools(boolean enableDrools) {
		this.enableDrools = enableDrools;
	}

	public boolean isEnableGoogleSheetDroolReport() {
		return enableGoogleSheetDroolReport;
	}

	public void setEnableGoogleSheetDroolReport(boolean enableGoogleSheetDroolReport) {
		this.enableGoogleSheetDroolReport = enableGoogleSheetDroolReport;
	}

	public boolean isJiraIssueCreationFlag() {
		return jiraIssueCreationFlag;
	}

	public void setJiraIssueCreationFlag(boolean jiraIssueCreationFlag) {
		this.jiraIssueCreationFlag = jiraIssueCreationFlag;
	}

	public String getProductName() {
		return productName;
	}

	public void setProductName(String productName) {
		this.productName = productName;
	}

	public String getReportingStage() {
		return reportingStage;
	}

	public void setReportingStage(String reportingStage) {
		this.reportingStage = reportingStage;
	}

	@Override
	public String toString() {
		return "QATestConfig [id=" + id + ", assertionGroupNames="
				+ assertionGroupNames + ", droolsRulesGroupNames="
				+ droolsRulesGroupNames + ", previousInternationalRelease="
				+ previousInternationalRelease + ", previousExtensionRelease="
				+ previousExtensionRelease + ", extensionDependencyRelease="
				+ extensionDependencyRelease + ", storageLocation="
				+ storageLocation + ", enableDrools="
				+ enableDrools + ", jiraIssueCreationFlag="
				+ jiraIssueCreationFlag + ", product_name="
				+ productName + ", reporting_stage="
				+ reportingStage + ", enableMRCMValidation="
				+ enableMRCMValidation + ", enableGoogleSheetDroolReport="
				+ enableGoogleSheetDroolReport + "]";
	}

	public String getStorageLocation() {
		return storageLocation;
	}

	public void setStorageLocation(final String storageLocation) {
		this.storageLocation = storageLocation;
	}

	public boolean isEnableMRCMValidation() {
		return enableMRCMValidation;
	}

	public void setEnableMRCMValidation(boolean enableMRCMValidation) {
		this.enableMRCMValidation = enableMRCMValidation;
	}

	public Integer getMaxFailureExport() {
		return maxFailureExport;
	}

	public void setMaxFailureExport(Integer maxFailureExport) {
		this.maxFailureExport = maxFailureExport;
	}
}

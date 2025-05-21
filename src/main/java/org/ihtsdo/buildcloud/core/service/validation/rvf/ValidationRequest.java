package org.ihtsdo.buildcloud.core.service.validation.rvf;

public class ValidationRequest {
	
	private String buildBucketName;
	
	private String releaseZipFileS3Path;
	
	private String manifestFileS3Path;
	
	private Integer failureExportMax;
	
	private String effectiveTime;

	private String previousPublishedPackage;

	private String extensionDependencyRelease;

	private String previousExtensionDependencyEffectiveTime;
	
	private boolean releaseAsAnEdition;

	private boolean standAloneProduct;

	private String removeRF2Files;

	private boolean dailyBuild;

	private String defaultModuleId;

	private String includedModuleIds;

	private final String runId;

	private String responseQueue;

	private String branchPath;

	private String excludedRefsetDescriptorMembers;

	public ValidationRequest(String runId) {
		this.runId = runId;
	}

	public String getRunId() {
		return this.runId;
	}
	
	public String getBuildBucketName() {
		return buildBucketName;
	}

	public void setBuildBucketName(String buildBucketName) {
		this.buildBucketName = buildBucketName;
	}

	public String getReleaseZipFileS3Path() {
		return releaseZipFileS3Path;
	}

	public void setReleaseZipFileS3Path(String s3ZipFilePath) {
		this.releaseZipFileS3Path = s3ZipFilePath;
	}

	public String getManifestFileS3Path() {
		return manifestFileS3Path;
	}

	public void setManifestFileS3Path(String manifestFileS3Path) {
		this.manifestFileS3Path = manifestFileS3Path;
	}

	public Integer getFailureExportMax() {
		return failureExportMax;
	}

	public void setFailureExportMax(Integer failureExportMax) {
		this.failureExportMax = failureExportMax;
	}

	public String getEffectiveTime() {
		return effectiveTime;
	}

	public void setEffectiveTime(String effectiveTime) {
		this.effectiveTime = effectiveTime;
	}

	public String getPreviousPublishedPackage() {
		return previousPublishedPackage;
	}

	public void setPreviousPublishedPackage(String previousPublishedPackage) {
		this.previousPublishedPackage = previousPublishedPackage;
	}

	public String getExtensionDependencyRelease() {
		return extensionDependencyRelease;
	}

	public void setExtensionDependencyRelease(String extensionDependencyRelease) {
		this.extensionDependencyRelease = extensionDependencyRelease;
	}

	public String getPreviousExtensionDependencyEffectiveTime() {
		return previousExtensionDependencyEffectiveTime;
	}

	public void setPreviousExtensionDependencyEffectiveTime(String previousExtensionDependencyEffectiveTime) {
		this.previousExtensionDependencyEffectiveTime = previousExtensionDependencyEffectiveTime;
	}

	public boolean isReleaseAsAnEdition() {
		return releaseAsAnEdition;
	}

	public void setReleaseAsAnEdition(boolean releaseAsAnEdition) {
		this.releaseAsAnEdition = releaseAsAnEdition;
	}

	public boolean isStandAloneProduct() {
		return standAloneProduct;
	}

	public void setStandAloneProduct(boolean standAloneProduct) {
		this.standAloneProduct = standAloneProduct;
	}

	public String getRemoveRF2Files() {
		return removeRF2Files;
	}

	public void setRemoveRF2Files(String removeRF2Files) {
		this.removeRF2Files = removeRF2Files;
	}

	public String getDefaultModuleId() {
		return defaultModuleId;
	}

	public void setDefaultModuleId(String defaultModuleId) {
		this.defaultModuleId = defaultModuleId;
	}

	public String getIncludedModuleIds() {
		return includedModuleIds;
	}

	public void setIncludedModuleIds(String includedModuleIds) {
		this.includedModuleIds = includedModuleIds;
	}

	public final String getResponseQueue() {
		return responseQueue;
	}

	public final void setResponseQueue(final String responseQueue) {
		this.responseQueue = responseQueue;
	}

	public void setBranchPath(String branchPath) {
		this.branchPath = branchPath;
	}

	public String getBranchPath() {
		return branchPath;
	}

	public void setExcludedRefsetDescriptorMembers(String excludedRefsetDescriptorMembers) {
		this.excludedRefsetDescriptorMembers = excludedRefsetDescriptorMembers;
	}

	public String getExcludedRefsetDescriptorMembers() {
		return excludedRefsetDescriptorMembers;
	}

	public void setDailyBuild(boolean dailyBuild) {
		this.dailyBuild = dailyBuild;
	}

	public boolean isDailyBuild() {
		return dailyBuild;
	}
}

package org.ihtsdo.buildcloud.core.service.validation.rvf;

public class ValidationRequest {
	
	private String buildBucketName;
	
	private String releaseZipFileS3Path;
	
	private String manifestFileS3Path;
	
	private Integer failureExportMax;
	
	private String effectiveTime;

	private String previousDependencyEffectiveTime;
	
	private boolean releaseAsAnEdition; 
	
	private String includedModuleId;

	private final String runId;

	private String responseQueue;

	private String branchPath;

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

	public String getPreviousDependencyEffectiveTime() {
		return previousDependencyEffectiveTime;
	}

	public void setPreviousDependencyEffectiveTime(String previousDependencyEffectiveTime) {
		this.previousDependencyEffectiveTime = previousDependencyEffectiveTime;
	}

	public boolean isReleaseAsAnEdition() {
		return releaseAsAnEdition;
	}

	public void setReleaseAsAnEdition(boolean releaseAsAnEdition) {
		this.releaseAsAnEdition = releaseAsAnEdition;
	}

	public String getIncludedModuleId() {
		return includedModuleId;
	}

	public void setIncludedModuleId(String includedModuleId) {
		this.includedModuleId = includedModuleId;
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
}

package org.ihtsdo.buildcloud.service.rvf;

import org.ihtsdo.buildcloud.entity.QATestConfig.CharacteristicType;

public class ValidationRequest {
	
	private String buildBucketName;
	
	private String releaseZipFileS3Path;
	
	private String manifestFileS3Path;
	
	private Integer failureExportMax;
	
	private String effectiveTime;
	
	private boolean releaseAsAnEdition; 
	
	private String includedModuleId;

	private String runId;

	private CharacteristicType mrcmValidationForm;

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

	public CharacteristicType getMrcmValidationForm() {
		return mrcmValidationForm;
	}

	public void setMrcmValidationForm(CharacteristicType mrcmValidationForm) {
		this.mrcmValidationForm = mrcmValidationForm;
	}
}

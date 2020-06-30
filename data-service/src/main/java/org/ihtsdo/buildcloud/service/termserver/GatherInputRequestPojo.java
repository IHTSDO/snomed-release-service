package org.ihtsdo.buildcloud.service.termserver;


import org.ihtsdo.otf.rest.client.terminologyserver.SnowstormRestClient.ExportCategory;

import java.util.Set;

public class GatherInputRequestPojo {

	private String trackerId;
	private String termServerUrl;
	private String effectiveDate;
	private ExportCategory exportCategory;
	private String branchPath;
	private boolean loadTermServerData = true;
	private boolean loadExternalRefsetData = true;
	private Set<String> excludedModuleIds;
	private boolean useSnowOwl;
	private Integer maxFailuresExport;
	
	public GatherInputRequestPojo() {
	}

	public String getTermServerUrl() {
		return termServerUrl;
	}

	public void setTermServerUrl(String termServerUrl) {
		this.termServerUrl = termServerUrl;
	}

	public String getEffectiveDate() {
		return effectiveDate;
	}

	public void setEffectiveDate(String effectiveDate) {
		this.effectiveDate = effectiveDate;
	}

	public ExportCategory getExportCategory() {
		return exportCategory;
	}

	public void setExportCategory(ExportCategory exportCategory) {
		this.exportCategory = exportCategory;
	}

	public String getBranchPath() {
		return branchPath;
	}

	public void setBranchPath(String branchPath) {
		this.branchPath = branchPath;
	}

	public Set<String> getExcludedModuleIds() {
		return excludedModuleIds;
	}

	public void setExcludedModuleIds(Set<String> excludedModuleIds) {
		this.excludedModuleIds = excludedModuleIds;
	}

	public boolean isLoadTermServerData() {
		return loadTermServerData;
	}

	public void setLoadTermServerData(boolean loadTermServerData) {
		this.loadTermServerData = loadTermServerData;
	}

	public boolean isLoadExternalRefsetData() {
		return loadExternalRefsetData;
	}

	public void setLoadExternalRefsetData(boolean loadExternalRefsetData) {
		this.loadExternalRefsetData = loadExternalRefsetData;
	}

	public String getTrackerId() {
		return trackerId;
	}

	public void setTrackerId(String trackerId) {
		this.trackerId = trackerId;
	}

	public boolean getUseSnowOwl() {
		return useSnowOwl;
	}

	public void setUseSnowOwl(boolean useSnowOwl) {
		this.useSnowOwl = useSnowOwl;
	}

	public Integer getMaxFailuresExport() {
		return maxFailuresExport;
	}

	public void setMaxFailuresExport(Integer maxFailuresExport) {
		this.maxFailuresExport = maxFailuresExport;
	}

	@Override
	public String toString() {
		return "TermserverReleaseRequestPojo{" +
				", termServerUrl='" + termServerUrl + '\''+
				", effectiveDate='" + effectiveDate + '\'' +
				", exportCategory=" + exportCategory +
				", branchPath='" + branchPath + '\'' +
				", excludedModuleIds=" + excludedModuleIds +
				", loadTermServerData=" + loadTermServerData +
				", loadExternalRefsetData=" + loadExternalRefsetData +
				", useSnowOwl=" + useSnowOwl +
				", maxFailuresExport=" + maxFailuresExport +
				'}';
	}

}
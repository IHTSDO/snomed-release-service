package org.ihtsdo.buildcloud.service.termserver;

import org.ihtsdo.otf.rest.client.snowowl.SnowOwlRestClient;

import java.util.Set;

public class GatherInputRequestPojo {

	private String effectiveDate;
	private SnowOwlRestClient.ExportCategory exportCategory;
	private String branchPath;
	private Set<String> excludedModuleIds;
	private String namespaceId;
	private boolean includeUnpublished = true;
	private boolean loadTermServerData;
	private boolean loadExternalRefsetData;

	public GatherInputRequestPojo() {
	}

	public String getEffectiveDate() {
		return effectiveDate;
	}

	public void setEffectiveDate(String effectiveDate) {
		this.effectiveDate = effectiveDate;
	}

	public SnowOwlRestClient.ExportCategory getExportCategory() {
		return exportCategory;
	}

	public void setExportCategory(SnowOwlRestClient.ExportCategory exportCategory) {
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

	public String getNamespaceId() {
		return namespaceId;
	}

	public void setNamespaceId(String namespaceId) {
		this.namespaceId = namespaceId;
	}

	public boolean isIncludeUnpublished() {
		return includeUnpublished;
	}

	public void setIncludeUnpublished(boolean includeUnpublished) {
		this.includeUnpublished = includeUnpublished;
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

	@Override
	public String toString() {
		return "TermserverReleaseRequestPojo{" +
				", effectiveDate='" + effectiveDate + '\'' +
				", exportCategory=" + exportCategory +
				", branchPath='" + branchPath + '\'' +
				", excludedModuleIds=" + excludedModuleIds +
				", namespaceId=" + namespaceId +
				", includeUnpublished=" + includeUnpublished +
				", loadTermServerData=" + loadTermServerData +
				", loadExternalRefsetData=" + loadExternalRefsetData +
				'}';
	}
}
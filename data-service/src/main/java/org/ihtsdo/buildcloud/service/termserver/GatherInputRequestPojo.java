package org.ihtsdo.buildcloud.service.termserver;

import org.ihtsdo.otf.rest.client.terminologyserver.SnowstormRestClient;

import java.util.Set;

public class GatherInputRequestPojo {

	private String trackerId;
	private String termServerUrl;
	private String effectiveDate;
	private SnowstormRestClient.ExportCategory exportCategory;
	private String branchPath;
	private boolean loadTermServerData;
	private boolean loadExternalRefsetData;
	private Set<String> excludedModuleIds;
	/*private String namespaceId;
	private String startEffectiveDate;
	private String endEffectiveDate;
	private boolean includeUnpublished = false;
	private String codeSystemShortName;*/

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

	public SnowstormRestClient.ExportCategory getExportCategory() {
		return exportCategory;
	}

	public void setExportCategory(SnowstormRestClient.ExportCategory exportCategory) {
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

	/*public String getNamespaceId() {
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


	public String getStartEffectiveDate() {
		return startEffectiveDate;
	}

	public void setStartEffectiveDate(String startEffectiveDate) {
		this.startEffectiveDate = startEffectiveDate;
	}

	public String getEndEffectiveDate() {
		return endEffectiveDate;
	}

	public void setEndEffectiveDate(String endEffectiveDate) {
		this.endEffectiveDate = endEffectiveDate;
	}

	public String getCodeSystemShortName() {
		return codeSystemShortName;
	}

	public void setCodeSystemShortName(String codeSystemShortName) {
		this.codeSystemShortName = codeSystemShortName;
	}*/

	@Override
	public String toString() {
		return "TermserverReleaseRequestPojo{" +
				", termServerUrl='" + termServerUrl + '\''+
				", effectiveDate='" + effectiveDate + '\'' +
				/*", startEffectiveDate='" + startEffectiveDate + '\'' +
				", EndEffectiveDate='" + endEffectiveDate + '\'' +*/
				", exportCategory=" + exportCategory +
				", branchPath='" + branchPath + '\'' +
				", excludedModuleIds=" + excludedModuleIds +
				/*", namespaceId=" + namespaceId +
				", includeUnpublished=" + includeUnpublished +*/
				", loadTermServerData=" + loadTermServerData +
				", loadExternalRefsetData=" + loadExternalRefsetData +
				'}';
	}

}
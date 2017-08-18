package org.ihtsdo.buildcloud.service.termserver;

import org.ihtsdo.otf.rest.client.snowowl.SnowOwlRestClient;

import java.util.Set;

public class TermserverReleaseRequestPojo {

	private String effectiveDate;
	private SnowOwlRestClient.ExportCategory exportCategory;
	private String branchPath;
	private Set<String> excludedModuleIds;

	public TermserverReleaseRequestPojo() {
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

	@Override
	public String toString() {
		return "TermserverReleaseRequestPojo{" +
				", effectiveDate='" + effectiveDate + '\'' +
				", exportCategory=" + exportCategory +
				", branchPath='" + branchPath + '\'' +
				", excludedModuleIds=" + excludedModuleIds +
				'}';
	}
}
package org.ihtsdo.buildcloud.core.service;

import org.ihtsdo.buildcloud.core.entity.Build;
import org.ihtsdo.buildcloud.core.service.build.compare.BuildComparisonReport;
import org.ihtsdo.buildcloud.core.service.build.compare.FileDiffReport;

import java.util.List;

public interface AutomatedTestService {
	List<BuildComparisonReport> getAllTestReports();

	BuildComparisonReport getTestReport(String releaseCenterKey, String productKey, String compareId);

	void compareBuilds(String compareId, Build leftBuild, Build rightBuild, String username);

	void compareFiles(Build leftBuild, Build rightBuild, String fileName, String compareId, boolean ignoreIdComparison);

	FileDiffReport getFileDiffReport(String releaseCenterKey, String productKey, String compareId, String fileName, boolean ignoreIdComparison);
}

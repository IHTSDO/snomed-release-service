package org.ihtsdo.buildcloud.core.service;

import org.ihtsdo.buildcloud.core.entity.Build;
import org.ihtsdo.buildcloud.core.service.build.compare.BuildComparisonReport;
import org.ihtsdo.buildcloud.core.service.build.compare.FileDiffReport;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;

import java.util.List;

public interface AutomatedTestService {
	List<BuildComparisonReport> getAllTestReports();

	BuildComparisonReport getTestReport(String releaseCenterKey, String productKey, String compareId);

	void deleteTestReport(String releaseCenterKey, String productKey, String compareId) throws BusinessServiceException;

	void compareBuilds(String compareId, Build leftBuild, Build rightBuild, String username);

	void compareFiles(Build leftBuild, Build rightBuild, String fileName, String compareId, boolean ignoreIdComparison);

	FileDiffReport getFileDiffReport(String releaseCenterKey, String productKey, String compareId, String fileName, boolean ignoreIdComparison);
}

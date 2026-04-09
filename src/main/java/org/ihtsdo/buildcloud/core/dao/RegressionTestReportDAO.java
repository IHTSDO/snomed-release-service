package org.ihtsdo.buildcloud.core.dao;

import org.ihtsdo.buildcloud.core.service.build.compare.BuildComparisonReport;
import org.ihtsdo.buildcloud.core.service.build.compare.FileDiffReport;

import java.io.IOException;
import java.util.List;

public interface RegressionTestReportDAO {

	void saveBuildComparisonReport(String releaseCenterKey, String productKey, String compareId, BuildComparisonReport report) throws IOException;

	List<String> listBuildComparisonReportPaths(String releaseCenterKey, String productKey);

	BuildComparisonReport getBuildComparisonReport(String releaseCenterKey, String productKey, String compareId) throws IOException;

	void deleteBuildComparisonReport(String releaseCenterKey, String productKey, String compareId);

	void saveFileComparisonReport(String releaseCenterKey, String productKey, String compareId, boolean ignoreIdComparison, FileDiffReport report) throws IOException;

	FileDiffReport getFileComparisonReport(String releaseCenterKey, String productKey, String compareId, String fileName, boolean ignoreIdComparison) throws IOException;
}

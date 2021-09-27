package org.ihtsdo.buildcloud.core.service;

import com.github.difflib.text.DiffRow;
import org.ihtsdo.buildcloud.core.entity.Build;
import org.ihtsdo.buildcloud.core.service.build.compare.BuildComparisonReport;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;

import java.io.IOException;
import java.util.List;

public interface AutomatedTestService {
    List<BuildComparisonReport> getAllTestReports();

    BuildComparisonReport getTestReport(String releaseCenterKey, String productKey, String compareId);

	void compareBuilds(String compareId, Build leftBuild, Build rightBuild);

    List<DiffRow> findDiff(Build leftBuild, Build rightBuild, String fileName) throws BusinessServiceException;
}

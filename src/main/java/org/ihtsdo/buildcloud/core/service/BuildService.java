package org.ihtsdo.buildcloud.core.service;

import org.ihtsdo.buildcloud.core.entity.Build;
import org.ihtsdo.buildcloud.core.entity.BuildConfiguration;
import org.ihtsdo.buildcloud.core.entity.QATestConfig;
import org.ihtsdo.buildcloud.rest.pojo.BuildPage;
import org.ihtsdo.buildcloud.rest.pojo.BuildRequestPojo;
import org.ihtsdo.otf.rest.exception.BadConfigurationException;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.otf.rest.exception.ResourceNotFoundException;
import org.springframework.data.domain.PageRequest;

import java.io.InputStream;
import java.util.List;

public interface BuildService {

	String MDC_BUILD_KEY = "build";

	enum View {
		DEFAULT, UNPUBLISHED, PUBLISHED, ALL_RELEASES
	}

	Build createBuildFromProduct(String releaseCenterKey, String productKey, BuildRequestPojo buildRequest, String user) throws BusinessServiceException;

	/**
	 * Synchronous method which runs the build.
	 * @param build
	 * @return
	 * @throws BusinessServiceException
	 */
	Build triggerBuild(Build build, Boolean enableTelemetryStream);

	List<Build> findAllDesc(String releaseCenterKey, String productKey, Boolean includeBuildConfiguration, Boolean includeQAConfiguration, Boolean includeRvfURL, Boolean visibility) throws ResourceNotFoundException;

	BuildPage<Build> findAllDescPage(String releaseCenterKey, String productKey, Boolean includeBuildConfiguration, Boolean includeQAConfiguration, Boolean includeRvfURL, Boolean visibility, View viewMode, PageRequest pageRequest) throws ResourceNotFoundException;

	Build find(String releaseCenterKey, String productKey, String buildId, Boolean includeBuildConfiguration, Boolean includeQAConfiguration, Boolean includeRvfURL, Boolean visibility) throws ResourceNotFoundException;

	void markBuildAsDeleted(Build build);

	void delete(String releaseCenterKey, String productKey, String buildId) throws ResourceNotFoundException;

	BuildConfiguration loadBuildConfiguration(String releaseCenterKey, String productKey, String buildId) throws BusinessServiceException;
	
	QATestConfig loadQATestConfig(String releaseCenterKey, String productKey, String buildId) throws BusinessServiceException;

	InputStream getOutputFile(String releaseCenterKey, String productKey, String buildId, String outputFilePath) throws ResourceNotFoundException;

	List<String> getOutputFilePaths(String releaseCenterKey, String productKey, String buildId) throws BusinessServiceException;

	InputStream getInputFile(String releaseCenterKey, String productKey, String buildId, String inputFileName) throws ResourceNotFoundException;

	List<String> getInputFilePaths(String releaseCenterKey, String productKey, String buildId) throws ResourceNotFoundException;

	List<String> getLogFilePaths(String releaseCenterKey, String productKey, String buildId) throws ResourceNotFoundException;

	InputStream getLogFile(String releaseCenterKey, String productKey, String buildId, String logFileName) throws ResourceNotFoundException;

	InputStream getBuildReportFile(Build build) throws ResourceNotFoundException;

	InputStream getBuildReportFile(String releaseCenterKey, String productKey,String buildId) throws ResourceNotFoundException;

	InputStream getBuildInputFilesPrepareReport(String releaseCenterKey, String productKey, String buildId);

	void requestCancelBuild(String releaseCenterKey, String productKey, String buildId) throws ResourceNotFoundException, BadConfigurationException;

	InputStream getBuildInputGatherReport(String releaseCenterKey, String productKey, String buildId);

	InputStream getPreConditionChecksReport(String releaseCenterKey, String productKey, String buildId);

    InputStream getPostConditionChecksReport(String releaseCenterKey, String productKey, String buildId);

    List<String> getClassificationResultOutputFilePaths(String releaseCenterKey, String productKey, String buildId);

	InputStream getClassificationResultOutputFile(String releaseCenterKey, String productKey, String buildId, String inputFileName) throws ResourceNotFoundException;

	void updateVisibility(String releaseCenterKey, String productKey, String buildId, boolean visibility);

	void updateVisibility(Build build, boolean visibility);

	void saveTags(Build build, List<Build.Tag> tags);

	Build cloneBuild(String releaseCenterKey, String productKey, String buildId, String username) throws BusinessServiceException;

	String getManifestFileName(String releaseCenterKey, String productKey, String buildId) throws ResourceNotFoundException;

	InputStream getManifestStream(String releaseCenterKey, String productKey, String buildId) throws ResourceNotFoundException;
}

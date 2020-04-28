package org.ihtsdo.buildcloud.service;

import java.io.InputStream;
import java.util.List;

import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.BuildConfiguration;
import org.ihtsdo.buildcloud.entity.QATestConfig;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.otf.rest.exception.ResourceNotFoundException;

public interface BuildService {

	String MDC_BUILD_KEY = "build";

	Build createBuildFromProduct(String releaseCenterKey, String productKey) throws BusinessServiceException;

	/**
	 * Synchronous method which runs the build.
	 * @param releaseCenterKey
	 * @param productKey
	 * @param buildId
	 * @param failureExportMax 
	 * @return
	 * @throws BusinessServiceException
	 */
	Build triggerBuild(String releaseCenterKey, String productKey, String buildId, Integer failureExportMax) throws BusinessServiceException;

	List<Build> findAllDesc(String releaseCenterKey, String productKey) throws ResourceNotFoundException;

	Build find(String releaseCenterKey, String productKey, String buildId) throws ResourceNotFoundException;

	BuildConfiguration loadBuildConfiguration(String releaseCenterKey, String productKey, String buildId) throws BusinessServiceException;
	
	QATestConfig loadQATestConfig(String releaseCenterKey, String productKey, String buildId) throws BusinessServiceException;

	InputStream getOutputFile(String releaseCenterKey, String productKey, String buildId, String outputFilePath) throws ResourceNotFoundException;

	List<String> getOutputFilePaths(String releaseCenterKey, String productKey, String buildId) throws BusinessServiceException;

	InputStream getInputFile(String releaseCenterKey, String productKey, String buildId, String inputFileName) throws ResourceNotFoundException;

	List<String> getInputFilePaths(String releaseCenterKey, String productKey, String buildId) throws ResourceNotFoundException;

	List<String> getLogFilePaths(String releaseCenterKey, String productKey, String buildId) throws ResourceNotFoundException;

	InputStream getLogFile(String releaseCenterKey, String productKey, String buildId, String logFileName) throws ResourceNotFoundException;

	InputStream getBuildReportFile(String releaseCenterKey, String productKey,String buildId) throws ResourceNotFoundException;

	InputStream getBuildInputFilesPrepareReport(String releaseCenterKey, String productKey, String buildId);

	InputStream getPreConditionChecksReport(String releaseCenterKey, String productKey, String buildId);

    InputStream getPostConditionChecksReport(String releaseCenterKey, String productKey, String buildId);
}

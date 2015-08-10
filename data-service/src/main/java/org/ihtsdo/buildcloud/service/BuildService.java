package org.ihtsdo.buildcloud.service;

import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.BuildConfiguration;
import org.ihtsdo.buildcloud.entity.QATestConfig;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.otf.rest.exception.ResourceNotFoundException;

import java.io.InputStream;
import java.util.List;

public interface BuildService {

	String MDC_BUILD_KEY = "build";

	Build createBuildFromProduct(String releaseCenterKey, String productKey) throws BusinessServiceException;

	/**
	 * Synchronous method which runs the build.
	 * @param releaseCenterKey
	 * @param productKey
	 * @param buildId
	 * @return
	 * @throws BusinessServiceException
	 */
	Build triggerBuild(String releaseCenterKey, String productKey, String buildId) throws BusinessServiceException;

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

}

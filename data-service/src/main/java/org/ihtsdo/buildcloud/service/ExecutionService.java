package org.ihtsdo.buildcloud.service;

import org.ihtsdo.buildcloud.entity.Execution;
import org.ihtsdo.buildcloud.service.exception.BusinessServiceException;
import org.ihtsdo.buildcloud.service.exception.ResourceNotFoundException;

import java.io.InputStream;
import java.util.List;

public interface ExecutionService {

	String MDC_EXECUTION_KEY = "execution";

	Execution createExecutionFromBuild(String releaseCenterKey, String buildKey) throws BusinessServiceException;

	Execution triggerExecution(String releaseCenterKey, String buildKey, String executionId) throws BusinessServiceException;

	List<Execution> findAllDesc(String releaseCenterKey, String buildKey) throws ResourceNotFoundException;

	Execution find(String releaseCenterKey, String buildKey, String executionId) throws ResourceNotFoundException;

	String loadConfiguration(String releaseCenterKey, String buildKey, String executionId) throws BusinessServiceException;

	InputStream getOutputFile(String releaseCenterKey, String buildKey, String executionId, String outputFilePath) throws ResourceNotFoundException;

	List<String> getOutputFilePaths(String releaseCenterKey, String buildKey, String executionId) throws BusinessServiceException;

	InputStream getInputFile(String releaseCenterKey, String buildKey, String executionId, String inputFileName) throws ResourceNotFoundException;

	List<String> getInputFilePaths(String releaseCenterKey, String buildKey, String executionId) throws ResourceNotFoundException;

	List<String> getLogFilePaths(String releaseCenterKey, String buildKey, String executionId) throws ResourceNotFoundException;

	InputStream getLogFile(String releaseCenterKey, String buildKey, String executionId, String logFileName) throws ResourceNotFoundException;

}

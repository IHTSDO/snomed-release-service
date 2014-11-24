package org.ihtsdo.buildcloud.service;

import org.ihtsdo.buildcloud.entity.Execution;
import org.ihtsdo.buildcloud.service.exception.BusinessServiceException;
import org.ihtsdo.buildcloud.service.exception.ResourceNotFoundException;

import java.io.InputStream;
import java.util.List;

public interface ExecutionService {

	String MDC_EXECUTION_KEY = "execution";

	Execution createExecutionFromProduct(String releaseCenterKey, String productKey) throws BusinessServiceException;

	Execution triggerExecution(String releaseCenterKey, String productKey, String executionId) throws BusinessServiceException;

	List<Execution> findAllDesc(String releaseCenterKey, String productKey) throws ResourceNotFoundException;

	Execution find(String releaseCenterKey, String productKey, String executionId) throws ResourceNotFoundException;

	String loadConfiguration(String releaseCenterKey, String productKey, String executionId) throws BusinessServiceException;

	InputStream getOutputFile(String releaseCenterKey, String productKey, String executionId, String outputFilePath) throws ResourceNotFoundException;

	List<String> getOutputFilePaths(String releaseCenterKey, String productKey, String executionId) throws BusinessServiceException;

	InputStream getInputFile(String releaseCenterKey, String productKey, String executionId, String inputFileName) throws ResourceNotFoundException;

	List<String> getInputFilePaths(String releaseCenterKey, String productKey, String executionId) throws ResourceNotFoundException;

	List<String> getLogFilePaths(String releaseCenterKey, String productKey, String executionId) throws ResourceNotFoundException;

	InputStream getLogFile(String releaseCenterKey, String productKey, String executionId, String logFileName) throws ResourceNotFoundException;

}

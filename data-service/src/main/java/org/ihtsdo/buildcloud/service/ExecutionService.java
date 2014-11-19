package org.ihtsdo.buildcloud.service;

import org.ihtsdo.buildcloud.dto.ExecutionPackageDTO;
import org.ihtsdo.buildcloud.entity.Execution;
import org.ihtsdo.buildcloud.service.exception.BusinessServiceException;
import org.ihtsdo.buildcloud.service.exception.ResourceNotFoundException;

import java.io.InputStream;
import java.util.List;

public interface ExecutionService {

	String MDC_EXECUTION_KEY = "execution";

	Execution createExecutionFromBuild(String buildCompositeKey) throws BusinessServiceException;

	List<Execution> findAllDesc(String buildCompositeKey) throws ResourceNotFoundException;

	Execution find(String buildCompositeKey, String executionId) throws ResourceNotFoundException;

	String loadConfiguration(String buildCompositeKey, String executionId) throws BusinessServiceException;

	List<ExecutionPackageDTO> getExecutionPackages(String buildCompositeKey, String executionId) throws BusinessServiceException;

	ExecutionPackageDTO getExecutionPackage(String buildCompositeKey, String executionId, String packageId) throws BusinessServiceException;

	Execution triggerBuild(String buildCompositeKey, String executionId) throws BusinessServiceException;

	void updateStatus(String buildCompositeKey, String executionId, String status) throws BusinessServiceException;

	InputStream getOutputFile(String buildCompositeKey, String executionId, String packageId, String outputFilePath) throws ResourceNotFoundException;

	List<String> getExecutionPackageOutputFilePaths(String buildCompositeKey, String executionId, String packageId) throws BusinessServiceException;

	InputStream getInputFile(String buildCompositeKey, String executionId, String packageId, String inputFileName) throws ResourceNotFoundException;

	List<String> getExecutionPackageInputFilePaths(String buildCompositeKey, String executionId, String packageId) throws ResourceNotFoundException;

	InputStream getLogFile(String buildCompositeKey, String executionId, String packageId, String logFileName) throws ResourceNotFoundException;

	List<String> getExecutionPackageLogFilePaths(String buildCompositeKey, String executionId, String packageId) throws ResourceNotFoundException;

	List<String> getExecutionLogFilePaths(String buildCompositeKey, String executionId) throws ResourceNotFoundException;

	InputStream getExecutionLogFile(String buildCompositeKey, String executionId, String logFileName) throws ResourceNotFoundException;
}

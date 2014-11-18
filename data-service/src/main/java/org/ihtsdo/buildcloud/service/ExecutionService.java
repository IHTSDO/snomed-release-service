package org.ihtsdo.buildcloud.service;

import org.ihtsdo.buildcloud.dto.ExecutionPackageDTO;
import org.ihtsdo.buildcloud.entity.Execution;
import org.ihtsdo.buildcloud.entity.User;
import org.ihtsdo.buildcloud.service.exception.BusinessServiceException;
import org.ihtsdo.buildcloud.service.exception.ResourceNotFoundException;

import java.io.InputStream;
import java.util.List;

public interface ExecutionService {

	String MDC_EXECUTION_KEY = "execution";

	Execution createExecutionFromBuild(String buildCompositeKey, User authenticatedUser) throws BusinessServiceException;

	List<Execution> findAllDesc(String buildCompositeKey, User authenticatedUser) throws ResourceNotFoundException;

	Execution find(String buildCompositeKey, String executionId, User authenticatedUser) throws ResourceNotFoundException;

	String loadConfiguration(String buildCompositeKey, String executionId, User authenticatedUser) throws BusinessServiceException;

	List<ExecutionPackageDTO> getExecutionPackages(String buildCompositeKey, String executionId, User authenticatedUser) throws BusinessServiceException;

	ExecutionPackageDTO getExecutionPackage(String buildCompositeKey, String executionId, String packageId, User authenticatedUser) throws BusinessServiceException;

	Execution triggerBuild(String buildCompositeKey, String executionId, User authenticatedUser) throws BusinessServiceException;

	void updateStatus(String buildCompositeKey, String executionId, String status, User authenticatedUser) throws BusinessServiceException;

	InputStream getOutputFile(String buildCompositeKey, String executionId, String packageId, String outputFilePath, User authenticatedUser) throws ResourceNotFoundException;

	List<String> getExecutionPackageOutputFilePaths(String buildCompositeKey, String executionId, String packageId, User authenticatedUser) throws BusinessServiceException;

	InputStream getInputFile(String buildCompositeKey, String executionId, String packageId, String inputFileName, User authenticatedUser) throws ResourceNotFoundException;

	List<String> getExecutionPackageInputFilePaths(String buildCompositeKey, String executionId, String packageId, User authenticatedUser) throws ResourceNotFoundException;

	InputStream getLogFile(String buildCompositeKey, String executionId, String packageId, String logFileName, User authenticatedUser) throws ResourceNotFoundException;

	List<String> getExecutionPackageLogFilePaths(String buildCompositeKey, String executionId, String packageId, User authenticatedUser) throws ResourceNotFoundException;

	List<String> getExecutionLogFilePaths(String buildCompositeKey, String executionId, User authenticatedUser) throws ResourceNotFoundException;

	InputStream getExecutionLogFile(String buildCompositeKey, String executionId, String logFileName, User authenticatedUser) throws ResourceNotFoundException;
}

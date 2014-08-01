package org.ihtsdo.buildcloud.service;

import org.ihtsdo.buildcloud.dto.ExecutionPackageDTO;
import org.ihtsdo.buildcloud.entity.Execution;
import org.ihtsdo.buildcloud.entity.User;
import org.ihtsdo.buildcloud.service.exception.BadConfigurationException;
import org.ihtsdo.buildcloud.service.exception.NamingConflictException;
import org.ihtsdo.buildcloud.service.exception.ResourceNotFoundException;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

public interface ExecutionService {

	/**
	 * Create snapshot of build files and configuration for review and possibly using to run a build.
	 * @param buildCompositeKey
	 * @param authenticatedUser
	 * @return
	 * @throws IOException
	 * @throws NamingConflictException 
	 * @throws ResourceNotFoundException 
	 */
	Execution create(String buildCompositeKey, User authenticatedUser) throws IOException, BadConfigurationException, NamingConflictException, ResourceNotFoundException;

	List<Execution> findAllDesc(String buildCompositeKey, User authenticatedUser) throws ResourceNotFoundException;

	Execution find(String buildCompositeKey, String executionId, User authenticatedUser) throws ResourceNotFoundException;

	String loadConfiguration(String buildCompositeKey, String executionId, User authenticatedUser) throws IOException, ResourceNotFoundException;

	List<ExecutionPackageDTO> getExecutionPackages(String buildCompositeKey, String executionId, User authenticatedUser) throws IOException, ResourceNotFoundException;

	ExecutionPackageDTO getExecutionPackage(String buildCompositeKey, String executionId, String packageId, User authenticatedUser) throws IOException, ResourceNotFoundException;

	Map<String, Object> triggerBuild(String buildCompositeKey, String executionId, User authenticatedUser) throws IOException, Exception;

	void updateStatus(String buildCompositeKey, String executionId, String status, User authenticatedUser) throws ResourceNotFoundException;

	InputStream getOutputFile(String buildCompositeKey, String executionId, String packageId, String outputFilePath, User authenticatedUser) throws ResourceNotFoundException;

	List<String> getExecutionPackageOutputFilePaths(String buildCompositeKey, String executionId, String packageId, User authenticatedUser) throws IOException, ResourceNotFoundException;

	InputStream getLogFile(String buildCompositeKey, String executionId, String packageId, String logFileName, User authenticatedUser) throws ResourceNotFoundException;

	List<String> getExecutionPackageLogFilePaths(String buildCompositeKey, String executionId, String packageId, User authenticatedUser) throws IOException, ResourceNotFoundException;
}

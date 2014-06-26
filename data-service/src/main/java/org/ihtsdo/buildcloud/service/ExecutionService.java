package org.ihtsdo.buildcloud.service;

import org.ihtsdo.buildcloud.dto.ExecutionPackageDTO;
import org.ihtsdo.buildcloud.entity.Execution;
import org.ihtsdo.buildcloud.entity.User;
import org.ihtsdo.buildcloud.service.exception.BadConfigurationException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public interface ExecutionService {

	/**
	 * Create snapshot of build files and configuration for review and possibly using to run a build.
	 * @param buildCompositeKey
	 * @param authenticatedUser
	 * @return
	 * @throws IOException
	 */
	Execution create(String buildCompositeKey, User authenticatedUser) throws IOException, BadConfigurationException;

	List<Execution> findAll(String buildCompositeKey, User authenticatedUser);

	Execution find(String buildCompositeKey, String executionId, User authenticatedUser);

	String loadConfiguration(String buildCompositeKey, String executionId, User authenticatedUser) throws IOException;

	List<ExecutionPackageDTO> getExecutionPackages(String buildCompositeKey, String executionId, User authenticatedUser) throws IOException;

	ExecutionPackageDTO getExecutionPackage(String buildCompositeKey, String executionId, String packageId, User authenticatedUser) throws IOException;

	Execution triggerBuild(String buildCompositeKey, String executionId, User authenticatedUser) throws IOException, Exception;

	void streamBuildScriptsZip(String buildCompositeKey, String executionId, User authenticatedUser, OutputStream outputStream) throws IOException;

	void putOutputFile(String buildCompositeKey, String executionId, String filePath, InputStream inputStream, Long size, User authenticatedUser);

	void updateStatus(String buildCompositeKey, String executionId, String status, User authenticatedUser);

	InputStream getOutputFile(String buildCompositeKey, String executionId, String filePath, User authenticatedUser);

}

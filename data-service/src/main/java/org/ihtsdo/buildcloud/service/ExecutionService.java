package org.ihtsdo.buildcloud.service;

import org.ihtsdo.buildcloud.entity.Execution;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public interface ExecutionService {

	Execution create(String buildCompositeKey, String authenticatedId) throws IOException;

	List<Execution> findAll(String buildCompositeKey, String authenticatedId);

	Execution find(String buildCompositeKey, String executionId, String authenticatedId);

	String loadConfiguration(String buildCompositeKey, String executionId, String authenticatedId) throws IOException;

	Execution triggerBuild(String buildCompositeKey, String executionId, String authenticatedId) throws IOException;

	void streamBuildScriptsZip(String buildCompositeKey, String executionId, String authenticatedId, OutputStream outputStream) throws IOException;

	void saveOutputFile(String buildCompositeKey, String executionId, String filePath, InputStream inputStream, Long size, String authenticatedId);

}

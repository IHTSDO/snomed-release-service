package org.ihtsdo.buildcloud.service;

import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.Execution;
import org.ihtsdo.buildcloud.entity.User;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public interface FileService {

	void putManifestFile(String buildCompositeKey, String packageBusinessKey, InputStream inputStream, String originalFilename, long fileSize, User subject);

	String getManifestFileName(String buildCompositeKey, String packageBusinessKey, User authenticatedUser);

	InputStream getManifestStream(String buildCompositeKey, String packageBusinessKey, User authenticatedUser);

	void putFile(String buildCompositeKey, String packageBusinessKey, InputStream inputStream, String filename, long fileSize, User authenticatedUser);

	InputStream getFileStream(String buildCompositeKey, String packageBusinessKey, String filename, User authenticatedUser);

	List<String> listFilePaths(String buildCompositeKey, String packageBusinessKey, User authenticatedUser);

	void copyAll(Build buildSource, Execution executionTarget);

	void deleteFile(String buildCompositeKey, String packageBusinessKey, String filename, User authenticatedUser);

	List<String> listInputFilePaths(Execution execution, String packageId);

	InputStream getExecutionInputFileStream(Execution execution, String packageBusinessKey, String relativeFilePath);

	OutputStream getExecutionOutputFileOutputStream(Execution execution, String packageBusinessKey, String relativeFilePath) throws IOException;

	OutputStream getExecutionOutputFileOutputStream(String executionOutputFilePath) throws IOException;

	void copyInputFileToOutputFile(Execution execution, String packageBusinessKey, String relativeFilePath);
}

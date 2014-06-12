package org.ihtsdo.buildcloud.service;

import org.apache.commons.codec.DecoderException;
import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.Execution;
import org.ihtsdo.buildcloud.entity.User;
import org.ihtsdo.buildcloud.entity.Package;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;
import java.util.List;

public interface FileService {

	void putManifestFile(String buildCompositeKey, String packageBusinessKey, InputStream inputStream, String originalFilename, long fileSize, User subject);

	String getManifestFileName(String buildCompositeKey, String packageBusinessKey, User authenticatedUser);

	InputStream getManifestStream(String buildCompositeKey, String packageBusinessKey, User authenticatedUser);

	InputStream getManifestStream(Package pkg);

	void putInputFile(String buildCompositeKey, String packageBusinessKey, InputStream inputStream, String filename, long fileSize, User authenticatedUser);

	String putOutputFile(Execution execution, Package pkg, File file, boolean calcMD5) throws FileNotFoundException, IOException, NoSuchAlgorithmException, DecoderException;

	InputStream getFileInputStream(String buildCompositeKey, String packageBusinessKey, String filename, User authenticatedUser);

	InputStream getFileInputStream(Package pkg, String filename);

	List<String> listFilePaths(String buildCompositeKey, String packageBusinessKey, User authenticatedUser);

	void copyAll(Build buildSource, Execution executionTarget);

	void deleteFile(String buildCompositeKey, String packageBusinessKey, String filename, User authenticatedUser);

	List<String> listInputFilePaths(Execution execution, String packageId);

	InputStream getExecutionInputFileStream(Execution execution, String packageBusinessKey, String relativeFilePath);

	OutputStream getExecutionOutputFileOutputStream(Execution execution, String packageBusinessKey, String relativeFilePath) throws IOException;

	OutputStream getExecutionFileOutputStream(String executionOutputFilePath) throws IOException;

	void copyInputFileToOutputFile(Execution execution, String packageBusinessKey, String relativeFilePath);

	List<String> listTransformedFilePaths(Execution execution, String packageId);
	
	List<String> listOutputFilePaths(Execution execution, String packageId);

	OutputStream getExecutionTransformedFileOutputStream(Execution execution, String packageBusinessKey, String relativeFilePath) throws IOException;

	void copyTransformedFileToOutput(Execution execution, String packageBusinessKey, String sourceFileName, String targetFileName );
	
	void copyTransformedFileToOutput(Execution execution, String packageBusinessKey, String relativeFilePath);
	
	InputStream getTransformedFileAsInputStream(Execution execution, String businessKey, String relativeFilePath);
}

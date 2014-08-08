package org.ihtsdo.buildcloud.service;

import org.ihtsdo.buildcloud.entity.User;
import org.ihtsdo.buildcloud.service.exception.ResourceNotFoundException;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public interface InputFileService {

	void putManifestFile(String buildCompositeKey, String packageBusinessKey, InputStream inputStream, String originalFilename, long fileSize, User subject) throws ResourceNotFoundException;

	String getManifestFileName(String buildCompositeKey, String packageBusinessKey, User authenticatedUser) throws ResourceNotFoundException;

	InputStream getManifestStream(String buildCompositeKey, String packageBusinessKey, User authenticatedUser) throws ResourceNotFoundException;

	void putInputFile(String buildCompositeKey, String packageBusinessKey, InputStream inputStream, String filename, long fileSize, User authenticatedUser) throws ResourceNotFoundException, IOException;

	InputStream getFileInputStream(String buildCompositeKey, String packageBusinessKey, String filename, User authenticatedUser) throws ResourceNotFoundException;

	List<String> listInputFilePaths(String buildCompositeKey, String packageBusinessKey, User authenticatedUser) throws ResourceNotFoundException;

	void deleteFile(String buildCompositeKey, String packageBusinessKey, String filename, User authenticatedUser) throws ResourceNotFoundException;

	void deleteFilesByPattern(String buildCompositeKey,
			String packageBusinessKey, String inputFileNamePattern,
			User authenticatedUser) throws ResourceNotFoundException;

}

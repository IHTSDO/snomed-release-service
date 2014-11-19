package org.ihtsdo.buildcloud.service;

import org.ihtsdo.buildcloud.service.exception.ResourceNotFoundException;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public interface InputFileService {

	void putManifestFile(String buildCompositeKey, String packageBusinessKey, InputStream inputStream, String originalFilename, long fileSize) throws ResourceNotFoundException;

	String getManifestFileName(String buildCompositeKey, String packageBusinessKey) throws ResourceNotFoundException;

	InputStream getManifestStream(String buildCompositeKey, String packageBusinessKey) throws ResourceNotFoundException;

	void putInputFile(String buildCompositeKey, String packageBusinessKey, InputStream inputStream, String filename, long fileSize) throws ResourceNotFoundException, IOException;

	InputStream getFileInputStream(String buildCompositeKey, String packageBusinessKey, String filename) throws ResourceNotFoundException;

	List<String> listInputFilePaths(String buildCompositeKey, String packageBusinessKey) throws ResourceNotFoundException;

	void deleteFile(String buildCompositeKey, String packageBusinessKey, String filename) throws ResourceNotFoundException;

	void deleteFilesByPattern(String buildCompositeKey,
			String packageBusinessKey, String inputFileNamePattern) throws ResourceNotFoundException;

}

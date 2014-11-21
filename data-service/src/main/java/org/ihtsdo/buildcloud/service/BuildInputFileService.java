package org.ihtsdo.buildcloud.service;

import org.ihtsdo.buildcloud.service.exception.ResourceNotFoundException;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public interface BuildInputFileService {

	void putManifestFile(String centerKey, String buildKey, InputStream inputStream, String originalFilename, long fileSize) throws ResourceNotFoundException;

	String getManifestFileName(String centerKey, String buildKey) throws ResourceNotFoundException;

	InputStream getManifestStream(String centerKey, String buildKey) throws ResourceNotFoundException;

	void putInputFile(String centerKey, String buildKey, InputStream inputStream, String filename, long fileSize) throws ResourceNotFoundException, IOException;

	InputStream getFileInputStream(String centerKey, String buildKey, String filename) throws ResourceNotFoundException;

	List<String> listInputFilePaths(String centerKey, String buildKey) throws ResourceNotFoundException;

	void deleteFile(String centerKey, String buildKey, String filename) throws ResourceNotFoundException;

	void deleteFilesByPattern(String centerKey, String buildKey, String inputFileNamePattern) throws ResourceNotFoundException;

}

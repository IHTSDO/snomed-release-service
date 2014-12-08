package org.ihtsdo.buildcloud.service;

import org.ihtsdo.buildcloud.service.exception.ResourceNotFoundException;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public interface ProductInputFileService {

	void putManifestFile(String centerKey, String productKey, InputStream inputStream, String originalFilename, long fileSize) throws ResourceNotFoundException;

	String getManifestFileName(String centerKey, String productKey) throws ResourceNotFoundException;

	InputStream getManifestStream(String centerKey, String productKey) throws ResourceNotFoundException;

	void putInputFile(String centerKey, String productKey, InputStream inputStream, String filename, long fileSize) throws ResourceNotFoundException, IOException;

	InputStream getFileInputStream(String centerKey, String productKey, String filename) throws ResourceNotFoundException;

	List<String> listInputFilePaths(String centerKey, String productKey) throws ResourceNotFoundException;

	void deleteFile(String centerKey, String productKey, String filename) throws ResourceNotFoundException;

	void deleteFilesByPattern(String centerKey, String productKey, String inputFileNamePattern) throws ResourceNotFoundException;

}

package org.ihtsdo.buildcloud.service;

import org.apache.commons.codec.DecoderException;
import org.ihtsdo.buildcloud.service.fileprocessing.FileProcessingReport;
import org.ihtsdo.otf.rest.exception.ResourceNotFoundException;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Set;

public interface ProductInputFileService {

	void putManifestFile(String centerKey, String productKey, InputStream inputStream, String originalFilename, long fileSize) throws ResourceNotFoundException;

	String getManifestFileName(String centerKey, String productKey) throws ResourceNotFoundException;

	InputStream getManifestStream(String centerKey, String productKey) throws ResourceNotFoundException;

	void putInputFile(String centerKey, String productKey, InputStream inputStream, String filename, long fileSize) throws ResourceNotFoundException, IOException;

	InputStream getFileInputStream(String centerKey, String productKey, String filename) throws ResourceNotFoundException;

	List<String> listInputFilePaths(String centerKey, String productKey) throws ResourceNotFoundException;

	void deleteFile(String centerKey, String productKey, String filename) throws ResourceNotFoundException;

	void deleteFilesByPattern(String centerKey, String productKey, String inputFileNamePattern) throws ResourceNotFoundException;

	void putSourceFile(String sourceName, String centerKey, String productKey, InputStream inputStream, String filename, long fileSize) throws ResourceNotFoundException, IOException;

	List<String> listSourceFilePaths(String centerKey, String productKey) throws ResourceNotFoundException;

	List<String> listSourceFilePathsFromSubDirectories(String centerKey, String productKey, Set<String> subDirectories) throws ResourceNotFoundException;

	List<String> listSourceFilePathsFromSubDirectory(String centerKey, String productKey, String subDirectory) throws ResourceNotFoundException;

	void deleteSourceFile(String centerKey, String productKey, String fileName, String subDirectory) throws ResourceNotFoundException;

	void deleteSourceFilesByPattern(String centerKey, String productKey, String inputFileNamePattern, Set<String> subDirectories) throws ResourceNotFoundException;

	FileProcessingReport prepareInputFiles(String centerKey, String productKey, boolean copyFilesInManifest) throws ResourceNotFoundException, IOException, JAXBException, DecoderException, NoSuchAlgorithmException;

	InputStream getInputPrepareReport(String centerKey, String productKey) throws ResourceNotFoundException;

}

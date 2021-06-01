package org.ihtsdo.buildcloud.core.service;

import org.ihtsdo.buildcloud.core.entity.Product;
import org.ihtsdo.buildcloud.core.service.inputfile.gather.GatherInputRequestPojo;
import org.ihtsdo.buildcloud.core.service.inputfile.gather.InputGatherReport;
import org.ihtsdo.buildcloud.core.service.inputfile.prepare.SourceFileProcessingReport;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.otf.rest.exception.ResourceNotFoundException;
import org.springframework.security.core.context.SecurityContext;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;

public interface ProductInputFileService {

	void putManifestFile(String centerKey, String productKey, InputStream inputStream, String originalFilename, long fileSize) throws ResourceNotFoundException;

	String getManifestFileName(String centerKey, String productKey) throws ResourceNotFoundException;

	InputStream getManifestStream(String centerKey, String productKey) throws ResourceNotFoundException;

	void putInputFile(String centerKey, String productKey, InputStream inputStream, String filename, long fileSize) throws ResourceNotFoundException, IOException;

	void putInputFile(String centerKey, Product product, InputStream inputStream, String filename, long fileSize) throws IOException;

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

	SourceFileProcessingReport prepareInputFiles(String centerKey, String productKey, boolean copyFilesInManifest) throws BusinessServiceException;

	InputStream getInputPrepareReport(String centerKey, String productKey) throws ResourceNotFoundException;

	InputGatherReport gatherSourceFiles(String centerKey, String productKey, GatherInputRequestPojo requestConfig, SecurityContext securityContext) throws BusinessServiceException, IOException;

	InputStream getInputGatherReport(String centerKey, String productKey);

	void gatherSourceFilesFromExternallyMaintainedBucket(String centerKey, String productKey, String effectiveDate
			, InputGatherReport inputGatherReport) throws IOException;

	InputStream getSourceFileStream(String releaseCenterKey, String productKey, String source, String sourceFileName);

	InputStream getFullBuildLogFromProductIfExists(String releaseCenterKey, String productKey);

}

package org.ihtsdo.buildcloud.core.service;

import org.ihtsdo.buildcloud.core.entity.Build;
import org.ihtsdo.buildcloud.core.entity.Product;
import org.ihtsdo.buildcloud.core.service.inputfile.gather.InputGatherReport;
import org.ihtsdo.buildcloud.core.service.inputfile.prepare.SourceFileProcessingReport;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.otf.rest.exception.ResourceNotFoundException;
import org.springframework.security.core.context.SecurityContext;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;

public interface InputFileService {

	void putManifestFile(String centerKey, String productKey, InputStream inputStream, String originalFilename, long fileSize) throws ResourceNotFoundException;

	String getManifestFileName(String centerKey, String productKey) throws ResourceNotFoundException;

	InputStream getManifestStream(String centerKey, String productKey) throws ResourceNotFoundException;

	void putInputFile(String centerKey, Product product, String buildId, InputStream inputStream, String filename, long fileSize) throws IOException;

	void putSourceFile(String sourceName, String centerKey, String productKey, String buildId, InputStream inputStream, String filename, long fileSize) throws ResourceNotFoundException, IOException;

	List<String> listSourceFilePaths(String centerKey, String productKey, String buildId) throws ResourceNotFoundException;

	List<String> listSourceFilePathsFromSubDirectories(String centerKey, String productKey, Set<String> subDirectories, String buildId) throws ResourceNotFoundException;

	SourceFileProcessingReport prepareInputFiles(String centerKey, String productKey, Build build, boolean copyFilesInManifest) throws BusinessServiceException;

	InputGatherReport gatherSourceFiles(String centerKey, String productKey, Build build, SecurityContext securityContext) throws BusinessServiceException, IOException;

	InputStream getSourceFileStream(String releaseCenterKey, String productKey, String source, String sourceFileName);

}

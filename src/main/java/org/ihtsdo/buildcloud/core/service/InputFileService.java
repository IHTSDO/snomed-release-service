package org.ihtsdo.buildcloud.core.service;

import org.apache.commons.codec.DecoderException;
import org.ihtsdo.buildcloud.core.entity.Build;
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

	void putManifestFile(String centerKey, String productKey, InputStream inputStream, String originalFilename, long fileSize) throws ResourceNotFoundException, IOException, DecoderException;

	String getManifestFileName(String centerKey, String productKey) throws ResourceNotFoundException;

	InputStream getManifestStream(String centerKey, String productKey) throws ResourceNotFoundException;

	void putInputFile(String centerKey, String productKey, String buildId, InputStream inputStream, String filename, long fileSize) throws IOException, DecoderException;

	void putSourceFile(String sourceName, String centerKey, String productKey, String buildId, InputStream inputStream, String filename, long fileSize) throws ResourceNotFoundException, IOException, DecoderException;

	List<String> listSourceFilePaths(String centerKey, String productKey, String buildId) throws ResourceNotFoundException;

	List<String> listSourceFilePathsFromSubDirectories(String centerKey, String productKey, String buildId, Set<String> subDirectories) throws ResourceNotFoundException;

	SourceFileProcessingReport prepareInputFiles(Build build, boolean copyFilesInManifest) throws BusinessServiceException;

	InputGatherReport gatherSourceFiles(Build build, SecurityContext securityContext) throws BusinessServiceException, IOException;

	InputStream getSourceFileStream(String centerKey, String productKey, String buildId, String source, String sourceFileName);
}

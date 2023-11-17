package org.ihtsdo.buildcloud.core.dao;

import org.apache.commons.codec.DecoderException;
import org.ihtsdo.buildcloud.core.service.inputfile.gather.InputGatherReport;
import org.ihtsdo.buildcloud.core.service.inputfile.prepare.SourceFileProcessingReport;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;

public interface InputFileDAO {

	InputStream getManifestStream(String releaseCenterKey, String productKey);

	InputStream getManifestStream(String releaseCenterKey, String productKey, String buildId);

	String getManifestPath(String releaseCenterKey, String productKey);

	String getManifestPath(String releaseCenterKey, String productKey, String buildId);

	void putManifestFile(String releaseCenterKey, String productKey, InputStream inputStream, String originalFilename, long fileSize) throws IOException, DecoderException;

	void putManifestFile(String releaseCenterKey, String productKey, String buildId, InputStream inputStream, String originalFilename, long fileSize) throws IOException, DecoderException;

	void deleteManifest(String releaseCenterKey, String productKey);

	String getKnownManifestPath(String releaseCenterKey, String productKey, String filename);

	List<String> listRelativeSourceFilePaths(String releaseCenterKey, String productKey, String buildId);

	List<String> listRelativeSourceFilePaths(String releaseCenterKey, String productKey, String buildId, Set<String> subDirectories);

	void persistInputPrepareReport(String releaseCenterKey, String productKey, String buildId, SourceFileProcessingReport fileProcessingReport) throws IOException;

	void persistSourcesGatherReport(String releaseCenterKey, String productKey, String buildId, InputGatherReport inputGatherReport) throws IOException;

}

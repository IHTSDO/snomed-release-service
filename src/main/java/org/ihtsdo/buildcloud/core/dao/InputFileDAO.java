package org.ihtsdo.buildcloud.core.dao;

import org.ihtsdo.buildcloud.core.entity.Product;
import org.ihtsdo.buildcloud.core.service.inputfile.gather.InputGatherReport;
import org.ihtsdo.buildcloud.core.service.inputfile.prepare.SourceFileProcessingReport;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;

public interface InputFileDAO {

	InputStream getManifestStream(Product product);

	InputStream getManifestStream(final Product product, String buildId);

	String getManifestPath(Product product);

	void putManifestFile(Product product, InputStream inputStream,
			String originalFilename, long fileSize);

	void putManifestFile(Product product, String buildId, InputStream inputStream,
						 String originalFilename, long fileSize);

	void deleteManifest(Product product);

	String getKnownManifestPath(Product product, String filename);

	List<String> listRelativeSourceFilePaths(Product product, String buildId);

	List<String> listRelativeSourceFilePaths(Product product, String buildId, Set<String> subDirectories);

	List<String> listRelativeSourceFilePaths(Product product, String buildId, String subDirectory);

	void persistInputPrepareReport(Product product, String buildId, SourceFileProcessingReport fileProcessingReport) throws IOException;

	void persistSourcesGatherReport(Product product, String buildId, InputGatherReport inputGatherReport) throws IOException;

	InputStream getInputGatherReport(Product product, String buildId);

}

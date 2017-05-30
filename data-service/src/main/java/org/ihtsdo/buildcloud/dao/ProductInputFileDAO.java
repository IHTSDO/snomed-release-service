package org.ihtsdo.buildcloud.dao;

import org.ihtsdo.buildcloud.entity.Product;
import org.ihtsdo.buildcloud.service.fileprocessing.FileProcessingReport;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;

public interface ProductInputFileDAO {

	InputStream getManifestStream(Product product);

	public List<String> listRelativeInputFilePaths(Product product);

	String getManifestPath(Product product);

	void putManifestFile(Product product, InputStream inputStream,
			String originalFilename, long fileSize);

	void deleteManifest(Product product);

	String getKnownManifestPath(Product product, String filename);

	List<String> listRelativeSourceFilePaths(Product product);

	List<String> listRelativeSourceFilePaths(Product product, Set<String> subDirectories);

	List<String> listRelativeSourceFilePaths(Product product, String subDirectory);

	void persistInputPrepareReport(Product product, FileProcessingReport fileProcessingReport) throws IOException;

	InputStream getInputPrepareReport(Product product);

}

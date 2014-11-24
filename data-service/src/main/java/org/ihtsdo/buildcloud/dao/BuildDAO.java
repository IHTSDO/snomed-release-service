package org.ihtsdo.buildcloud.dao;

import org.ihtsdo.buildcloud.dao.io.AsyncPipedStreamBean;
import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.Product;
import org.ihtsdo.buildcloud.entity.ReleaseCenter;
import org.ihtsdo.buildcloud.service.exception.BadConfigurationException;

import java.io.*;
import java.util.List;
import java.util.Map;

public interface BuildDAO {

	void save(Build build, String jsonConfig);

	List<Build> findAllDesc(Product product);

	Build find(Product product, String buildId);

	String loadConfiguration(Build build) throws IOException;

	Map<String, Object> loadConfigurationMap(Build build) throws IOException;

	void updateStatus(Build build, Build.Status newStatus);

	void assertStatus(Build build, Build.Status ensureStatus) throws BadConfigurationException;

	InputStream getOutputFileStream(Build build, String filePath);

	List<String> listInputFileNames(Build build);

	InputStream getInputFileStream(Build build, String relativeFilePath);

	InputStream getLocalInputFileStream(Build build, String relativeFilePath) throws FileNotFoundException;

	AsyncPipedStreamBean getOutputFileOutputStream(Build build, String relativeFilePath) throws IOException;

	AsyncPipedStreamBean getLogFileOutputStream(Build build, String relativeFilePath) throws IOException;

	void copyInputFileToOutputFile(Build build, String relativeFilePath);

	void copyAll(Product productSource, Build build);

	InputStream getOutputFileInputStream(Build build, String name);

	String putOutputFile(Build build, File file, boolean calcMD5)
			throws IOException;

	String putOutputFile(Build build, File file)
			throws IOException;

	void putTransformedFile(Build build, File file) throws IOException;

	InputStream getManifestStream(Build build);

	List<String> listTransformedFilePaths(Build build);

	List<String> listOutputFilePaths(Build build);

	List<String> listLogFilePaths(Build build);

	List<String> listBuildLogFilePaths(Build build);

	InputStream getLogFileStream(Build build, String logFileName);

	InputStream getBuildLogFileStream(Build build, String logFileName);

	String getTelemetryBuildLogFilePath(Build build);

	AsyncPipedStreamBean getTransformedFileOutputStream(Build build, String relativeFilePath) throws IOException;

	OutputStream getLocalTransformedFileOutputStream(Build build, String relativeFilePath) throws FileNotFoundException;

	InputStream getTransformedFileAsInputStream(Build build, String relativeFilePath);

	public InputStream getPublishedFileArchiveEntry(ReleaseCenter releaseCenter, String targetFileName, String previousPublishedPackage) throws IOException;

	void persistReport(Build build);
	
	void renameTransformedFile(Build build, String sourceFileName, String targetFileName);

}

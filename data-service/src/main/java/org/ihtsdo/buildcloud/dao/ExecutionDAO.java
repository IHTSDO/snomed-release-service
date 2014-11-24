package org.ihtsdo.buildcloud.dao;

import org.ihtsdo.buildcloud.dao.io.AsyncPipedStreamBean;
import org.ihtsdo.buildcloud.entity.Product;
import org.ihtsdo.buildcloud.entity.Execution;
import org.ihtsdo.buildcloud.entity.ReleaseCenter;
import org.ihtsdo.buildcloud.service.exception.BadConfigurationException;

import java.io.*;
import java.util.List;
import java.util.Map;

public interface ExecutionDAO {

	void save(Execution execution, String jsonConfig);

	List<Execution> findAllDesc(Product product);

	Execution find(Product product, String executionId);

	String loadConfiguration(Execution execution) throws IOException;

	Map<String, Object> loadConfigurationMap(Execution execution) throws IOException;

	void updateStatus(Execution execution, Execution.Status newStatus);

	void assertStatus(Execution execution, Execution.Status ensureStatus) throws BadConfigurationException;

	InputStream getOutputFileStream(Execution execution, String filePath);

	List<String> listInputFileNames(Execution execution);

	InputStream getInputFileStream(Execution execution, String relativeFilePath);

	InputStream getLocalInputFileStream(Execution execution, String relativeFilePath) throws FileNotFoundException;

	AsyncPipedStreamBean getOutputFileOutputStream(Execution execution, String relativeFilePath) throws IOException;

	AsyncPipedStreamBean getLogFileOutputStream(Execution execution, String relativeFilePath) throws IOException;

	void copyInputFileToOutputFile(Execution execution, String relativeFilePath);

	void copyAll(Product productSource, Execution execution);

	InputStream getOutputFileInputStream(Execution execution, String name);

	String putOutputFile(Execution execution, File file, boolean calcMD5)
			throws IOException;

	String putOutputFile(Execution execution, File file)
			throws IOException;

	void putTransformedFile(Execution execution, File file) throws IOException;

	InputStream getManifestStream(Execution execution);

	List<String> listTransformedFilePaths(Execution execution);

	List<String> listOutputFilePaths(Execution execution);

	List<String> listLogFilePaths(Execution execution);

	List<String> listExecutionLogFilePaths(Execution execution);

	InputStream getLogFileStream(Execution execution, String logFileName);

	InputStream getExecutionLogFileStream(Execution execution, String logFileName);

	String getTelemetryExecutionLogFilePath(Execution execution);

	AsyncPipedStreamBean getTransformedFileOutputStream(Execution execution, String relativeFilePath) throws IOException;

	OutputStream getLocalTransformedFileOutputStream(Execution execution, String relativeFilePath) throws FileNotFoundException;

	InputStream getTransformedFileAsInputStream(Execution execution, String relativeFilePath);

	public InputStream getPublishedFileArchiveEntry(ReleaseCenter releaseCenter, String targetFileName, String previousPublishedPackage) throws IOException;

	void persistReport(Execution execution);
	
	void renameTransformedFile(Execution execution, String sourceFileName, String targetFileName);

}

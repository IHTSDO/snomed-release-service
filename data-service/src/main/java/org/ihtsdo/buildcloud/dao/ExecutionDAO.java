package org.ihtsdo.buildcloud.dao;

import org.ihtsdo.buildcloud.dao.io.AsyncPipedStreamBean;
import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.Execution;
import org.ihtsdo.buildcloud.entity.Package;
import org.ihtsdo.buildcloud.entity.Product;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public interface ExecutionDAO {

	void save(Execution execution, String jsonConfig);

	ArrayList<Execution> findAllDesc(Build build);

	Execution find(Build build, String executionId);

	String loadConfiguration(Execution execution) throws IOException;

	Map<String, Object> loadConfigurationMap(Execution execution) throws IOException;

	void updateStatus(Execution execution, Execution.Status newStatus);

	void assertStatus(Execution execution, Execution.Status ensureStatus) throws Exception;

	InputStream getOutputFileStream(Execution execution, String packageId, String filePath);

	List<String> listInputFileNames(Execution execution, String packageId);

	InputStream getInputFileStream(Execution execution, String packageBusinessKey, String relativeFilePath);

	InputStream getLocalInputFileStream(Execution execution, String packageBusinessKey, String relativeFilePath) throws FileNotFoundException;

	AsyncPipedStreamBean getOutputFileOutputStream(Execution execution, String packageBusinessKey, String relativeFilePath) throws IOException;

	AsyncPipedStreamBean getLogFileOutputStream(Execution execution, String packageBusinessKey, String relativeFilePath) throws IOException;

	void copyInputFileToOutputFile(Execution execution, String packageBusinessKey, String relativeFilePath);

	void copyAll(Build buildSource, Execution execution);

	InputStream getOutputFileInputStream(Execution execution, String packageBusinessKey, String name);

	InputStream getOutputFileInputStream(Execution execution, Package pkg, String name);

	String putOutputFile(Execution execution, Package aPackage, File file, String targetRelativePath, boolean calcMD5)
			throws IOException;

	String putOutputFile(Execution execution, Package aPackage, File file)
			throws IOException;

	void putTransformedFile(Execution execution, Package pkg, File file) throws IOException;

	InputStream getManifestStream(Execution execution, Package pkg);

	List<String> listTransformedFilePaths(Execution execution, String packageId);

	List<String> listOutputFilePaths(Execution execution, String packageId);

	List<String> listLogFilePaths(Execution execution, String packageId);

	List<String> listExecutionLogFilePaths(Execution execution);

	InputStream getLogFileStream(Execution execution, String packageId, String logFileName);

	InputStream getExecutionLogFileStream(Execution execution, String logFileName);

	String getTelemetryExecutionLogFilePath(Execution execution);

	AsyncPipedStreamBean getTransformedFileOutputStream(Execution execution, String packageBusinessKey, String relativeFilePath) throws IOException;

	OutputStream getLocalTransformedFileOutputStream(Execution execution, String packageBusinessKey, String relativeFilePath) throws FileNotFoundException;

	void copyTransformedFileToOutput(Execution execution, String packageBusinessKey, String sourceFileName, String targetFileName);

	void copyTransformedFileToOutput(Execution execution, String packageBusinessKey, String relativeFilePath);

	InputStream getTransformedFileAsInputStream(Execution execution, String businessKey, String relativeFilePath);

	public InputStream getPublishedFileArchiveEntry(Product product, String targetFileName, String previousPublishedPackage) throws IOException;

	void persistReport(Execution execution);

}

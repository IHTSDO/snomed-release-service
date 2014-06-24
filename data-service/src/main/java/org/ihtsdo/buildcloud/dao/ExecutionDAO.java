package org.ihtsdo.buildcloud.dao;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.DecoderException;
import org.ihtsdo.buildcloud.dao.io.AsyncPipedStreamBean;
import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.Execution;
import org.ihtsdo.buildcloud.entity.Package;
import org.ihtsdo.buildcloud.entity.Product;
import org.ihtsdo.buildcloud.service.file.ArchiveEntry;

public interface ExecutionDAO {

	void save(Execution execution, String jsonConfig);

	ArrayList<Execution> findAll(Build build);

	Execution find(Build build, String executionId);

	String loadConfiguration(Execution execution) throws IOException;

	Map<String,Object> loadConfigurationMap(Execution execution) throws IOException;

	void saveBuildScripts(File buildScriptsTmpDirectory, Execution execution);

	void streamBuildScriptsZip(Execution execution, OutputStream outputStream) throws IOException;

	void queueForBuilding(Execution execution);

	void putOutputFile(Execution execution, String filePath, InputStream inputStream, Long size);

	void updateStatus(Execution execution, Execution.Status newStatus);
	
	void assertStatus(Execution execution, Execution.Status ensureStatus) throws Exception;

	InputStream getOutputFile(Execution execution, String filePath);
	
	List<String> listInputFilePaths(Execution execution, String packageId);

	InputStream getInputFileStream(Execution execution, String packageBusinessKey, String relativeFilePath);

	AsyncPipedStreamBean getOutputFileOutputStream(Execution execution, String packageBusinessKey, String relativeFilePath) throws IOException;

	AsyncPipedStreamBean getFileAsOutputStream(String executionOutputFilePath) throws IOException;

	void copyInputFileToOutputFile(Execution execution, String packageBusinessKey, String relativeFilePath);
	
	void copyAll(Build buildSource, Execution execution);

	InputStream getOutputFileInputStream(Execution execution, Package pkg, String name);

	String putOutputFile(Execution execution, Package aPackage, File file, String targetRelativePath, boolean calcMD5)
			throws NoSuchAlgorithmException, IOException, DecoderException;

	InputStream getManifestStream(Execution execution, Package pkg); 
	
	List<String> listTransformedFilePaths(Execution execution, String packageId);
	
	List<String> listOutputFilePaths(Execution execution, String packageId);

	AsyncPipedStreamBean getTransformedFileOutputStream(Execution execution, String packageBusinessKey, String relativeFilePath) throws IOException;

	void copyTransformedFileToOutput(Execution execution, String packageBusinessKey, String sourceFileName, String targetFileName );
	
	void copyTransformedFileToOutput(Execution execution, String packageBusinessKey, String relativeFilePath);
	
	InputStream getTransformedFileAsInputStream(Execution execution, String businessKey, String relativeFilePath);

	public ArchiveEntry getPublishedFile(Product product, String targetFileName, String previousPublishedPackage) throws IOException;

}

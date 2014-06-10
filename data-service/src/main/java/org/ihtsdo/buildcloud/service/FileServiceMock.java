package org.ihtsdo.buildcloud.service;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.Execution;
import org.ihtsdo.buildcloud.entity.Package;
import org.ihtsdo.buildcloud.entity.User;
import org.ihtsdo.buildcloud.entity.helper.EntityHelper;

import com.google.common.io.Files;

/**
 * Mock file service will attempt to return any file requested by looking for a local resource.
 * @author Peter
 *
 */
public class FileServiceMock implements FileService {
	
	private String manifestName;
	private String testFilesRelativePath;
	private Map<String, File> localFileCache;
	private String localCacheDirectory;
	
	public FileServiceMock (String manifestName, String testFilesRelativePath) {
		this.manifestName = manifestName;
		this.testFilesRelativePath = testFilesRelativePath;
		localFileCache = new HashMap<String, File>();
		localCacheDirectory = Files.createTempDir().getAbsolutePath();
	}

	@Override
	public void putManifestFile(String buildCompositeKey,
			String packageBusinessKey, InputStream inputStream,
			String originalFilename, long fileSize, User subject) {
		// TODO Auto-generated method stub

	}

	@Override
	public String getManifestFileName(String buildCompositeKey,
			String packageBusinessKey, User authenticatedUser) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public InputStream getManifestStream(String buildCompositeKey,
			String packageBusinessKey, User authenticatedUser) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public InputStream getManifestStream(Package pkg) {
		String lookingIn = this.getClass().getResource("/").getFile();
		return getClass().getResourceAsStream(testFilesRelativePath + manifestName);
	}

	@Override
	public void putInputFile(String buildCompositeKey, String packageBusinessKey,
			InputStream inputStream, String filename, long fileSize,
			User authenticatedUser) {
		// TODO Auto-generated method stub

	}

	@Override
	public InputStream getFileInputStream(String buildCompositeKey,
			String packageBusinessKey, String filename, User authenticatedUser) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public InputStream getFileInputStream(Package pkg, String filename) {
		return getClass().getResourceAsStream(testFilesRelativePath + filename);
	}

	@Override
	public List<String> listFilePaths(String buildCompositeKey,
			String packageBusinessKey, User authenticatedUser) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void copyAll(Build buildSource, Execution executionTarget) {
		// TODO Auto-generated method stub

	}

	@Override
	public void deleteFile(String buildCompositeKey, String packageBusinessKey,
			String filename, User authenticatedUser) {
		// TODO Auto-generated method stub

	}

	@Override
	public List<String> listInputFilePaths(Execution execution, String packageId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public InputStream getExecutionInputFileStream(Execution execution,
			String packageBusinessKey, String relativeFilePath) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OutputStream getExecutionOutputFileOutputStream(Execution execution,
			String packageBusinessKey, String relativeFilePath)
			throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OutputStream getExecutionOutputFileOutputStream(
			String executionOutputFilePath) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void copyInputFileToOutputFile(Execution execution,
			String packageBusinessKey, String relativeFilePath) {
		// TODO Auto-generated method stub

	}

	@Override
	public String putOutputFile(Execution execution, Package pkg, File file, boolean calcMD5) throws IOException {

		String destination = localCacheDirectory + File.separator + EntityHelper.formatAsBusinessKey(pkg.getName()) + File.separator + file.getName();
		File destFile = new File (destination);
		localFileCache.put(file.getName(), destFile);
		FileUtils.copyFile(file, destFile);
		return null;
	}

}

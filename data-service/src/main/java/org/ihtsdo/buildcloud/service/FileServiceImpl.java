package org.ihtsdo.buildcloud.service;

import org.ihtsdo.buildcloud.dao.FileDAO;
import org.ihtsdo.buildcloud.dao.PackageDAO;
import org.ihtsdo.buildcloud.dao.helper.ExecutionS3PathHelper;
import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.Execution;
import org.ihtsdo.buildcloud.entity.Package;
import org.ihtsdo.buildcloud.entity.User;
import org.ihtsdo.buildcloud.service.helper.CompositeKeyHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@Transactional
public class FileServiceImpl implements FileService {

	@Autowired
	private FileDAO fileDAO;

	@Autowired
	private PackageDAO packageDAO;

	@Autowired
	private ExecutionS3PathHelper s3PathHelper;

	private final ExecutorService executorService;

	private static final Logger LOGGER = LoggerFactory.getLogger(FileServiceImpl.class);

	public FileServiceImpl() {
		executorService = Executors.newCachedThreadPool();
	}

	@Override
	public void putManifestFile(String buildCompositeKey, String packageBusinessKey, InputStream inputStream, String originalFilename, long fileSize, User authenticatedUser) {
		Package aPackage = packageDAO.find(CompositeKeyHelper.getId(buildCompositeKey), packageBusinessKey, authenticatedUser);
		StringBuffer manifestDirectoryPathSB = s3PathHelper.getPackageManifestDirectoryPathPath(aPackage);

		// Fist delete any existing manifest files
		String directoryPath = manifestDirectoryPathSB.toString();
		List<String> files = fileDAO.listFiles(directoryPath);
		for (String file : files) {
			fileDAO.deleteFile(directoryPath + file);
		}

		// Put new manifest file
		String filePath = manifestDirectoryPathSB.append(originalFilename).toString();
		fileDAO.putFile(inputStream, fileSize, filePath);
	}

	@Override
	public String getManifestFileName(String buildCompositeKey, String packageBusinessKey, User authenticatedUser) {
		Package aPackage = packageDAO.find(CompositeKeyHelper.getId(buildCompositeKey), packageBusinessKey, authenticatedUser);
		StringBuffer manifestDirectoryPathSB = s3PathHelper.getPackageManifestDirectoryPathPath(aPackage);
		List<String> files = fileDAO.listFiles(manifestDirectoryPathSB.toString());
		if (!files.isEmpty()) {
			return files.iterator().next();
		} else {
			return null;
		}
	}

	@Override
	public InputStream getManifestStream(String buildCompositeKey, String packageBusinessKey, User authenticatedUser) {
		Package aPackage = packageDAO.find(CompositeKeyHelper.getId(buildCompositeKey), packageBusinessKey, authenticatedUser);
		StringBuffer manifestDirectoryPathSB = s3PathHelper.getPackageManifestDirectoryPathPath(aPackage);

		String directoryPath = manifestDirectoryPathSB.toString();
		List<String> files = fileDAO.listFiles(directoryPath);
		if (!files.isEmpty()) {
			String manifestFilePath = directoryPath + files.iterator().next();
			return fileDAO.getFileStream(manifestFilePath);
		} else {
			return null;
		}
	}

	@Override
	public void putFile(String buildCompositeKey, String packageBusinessKey, InputStream inputStream, String filename, long fileSize, User authenticatedUser) {
		Package aPackage = packageDAO.find(CompositeKeyHelper.getId(buildCompositeKey), packageBusinessKey, authenticatedUser);
		String pathPath = s3PathHelper.getPackageInputFilePath(aPackage, filename);
		fileDAO.putFile(inputStream, fileSize, pathPath);
	}

	@Override
	public InputStream getFileStream(String buildCompositeKey, String packageBusinessKey, String filename, User authenticatedUser) {
		Package aPackage = packageDAO.find(CompositeKeyHelper.getId(buildCompositeKey), packageBusinessKey, authenticatedUser);
		String filePath = s3PathHelper.getPackageInputFilePath(aPackage, filename);
		return fileDAO.getFileStream(filePath);
	}

	@Override
	public List<String> listFilePaths(String buildCompositeKey, String packageBusinessKey, User authenticatedUser) {
		Package aPackage = packageDAO.find(CompositeKeyHelper.getId(buildCompositeKey), packageBusinessKey, authenticatedUser);
		return listFilePaths(aPackage);
	}

	private List<String> listFilePaths(Package aPackage) {
		String directoryPath = s3PathHelper.getPackageInputFilesPath(aPackage);
		return fileDAO.listFiles(directoryPath);
	}

	@Override
	public void deleteFile(String buildCompositeKey, String packageBusinessKey, String filename, User authenticatedUser) {
		Package aPackage = packageDAO.find(CompositeKeyHelper.getId(buildCompositeKey), packageBusinessKey, authenticatedUser);
		String filePath = s3PathHelper.getPackageInputFilePath(aPackage, filename);
		fileDAO.deleteFile(filePath);
	}

	@Override
	public void copyAll(Build buildSource, Execution execution) {
		for (Package buildPackage : buildSource.getPackages()) {
			String buildPackageInputFilesPath = s3PathHelper.getPackageInputFilesPath(buildPackage);
			String executionPackageInputFilesPath = s3PathHelper.getExecutionInputFilesPath(execution, buildPackage).toString();
			List<String> filePaths = listFilePaths(buildPackage);
			for (String filePath : filePaths) {
				fileDAO.copyFile(buildPackageInputFilesPath + filePath, executionPackageInputFilesPath + filePath);
			}
		}
	}

	@Override
	public List<String> listInputFilePaths(Execution execution, String packageId) {
		String executionInputFilesPath = s3PathHelper.getExecutionInputFilesPath(execution, packageId).toString();
		return fileDAO.listFiles(executionInputFilesPath);
	}

	@Override
	public InputStream getExecutionInputFileStream(Execution execution, String packageBusinessKey, String inputFile) {
		String path = s3PathHelper.getExecutionInputFilePath(execution, packageBusinessKey, inputFile);
		return fileDAO.getFileStream(path);
	}

	@Override
	public OutputStream getExecutionOutputFileOutputStream(Execution execution, String packageBusinessKey, String relativeFilePath) throws IOException {
		String executionOutputFilePath = s3PathHelper.getExecutionOutputFilePath(execution, packageBusinessKey, relativeFilePath);
		return getExecutionOutputFileOutputStream(executionOutputFilePath);
	}

	@Override
	public void copyInputFileToOutputFile(Execution execution, String packageBusinessKey, String relativeFilePath) {
		String executionInputFilePath = s3PathHelper.getExecutionInputFilePath(execution, packageBusinessKey, relativeFilePath);
		String executionOutputFilePath = s3PathHelper.getExecutionOutputFilePath(execution, packageBusinessKey, relativeFilePath);
		fileDAO.copyFile(executionInputFilePath, executionOutputFilePath);
	}

	@Override
	public OutputStream getExecutionOutputFileOutputStream(final String executionOutputFilePath) throws IOException {
		// Stream file to fileDAO as it's written to the OutputStream
		final PipedInputStream pipedInputStream = new PipedInputStream();
		final PipedOutputStream pipedOutputStream = new PipedOutputStream(pipedInputStream);

		executorService.submit(new Callable<String>() {
			@Override
			public String call() throws Exception {
				fileDAO.putFile(pipedInputStream, executionOutputFilePath);
				LOGGER.debug("Execution outputfile stream ended: {}", executionOutputFilePath);
				return executionOutputFilePath;
			}
		});

		return pipedOutputStream;
	}

}

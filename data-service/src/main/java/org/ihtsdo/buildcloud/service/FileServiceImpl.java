package org.ihtsdo.buildcloud.service;

import org.apache.commons.codec.DecoderException;
import org.ihtsdo.buildcloud.dao.PackageDAO;
import org.ihtsdo.buildcloud.dao.helper.ExecutionS3PathHelper;
import org.ihtsdo.buildcloud.dao.helper.FileHelper;
import org.ihtsdo.buildcloud.dao.helper.S3ClientHelper;
import org.ihtsdo.buildcloud.dao.s3.S3Client;
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
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@Transactional
public class FileServiceImpl implements FileService {

	private FileHelper fileHelper;

	@Autowired
	private PackageDAO packageDAO;

	@Autowired
	private ExecutionS3PathHelper s3PathHelper;
	
	private final ExecutorService executorService;

	private static final Logger LOGGER = LoggerFactory.getLogger(FileServiceImpl.class);

	@Autowired
	public FileServiceImpl(String executionBucketName, S3Client s3Client, S3ClientHelper s3ClientHelper) {
		executorService = Executors.newCachedThreadPool();
		fileHelper = new FileHelper (executionBucketName, s3Client, s3ClientHelper);
	}

	@Override
	public void putManifestFile(String buildCompositeKey, String packageBusinessKey, InputStream inputStream, String originalFilename, long fileSize, User authenticatedUser) {
		Package aPackage = packageDAO.find(CompositeKeyHelper.getId(buildCompositeKey), packageBusinessKey, authenticatedUser);
		StringBuffer manifestDirectoryPathSB = s3PathHelper.getPackageManifestDirectoryPathPath(aPackage);

		// Fist delete any existing manifest files
		String directoryPath = manifestDirectoryPathSB.toString();
		List<String> files = fileHelper.listFiles(directoryPath);
		for (String file : files) {
			fileHelper.deleteFile(directoryPath + file);
		}

		// Put new manifest file
		String filePath = manifestDirectoryPathSB.append(originalFilename).toString();
		fileHelper.putFile(inputStream, fileSize, filePath);
	}

	@Override
	public String getManifestFileName(String buildCompositeKey, String packageBusinessKey, User authenticatedUser) {
		Package aPackage = packageDAO.find(CompositeKeyHelper.getId(buildCompositeKey), packageBusinessKey, authenticatedUser);
		StringBuffer manifestDirectoryPathSB = s3PathHelper.getPackageManifestDirectoryPathPath(aPackage);
		List<String> files = fileHelper.listFiles(manifestDirectoryPathSB.toString());
		if (!files.isEmpty()) {
			return files.iterator().next();
		} else {
			return null;
		}
	}

	@Override
	public InputStream getManifestStream(String buildCompositeKey, String packageBusinessKey, User authenticatedUser) {
		Package aPackage = packageDAO.find(CompositeKeyHelper.getId(buildCompositeKey), packageBusinessKey, authenticatedUser);
		return getManifestStream(aPackage);
	}
	
	@Override
	public InputStream getManifestStream(Package pkg) {
		StringBuffer manifestDirectoryPathSB = s3PathHelper.getPackageManifestDirectoryPathPath(pkg);

		String directoryPath = manifestDirectoryPathSB.toString();
		List<String> files = fileHelper.listFiles(directoryPath);
		if (!files.isEmpty()) {
			String manifestFilePath = directoryPath + files.iterator().next(); // It's the only file in the manifest directory
			return fileHelper.getFileStream(manifestFilePath);
		} else {
			return null;
		}
	}

	@Override
	public void putInputFile(String buildCompositeKey, String packageBusinessKey, InputStream inputStream, String filename, long fileSize, User authenticatedUser) {
		Package aPackage = packageDAO.find(CompositeKeyHelper.getId(buildCompositeKey), packageBusinessKey, authenticatedUser);
		String pathPath = s3PathHelper.getPackageInputFilePath(aPackage, filename);
		fileHelper.putFile(inputStream, fileSize, pathPath);
	}
	
	@Override
	public
	String putOutputFile(Execution execution, Package aPackage, File file, boolean calcMD5) throws NoSuchAlgorithmException, IOException, DecoderException {
		String outputFilePath = s3PathHelper.getExecutionOutputFilePath(execution, aPackage.getBusinessKey(), file.getName());
		return fileHelper.putFile(file, outputFilePath, calcMD5);
	}

	@Override
	public InputStream getFileInputStream(String buildCompositeKey, String packageBusinessKey, String filename, User authenticatedUser) {
		Package aPackage = packageDAO.find(CompositeKeyHelper.getId(buildCompositeKey), packageBusinessKey, authenticatedUser);
		return getFileInputStream(aPackage, filename);
	}
	
	@Override
	public InputStream getFileInputStream(Package pkg, String filename) {
		String filePath = s3PathHelper.getPackageInputFilePath(pkg, filename);
		return fileHelper.getFileStream(filePath);
	}
	

	@Override
	public List<String> listFilePaths(String buildCompositeKey, String packageBusinessKey, User authenticatedUser) {
		Package aPackage = packageDAO.find(CompositeKeyHelper.getId(buildCompositeKey), packageBusinessKey, authenticatedUser);
		return listFilePaths(aPackage);
	}

	private List<String> listFilePaths(Package aPackage) {
		String directoryPath = s3PathHelper.getPackageInputFilesPath(aPackage);
		return fileHelper.listFiles(directoryPath);
	}

	@Override
	public void deleteFile(String buildCompositeKey, String packageBusinessKey, String filename, User authenticatedUser) {
		Package aPackage = packageDAO.find(CompositeKeyHelper.getId(buildCompositeKey), packageBusinessKey, authenticatedUser);
		String filePath = s3PathHelper.getPackageInputFilePath(aPackage, filename);
		fileHelper.deleteFile(filePath);
	}

	@Override
	public void copyAll(Build buildSource, Execution execution) {
		for (Package buildPackage : buildSource.getPackages()) {
			String buildPackageInputFilesPath = s3PathHelper.getPackageInputFilesPath(buildPackage);
			String executionPackageInputFilesPath = s3PathHelper.getExecutionInputFilesPath(execution, buildPackage).toString();
			List<String> filePaths = listFilePaths(buildPackage);
			for (String filePath : filePaths) {
				fileHelper.copyFile(buildPackageInputFilesPath + filePath, executionPackageInputFilesPath + filePath);
			}
		}
	}

	@Override
	public List<String> listInputFilePaths(Execution execution, String packageId) {
		String executionInputFilesPath = s3PathHelper.getExecutionInputFilesPath(execution, packageId).toString();
		return fileHelper.listFiles(executionInputFilesPath);
	}

	@Override
	public InputStream getExecutionInputFileStream(Execution execution, String packageBusinessKey, String inputFile) {
		String path = s3PathHelper.getExecutionInputFilePath(execution, packageBusinessKey, inputFile);
		return fileHelper.getFileStream(path);
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
		fileHelper.copyFile(executionInputFilePath, executionOutputFilePath);
	}

	@Override
	public OutputStream getExecutionOutputFileOutputStream(final String executionOutputFilePath) throws IOException {
		// Stream file to fileHelper as it's written to the OutputStream
		final PipedInputStream pipedInputStream = new PipedInputStream();
		final PipedOutputStream pipedOutputStream = new PipedOutputStream(pipedInputStream);

		executorService.submit(new Callable<String>() {
			@Override
			public String call() throws Exception {
				fileHelper.putFile(pipedInputStream, executionOutputFilePath);
				LOGGER.debug("Execution outputfile stream ended: {}", executionOutputFilePath);
				return executionOutputFilePath;
			}
		});

		return pipedOutputStream;
	}
}

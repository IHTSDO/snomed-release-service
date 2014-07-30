package org.ihtsdo.buildcloud.service;

import org.ihtsdo.buildcloud.dao.ProductDAO;
import org.ihtsdo.buildcloud.dao.helper.ExecutionS3PathHelper;
import org.ihtsdo.buildcloud.dao.helper.FileHelper;
import org.ihtsdo.buildcloud.dao.helper.S3ClientHelper;
import org.ihtsdo.buildcloud.dao.s3.S3Client;
import org.ihtsdo.buildcloud.entity.*;
import org.ihtsdo.buildcloud.entity.Package;
import org.ihtsdo.buildcloud.service.exception.BadRequestException;
import org.ihtsdo.buildcloud.service.exception.ResourceNotFoundException;
import org.ihtsdo.buildcloud.service.execution.RF2Constants;
import org.ihtsdo.buildcloud.service.file.FileUtils;
import org.ihtsdo.buildcloud.service.helper.CompositeKeyHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
@Transactional
public class PublishServiceImpl implements PublishService {

	private final FileHelper executionFileHelper;
	private final FileHelper publishedFileHelper;

	@Autowired
	private ExecutionS3PathHelper executionS3PathHelper;

	@Autowired
	ProductDAO productDAO;

	private static final String SEPARATOR = "/";
	private final String publishedBucketName;

	private static final Logger LOGGER = LoggerFactory.getLogger(PublishServiceImpl.class);

	/**
	 * @param executionBucketName
	 * @param publishedBucketName
	 */
	@Autowired
	public PublishServiceImpl(String executionBucketName, String publishedBucketName,
			S3Client s3Client, S3ClientHelper s3ClientHelper){
		executionFileHelper = new FileHelper(executionBucketName, s3Client, s3ClientHelper);
		this.publishedBucketName = publishedBucketName;
		publishedFileHelper = new FileHelper(publishedBucketName, s3Client, s3ClientHelper);
	}

	@Override
	public void publishExecutionPackage(Execution execution, Package pk) throws IOException {
		String pkgOutPutDir = executionS3PathHelper.getExecutionOutputFilesPath(execution, pk.getBusinessKey()).toString();
		List<String> filesFound = executionFileHelper.listFiles(pkgOutPutDir);
		String releaseFileName = null;
		for(String fileName : filesFound){
			if (FileUtils.isZip(fileName)) {
				releaseFileName = fileName;
				//only one zip file per package
				break;
			}
		}
		if (releaseFileName == null) {
			throw new IllegalStateException("No zip file found for package: " + pk.getBusinessKey());
		}
		String outputFileFullPath = executionS3PathHelper.getExecutionOutputFilePath(execution, pk.getBusinessKey(), releaseFileName);
		String publishedFilePath = getPublishFilePath(execution, releaseFileName);
		executionFileHelper.copyFile(outputFileFullPath, publishedBucketName, publishedFilePath);

		publishExtractedPackage(publishedFilePath, publishedFileHelper.getFileStream(publishedFilePath));
	}

	/**
	 * @param execution
	 * @return a file structure like
	 * releaseCenter/extension/product/
	 */
	private String getPublishDirPath(Execution execution) {
		Product product = execution.getBuild().getProduct();
		return getPublishDirPath(product);
	}

	/**
	 * @param product
	 * @return a file structure like
	 * releaseCenter/extension/product/
	 */
	private String getPublishDirPath(Product product) {
		Extension extension = product.getExtension();
		ReleaseCenter releaseCenter = extension.getReleaseCenter();
		StringBuffer path = new StringBuffer();
		path.append(releaseCenter.getBusinessKey());
		path.append(SEPARATOR);
		path.append(extension.getBusinessKey());
		path.append(SEPARATOR);
		path.append(product.getBusinessKey());
		path.append(SEPARATOR);
		return path.toString();
	}

	/**
	 * @param execution
	 * @param releaseFileName
	 * @return a file structure like
	 * releaseCenter/extension/product/releaseFileName.zip
	 */
	private String getPublishFilePath(Execution execution, String releaseFileName) {
		return getPublishDirPath(execution) + releaseFileName;
	}

	@Override
	public List<String> getPublishedPackages(Product product) {
		List<String> packages = new ArrayList<>();
		List<String> allFiles = publishedFileHelper.listFiles(getPublishDirPath(product));
		for (String file : allFiles) {
			if (file.endsWith(RF2Constants.ZIP_FILE_EXTENSION)) {
				packages.add(file);
			}
		}
		return packages;
	}

	@Override
	public boolean exists(Product product, String targetFileName) {
		String path = getPublishDirPath(product) + targetFileName;
		return publishedFileHelper.exists(path);
	}

	@Override
	public void publishPackage(String releaseCenterBusinessKey, String extensionBusinessKey, String productBusinessKey,
			InputStream inputStream, String originalFilename, long size, User subject) throws ResourceNotFoundException, BadRequestException, IOException {
		Product product = productDAO.find(releaseCenterBusinessKey, extensionBusinessKey, productBusinessKey, subject);

		if (product == null) {
			String item = CompositeKeyHelper.getPath(releaseCenterBusinessKey, extensionBusinessKey, productBusinessKey);
			throw new ResourceNotFoundException ("Unable to find product: " +  item);
		}

		//We're expecting a zip file only
		if (!FileUtils.isZip(originalFilename)) {
			throw new BadRequestException("File " + originalFilename + " is not named as a zip archive");
		}

		LOGGER.debug("Reading stream to temp file");
		File tempZipFile = Files.createTempFile(getClass().getCanonicalName(), ".zip").toFile();
		try (InputStream in = inputStream;
			OutputStream out = new FileOutputStream(tempZipFile)) {
			StreamUtils.copy(in, out);
		}

		// Upload file
		String publishFilePath = getPublishDirPath(product) + originalFilename;
		LOGGER.info("Uploading package to {}", publishFilePath);
		publishedFileHelper.putFile(new FileInputStream(tempZipFile), size, publishFilePath);

		publishExtractedPackage(publishFilePath, new FileInputStream(tempZipFile));

		// Delete temp zip file
		tempZipFile.delete();
	}

	// Publish extracted entries in a directory of the same name
	private void publishExtractedPackage(String publishFilePath, InputStream fileStream) throws IOException {
		String zipExtractPath = publishFilePath.replace(".zip", "/");
		LOGGER.info("Start: Upload extracted package to {}", zipExtractPath);
		try (ZipInputStream zipInputStream = new ZipInputStream(fileStream)) {
			ZipEntry entry;
			zipInputStream.closeEntry();
			while ((entry = zipInputStream.getNextEntry()) != null) {
				if (!entry.isDirectory()) {
					String name = entry.getName();

					// Copy to temp file first to prevent zip input stream being closed
					File tempFile = Files.createTempFile(getClass().getCanonicalName(), "zip-entry").toFile();
					try (FileOutputStream out = new FileOutputStream(tempFile)) {
						StreamUtils.copy(zipInputStream, out);
					}

					String targetFilePath = zipExtractPath + name;
					try (FileInputStream tempEntryInputStream = new FileInputStream(tempFile)) {
						publishedFileHelper.putFile(tempEntryInputStream, entry.getSize(), targetFilePath);
					}
					tempFile.delete();
				}
			}
		}
		LOGGER.info("Finish: Upload extracted package to {}", zipExtractPath);
	}

}

package org.ihtsdo.buildcloud.service;

import org.apache.log4j.MDC;
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

	private static final String SEPARATOR = "/";

	private static final Logger LOGGER = LoggerFactory.getLogger(PublishServiceImpl.class);

	private final FileHelper executionFileHelper;

	private final FileHelper publishedFileHelper;

	private final String publishedBucketName;

	@Autowired
	private ExecutionS3PathHelper executionS3PathHelper;

	@Autowired
	private ProductDAO productDAO;

	@Autowired
	public PublishServiceImpl(final String executionBucketName, final String publishedBucketName,
			final S3Client s3Client, final S3ClientHelper s3ClientHelper) {
		executionFileHelper = new FileHelper(executionBucketName, s3Client, s3ClientHelper);
		this.publishedBucketName = publishedBucketName;
		publishedFileHelper = new FileHelper(publishedBucketName, s3Client, s3ClientHelper);
	}

	/**
	 * @param execution
	 * @return a file structure like
	 * releaseCenter/extension/product/
	 */
	private String getPublishDirPath(final Execution execution) {
		Product product = execution.getBuild().getProduct();
		return getPublishDirPath(product);
	}

	/**
	 * @param product
	 * @return a file structure like
	 * releaseCenter/extension/product/
	 */
	private String getPublishDirPath(final Product product) {
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
	private String getPublishFilePath(final Execution execution, final String releaseFileName) {
		return getPublishDirPath(execution) + releaseFileName;
	}

	@Override
	public List<String> getPublishedPackages(final Product product) {
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
	public void publishExecutionPackage(final Execution execution, final Package pk) throws IOException {
		MDC.put(ExecutionService.MDC_EXECUTION_KEY, execution.getUniqueId());
		try {
			String pkgOutPutDir = executionS3PathHelper.getExecutionOutputFilesPath(execution, pk.getBusinessKey()).toString();
			List<String> filesFound = executionFileHelper.listFiles(pkgOutPutDir);
			String releaseFileName = null;
			String md5FileName = null;
			for (String fileName : filesFound) {
				if (releaseFileName == null && FileUtils.isZip(fileName)) {
					releaseFileName = fileName;
					//only one zip file per package
				}
				if (md5FileName == null && FileUtils.isMD5(fileName)) {
					//expected to be only one MD5 file.
					md5FileName = fileName;
				}
			}
			if (releaseFileName == null) {
				LOGGER.error("No zip file found for package:{}", pk.getBusinessKey());
			} else {
				String outputFileFullPath = executionS3PathHelper.getExecutionOutputFilePath(execution, pk.getBusinessKey(), releaseFileName);
				String publishedFilePath = getPublishFilePath(execution, releaseFileName);
				executionFileHelper.copyFile(outputFileFullPath, publishedBucketName, publishedFilePath);
				LOGGER.info("Release file:{} is copied to the published bucket:{}", releaseFileName, publishedBucketName);
				publishExtractedPackage(publishedFilePath, publishedFileHelper.getFileStream(publishedFilePath));
			}
			//copy MD5 file if available
			if (md5FileName != null) {
				String source = executionS3PathHelper.getExecutionOutputFilePath(execution, pk.getBusinessKey(), md5FileName);
				String target = getPublishFilePath(execution, md5FileName);
				executionFileHelper.copyFile(source, publishedBucketName, target);
				LOGGER.info("MD5 file:{} is copied to the published bucket:{}", md5FileName, publishedBucketName);
			}
		} finally {
			MDC.remove(ExecutionService.MDC_EXECUTION_KEY);
		}
	}

	@Override
	public void publishPackage(final String releaseCenterBusinessKey, final String extensionBusinessKey, final String productBusinessKey,
			final InputStream inputStream, final String originalFilename, final long size, final User subject) throws ResourceNotFoundException, BadRequestException, IOException {
		Product product = productDAO.find(releaseCenterBusinessKey, extensionBusinessKey, productBusinessKey, subject);

		if (product == null) {
			String item = CompositeKeyHelper.getPath(releaseCenterBusinessKey, extensionBusinessKey, productBusinessKey);
			throw new ResourceNotFoundException("Unable to find product: " + item);
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
		if (!tempZipFile.delete()) {
			LOGGER.warn("Failed to delete file {}", tempZipFile.getAbsolutePath());
		}
	}

	@Override
	public boolean exists(final Product product, final String targetFileName) {
		String path = getPublishDirPath(product) + targetFileName;
		return publishedFileHelper.exists(path);
	}

	// Publish extracted entries in a directory of the same name
	private void publishExtractedPackage(final String publishFilePath, final InputStream fileStream) throws IOException {
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
					if (!tempFile.delete()) {
						LOGGER.warn("Failed to delete file {}", tempFile.getAbsolutePath());
					}
				}
			}
		}
		LOGGER.info("Finish: Upload extracted package to {}", zipExtractPath);
	}

}

package org.ihtsdo.buildcloud.service;

import org.apache.log4j.MDC;
import org.ihtsdo.buildcloud.dao.helper.ExecutionS3PathHelper;
import org.ihtsdo.buildcloud.dao.helper.FileHelper;
import org.ihtsdo.buildcloud.dao.helper.S3ClientHelper;
import org.ihtsdo.buildcloud.dao.s3.S3Client;
import org.ihtsdo.buildcloud.entity.Execution;
import org.ihtsdo.buildcloud.entity.ReleaseCenter;
import org.ihtsdo.buildcloud.service.exception.BadRequestException;
import org.ihtsdo.buildcloud.service.exception.BusinessServiceException;
import org.ihtsdo.buildcloud.service.exception.EntityAlreadyExistsException;
import org.ihtsdo.buildcloud.service.execution.RF2Constants;
import org.ihtsdo.buildcloud.service.file.FileUtils;
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
	public PublishServiceImpl(final String executionBucketName, final String publishedBucketName,
			final S3Client s3Client, final S3ClientHelper s3ClientHelper) {
		executionFileHelper = new FileHelper(executionBucketName, s3Client, s3ClientHelper);
		this.publishedBucketName = publishedBucketName;
		publishedFileHelper = new FileHelper(publishedBucketName, s3Client, s3ClientHelper);
	}

	private String getPublishDirPath(final ReleaseCenter releaseCenter) {
		return releaseCenter.getBusinessKey() + SEPARATOR;
	}

	private String getPublishFilePath(final ReleaseCenter releaseCenter, final String releaseFileName) {
		return getPublishDirPath(releaseCenter) + releaseFileName;
	}

	@Override
	public List<String> getPublishedPackages(final ReleaseCenter releaseCenter) {
		List<String> packages = new ArrayList<>();
		List<String> allFiles = publishedFileHelper.listFiles(getPublishDirPath(releaseCenter));
		for (String file : allFiles) {
			if (file.endsWith(RF2Constants.ZIP_FILE_EXTENSION)) {
				packages.add(file);
			}
		}
		return packages;
	}

	@Override
	public void publishExecution(final Execution execution) throws BusinessServiceException {
		MDC.put(ExecutionService.MDC_EXECUTION_KEY, execution.getUniqueId());
		try {
			String pkgOutPutDir = executionS3PathHelper.getExecutionOutputFilesPath(execution).toString();
			ReleaseCenter rc = execution.getBuild().getReleaseCenter();
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
				LOGGER.error("No zip file found for execution:{}", execution.getUniqueId());
			} else {
				String fileLock = releaseFileName.intern();
				synchronized (fileLock) {
					//Does a published file already exist for this product?
					if (exists(rc, releaseFileName)) {
						throw new EntityAlreadyExistsException(releaseFileName + " has already been published for Release Center " + rc.getName() + " (" + execution.getCreationTime() + ")");
					}
					
					String outputFileFullPath = executionS3PathHelper.getExecutionOutputFilePath(execution, releaseFileName);
					String publishedFilePath = getPublishFilePath(execution.getBuild().getReleaseCenter(), releaseFileName);
					executionFileHelper.copyFile(outputFileFullPath, publishedBucketName, publishedFilePath);
					LOGGER.info("Release file:{} is copied to the published bucket:{}", releaseFileName, publishedBucketName);
					publishExtractedVersionOfPackage(publishedFilePath, publishedFileHelper.getFileStream(publishedFilePath));
					
					// copy MD5 file if available
					if (md5FileName != null) {
						String source = executionS3PathHelper.getExecutionOutputFilePath(execution, md5FileName);
						String target = getPublishFilePath(execution.getBuild().getReleaseCenter(), md5FileName);
						executionFileHelper.copyFile(source, publishedBucketName, target);
						LOGGER.info("MD5 file:{} is copied to the published bucket:{}", md5FileName, publishedBucketName);
					}
				}
			}
		} catch (IOException e) {
			throw new BusinessServiceException("Failed to publish execution " + execution.getUniqueId(), e);
		} finally {
			MDC.remove(ExecutionService.MDC_EXECUTION_KEY);
		}

	}

	@Override
	public void publishAdHocFile(ReleaseCenter releaseCenter, InputStream inputStream, String originalFilename, long size) throws BusinessServiceException {
		//We're expecting a zip file only
		if (!FileUtils.isZip(originalFilename)) {
			throw new BadRequestException("File " + originalFilename + " is not named as a zip archive");
		}

		File tempZipFile = null;
		try {
			//Synchronize on the product to protect against double uploads
			// Internalize the filename so we can use it as a synchronization object
			String fileLock = originalFilename.intern();
			synchronized (fileLock) {
				//Does a published file already exist for this product?
				if (exists(releaseCenter, originalFilename)) {
					throw new EntityAlreadyExistsException(originalFilename + " has already been published for " + releaseCenter.getName() );
				}
				
				LOGGER.debug("Reading stream to temp file");
				tempZipFile = Files.createTempFile(getClass().getCanonicalName(), ".zip").toFile();
				try (InputStream in = inputStream; OutputStream out = new FileOutputStream(tempZipFile)) {
					StreamUtils.copy(in, out);
				}
				
				// Upload file
				String publishFilePath = getPublishDirPath(releaseCenter) + originalFilename;
				LOGGER.info("Uploading package to {}", publishFilePath);
				publishedFileHelper.putFile(new FileInputStream(tempZipFile), size, publishFilePath);
				//Also upload the extracted version of the archive for random access performance improvements
				publishExtractedVersionOfPackage(publishFilePath, new FileInputStream(tempZipFile));
			}
		} catch (IOException e) {
			throw new BusinessServiceException("Failed to publish ad-hoc file.", e);
		} finally {
			// Delete temp zip file
			if (tempZipFile != null && tempZipFile.isFile()) {
				if (!tempZipFile.delete()) {
					LOGGER.warn("Failed to delete file {}", tempZipFile.getAbsolutePath());
				}
			}
		}
	}

	@Override
	public boolean exists(final ReleaseCenter releaseCenter, final String targetFileName) {
		String path = getPublishDirPath(releaseCenter) + targetFileName;
		return publishedFileHelper.exists(path);
	}

	// Publish extracted entries in a directory of the same name
	private void publishExtractedVersionOfPackage(final String publishFilePath, final InputStream fileStream) throws IOException {
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

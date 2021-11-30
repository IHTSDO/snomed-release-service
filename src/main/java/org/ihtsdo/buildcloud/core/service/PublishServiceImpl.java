package org.ihtsdo.buildcloud.core.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.MDC;
import org.ihtsdo.buildcloud.core.dao.helper.S3PathHelper;
import org.ihtsdo.buildcloud.core.service.build.RF2Constants;
import org.ihtsdo.buildcloud.core.service.helper.ProcessingStatus;
import org.ihtsdo.buildcloud.core.dao.BuildDAO;
import org.ihtsdo.buildcloud.core.entity.Build;
import org.ihtsdo.buildcloud.core.entity.ReleaseCenter;
import org.ihtsdo.buildcloud.core.service.identifier.client.IdServiceRestClient;
import org.ihtsdo.buildcloud.core.service.identifier.client.SchemeIdType;
import org.ihtsdo.otf.dao.s3.S3Client;
import org.ihtsdo.otf.dao.s3.helper.FileHelper;
import org.ihtsdo.otf.dao.s3.helper.S3ClientHelper;
import org.ihtsdo.otf.rest.client.RestClientException;
import org.ihtsdo.otf.rest.exception.BadRequestException;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.otf.rest.exception.EntityAlreadyExistsException;
import org.ihtsdo.otf.utils.FileUtils;
import org.ihtsdo.snomed.util.rf2.schema.ComponentType;
import org.ihtsdo.snomed.util.rf2.schema.FileRecognitionException;
import org.ihtsdo.snomed.util.rf2.schema.SchemaFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;

@Service
@Transactional
public class PublishServiceImpl implements PublishService {

	private static final String PUBLISHED_BUILD = "published_build_backup";

	private static final String SEPARATOR = "/";

	private static final Logger LOGGER = LoggerFactory.getLogger(PublishServiceImpl.class);

	private final FileHelper srsFileHelper;

	private static final Map<String, ProcessingStatus> concurrentPublishingBuildStatus = new ConcurrentHashMap<>();

	@Value("${srs.build.versioned-content.bucketName}")
	private String versionedContentBucket;

	@Value("${srs.build.versioned-content.path}")
	private String versionedContentPath;

	@Value("${srs.publish.job.storage.path}")
	private String publishJobStoragePath;

	@Autowired
	private S3PathHelper s3PathHelper;

	@Autowired
	private IdServiceRestClient idRestClient;

	@Autowired
	private BuildDAO buildDao;
	
	@Autowired
	private SchemaFactory schemaFactory;
	
	private static final int BATCH_SIZE = 5000;
	
	private static final int MAX_FAILURE = 100;

	public enum Status {
		RUNNING, FAILED, COMPLETED
	}

	@Autowired
	public PublishServiceImpl(@Value("${srs.storage.bucketName}") final String storageBucketName,
			final S3Client s3Client,
			final S3ClientHelper s3ClientHelper) {
		srsFileHelper = new FileHelper(storageBucketName, s3Client, s3ClientHelper);
	}

	@Override
	public List<String> getPublishedPackages(final ReleaseCenter releaseCenter) {
		List<String> allFiles = srsFileHelper.listFiles(getPublishDirPath(releaseCenter));
		return allFiles.stream().filter(file -> file.endsWith(RF2Constants.ZIP_FILE_EXTENSION)).collect(Collectors.toList());
	}

	@Override
	@Async("securityContextAsyncTaskExecutor")
	public void publishBuildAsync(Build build, boolean publishComponentIds, String env) {
		try {
			this.publishBuild(build, publishComponentIds, env);
		} catch (BusinessServiceException e) {
			LOGGER.error("Failed to publish the build {}. Error message: ", build.getId(), e.getMessage());
		}
	}

	@Override
	public ProcessingStatus getPublishingBuildStatus(Build build) {
		if (concurrentPublishingBuildStatus.containsKey(getBuildUniqueKey(build))) {
			synchronized (concurrentPublishingBuildStatus) {
				ProcessingStatus status = concurrentPublishingBuildStatus.get(getBuildUniqueKey(build));
				if (status != null && Status.RUNNING.name().equals(status.getStatus())) {
					return status;
				}
				return concurrentPublishingBuildStatus.remove(getBuildUniqueKey(build));
			}
		}
		return null;
	}

	@Override
	public void publishBuild(final Build build, boolean publishComponentIds, String env) throws BusinessServiceException {
		MDC.put(BuildService.MDC_BUILD_KEY, build.getUniqueId());

		ProcessingStatus currentStatus = concurrentPublishingBuildStatus.get(getBuildUniqueKey(build));
		if (currentStatus != null && Status.RUNNING.name().equals(currentStatus.getStatus())) {
			return;
		}
		concurrentPublishingBuildStatus.putIfAbsent(getBuildUniqueKey(build), new ProcessingStatus(Status.RUNNING.name(), null));
		try {
			String pkgOutPutDir = s3PathHelper.getBuildOutputFilesPath(build).toString();
			List<String> filesFound = srsFileHelper.listFiles(pkgOutPutDir);
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

			ReleaseCenter releaseCenter = build.getProduct().getReleaseCenter();
			if (releaseFileName == null) {
				String errorMessage = "No zip file found for build: " + build.getUniqueId();
				LOGGER.error(errorMessage);
				concurrentPublishingBuildStatus.put(getBuildUniqueKey(build), new ProcessingStatus(Status.FAILED.name(), errorMessage));
			} else {
				String fileLock = releaseFileName.intern();
				synchronized (fileLock) {
					//publish component ids
					if (publishComponentIds) {
						try {
							LOGGER.info("Start publishing component ids for product {}  with build id {} ", build.getProduct().getBusinessKey(), build.getId());
							String buildOutputDir = s3PathHelper.getBuildOutputFilesPath(build).toString();
							boolean isBetaRelease = build.getProduct().getBuildConfiguration().isBetaRelease();
							publishComponentIds(srsFileHelper, buildOutputDir, isBetaRelease, releaseFileName);
							LOGGER.info("End publishing component ids for product {}  with build id {} ", build.getProduct().getBusinessKey(), build.getId());
						} catch (BusinessServiceException e) {
							concurrentPublishingBuildStatus.put(getBuildUniqueKey(build), new ProcessingStatus(Status.FAILED.name(), "Failed to publish build " + build.getUniqueId() + ". Error message: " + e.getMessage()));
							throw e;
						}

					}
					//Does a published file already exist for this product?
					if (exists(releaseCenter, releaseFileName)) {
						String errorMessage = releaseFileName + " has already been published for Release Center " + releaseCenter.getName() + " (" + build.getCreationTime() + ")";
						concurrentPublishingBuildStatus.put(getBuildUniqueKey(build), new ProcessingStatus(Status.FAILED.name(), errorMessage));
						throw new EntityAlreadyExistsException(errorMessage);
					}
					try {
						String outputFileFullPath = s3PathHelper.getBuildOutputFilePath(build, releaseFileName);
						String publishedFilePath = getPublishFilePath(releaseCenter, releaseFileName);
						srsFileHelper.copyFile(outputFileFullPath, publishedFilePath);
						LOGGER.info("Release file: {} is copied to the published path: {}", releaseFileName, publishedFilePath);
						publishExtractedVersionOfPackage(publishedFilePath, srsFileHelper.getFileStream(publishedFilePath));

						copyBuildToVersionedContentsStore(outputFileFullPath, releaseFileName, env);
					} catch (BusinessServiceException e) {
						concurrentPublishingBuildStatus.put(getBuildUniqueKey(build), new ProcessingStatus(Status.FAILED.name(), "Failed to publish build " + build.getUniqueId() + ". Error message: " + e.getMessage()));
						throw e;
					}
				}
				// copy MD5 file if available
				if (md5FileName != null) {
					String source = s3PathHelper.getBuildOutputFilePath(build, md5FileName);
					String target = getPublishFilePath(releaseCenter, md5FileName);
					srsFileHelper.copyFile(source, target);
					LOGGER.info("MD5 file: {} is copied to the published path: {}", md5FileName, target);
				}
				
				// copy build info to published storage path
				backupPublishedBuild(build);

				// mark the build as Published
				buildDao.addTag(build, Build.Tag.PUBLISHED);
				concurrentPublishingBuildStatus.put(getBuildUniqueKey(build), new ProcessingStatus(Status.COMPLETED.name(), null));
			}
		} catch (IOException e) {
			concurrentPublishingBuildStatus.put(getBuildUniqueKey(build), new ProcessingStatus(Status.FAILED.name(), "Failed to publish build " + build.getUniqueId() + ". Error message: " + e.getMessage()));
			throw new BusinessServiceException("Failed to publish build " + build.getUniqueId(), e);
		} finally {
			MDC.remove(BuildService.MDC_BUILD_KEY);
		}
	}

	@Override
	public void publishAdHocFile(ReleaseCenter releaseCenter, InputStream inputStream, String originalFilename, long size, boolean publishComponentIds) throws BusinessServiceException {
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
					throw new EntityAlreadyExistsException(originalFilename + " has already been published for " + releaseCenter.getName());
				}
				
				LOGGER.debug("Reading stream to temp file");
				tempZipFile = Files.createTempFile(getClass().getCanonicalName(), ".zip").toFile();
				try (InputStream in = inputStream; OutputStream out = new FileOutputStream(tempZipFile)) {
					StreamUtils.copy(in, out);
				}
				
				// Upload file
				String publishFilePath = getPublishDirPath(releaseCenter) + originalFilename;
				LOGGER.info("Uploading package to {}", publishFilePath);
				srsFileHelper.putFile(new FileInputStream(tempZipFile), size, publishFilePath);
				//Also upload the extracted version of the archive for random access performance improvements
				publishExtractedVersionOfPackage(publishFilePath, new FileInputStream(tempZipFile));
				
				// publish component ids
				if (publishComponentIds) {
					boolean isBetaRelease = originalFilename.startsWith(RF2Constants.BETA_RELEASE_PREFIX);
					String publishFileExtractedDir = publishFilePath.replace(".zip", "/");
					LOGGER.info("Start publishing component ids for published file {} ", originalFilename);
					publishComponentIds(srsFileHelper, publishFileExtractedDir, isBetaRelease, originalFilename);
					LOGGER.info("End publishing component ids for published file {} ", originalFilename);
				}
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
		LOGGER.info("Check if published file exists for path {} in storage bucket", path);
		return srsFileHelper.exists(path);
	}

	// Publish extracted entries in a directory of the same name
	private void publishExtractedVersionOfPackage(final String publishFilePath, final InputStream fileStream) throws IOException {
		String zipExtractPath = publishFilePath.replace(".zip", SEPARATOR);
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
						srsFileHelper.putFile(tempEntryInputStream, entry.getSize(), targetFilePath);
					}
					if (!tempFile.delete()) {
						LOGGER.warn("Failed to delete file {}", tempFile.getAbsolutePath());
					}
				}
			}
		}
		LOGGER.info("Finish: Upload extracted package to {}", zipExtractPath);
	}

	
	private void publishComponentIds(FileHelper fileHelper, String fileRootPath, boolean isBetaRelease, String releaseFileName) throws BusinessServiceException {
		try {
			try {
				idRestClient.logIn();
			} catch (RestClientException e) {
				throw new BusinessServiceException("Failed to logIn to the id service",e);
			}
			List<String> filesFound = fileHelper.listFiles(fileRootPath);
			LOGGER.info("Total files found {} from file path {}", filesFound.size(), fileRootPath);
			LOGGER.info("isBetaRelease flag is set to {}", isBetaRelease);
			
			for (String fileName : filesFound) {
					if (fileName.endsWith(RF2Constants.TXT_FILE_EXTENSION) && fileName.contains(RF2Constants.DELTA)) {
						String filenameToCheck = isBetaRelease ? fileName.replaceFirst(RF2Constants.BETA_RELEASE_PREFIX, RF2Constants.EMPTY_SPACE) : fileName;
						// file name might contain parent folder
						if (fileName.contains(SEPARATOR)) {
							String[] splits = fileName.split(SEPARATOR);
							filenameToCheck = splits[splits.length-1];
							filenameToCheck = isBetaRelease ? filenameToCheck.replaceFirst(RF2Constants.BETA_RELEASE_PREFIX, RF2Constants.EMPTY_SPACE) : filenameToCheck ;
						}
						if (filenameToCheck.startsWith(RF2Constants.SCT2)) {
							try {
								ComponentType type = schemaFactory.createSchemaBean(filenameToCheck).getComponentType();
								if (ComponentType.REFSET != type) {
									publishSctIds(fileHelper.getFileStream(fileRootPath + fileName), fileName, releaseFileName);
								}
							} catch (IOException | RestClientException | FileRecognitionException e) {
								throw new BusinessServiceException("Failed to publish SctIDs for file:" + fileName, e);
							}
						}
						if (filenameToCheck.startsWith(RF2Constants.DER2) && filenameToCheck.contains(RF2Constants.SIMPLE_MAP_FILE_IDENTIFIER)) {
							try {
								publishLegacyIds(fileHelper.getFileStream(fileRootPath + fileName), fileName, releaseFileName);
							} catch (IOException | RestClientException e) {
								throw new BusinessServiceException("Failed to publish LegacyIds for file:" + fileName, e);
							}
						}
					}
			}
		} finally {
			try {
				idRestClient.logOut();
			} catch (RestClientException e) {
				LOGGER.warn("Failed to log out the id service", e);
			}
		}
	}

	private Map<SchemeIdType, Collection<String>> getLegacyIdsFromFile(final InputStream inputStream) throws IOException {
		Map<SchemeIdType, Collection<String>> result = new HashMap<SchemeIdType, Collection<String>>();
		result.put(SchemeIdType.CTV3ID, new HashSet<String>());
		result.put(SchemeIdType.SNOMEDID, new HashSet<String>());
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, RF2Constants.UTF_8))) {
			String line = null;
			boolean isFirstLine = true;
			while ((line = reader.readLine()) != null) {
				if (isFirstLine) {
					isFirstLine = false;
					continue;
				}
				String[] columnValues = line.split(RF2Constants.COLUMN_SEPARATOR,-1);
				String refSetId = columnValues[4];
				String mapTarget = columnValues[6];
				if (mapTarget == null || mapTarget.isEmpty()) {
					LOGGER.warn("Found map target is null or empty for refsetId:" + refSetId);
					continue;
				}
				if (RF2Constants.CTV3_ID_REFSET_ID.equals(refSetId)) {
					result.get(SchemeIdType.CTV3ID).add(mapTarget);
				} else if (RF2Constants.SNOMED_ID_REFSET_ID.equals(refSetId)) {
					result.get(SchemeIdType.SNOMEDID).add(mapTarget);
				}
			}
		} 
		return result;
	}
	
	private void publishLegacyIds(final InputStream inputFileStream, String filename, String buildId) throws IOException, RestClientException {
		Map<SchemeIdType, Collection<String>> result = getLegacyIdsFromFile(inputFileStream);
		for (SchemeIdType type : result.keySet()) {
			int publishedIdCounter = 0;
			int assignedIdCounter = 0;
			List<String> batchJob = null;
			int counter = 0;
			for (String legacyId : result.get(type)) {
				if (batchJob == null) {
					batchJob = new ArrayList<>();
				}
				batchJob.add(legacyId);
				counter++;
				if (counter % BATCH_SIZE == 0 || counter == result.get(type).size()) {
					Map<String,String> idStatusMap = idRestClient.getStatusForSchemeIds(type, batchJob);
					List<String> idsAssigned = new ArrayList<>();
					for (String id : idStatusMap.keySet()) {
						String status = idStatusMap.get(id);
						if (IdServiceRestClient.ID_STATUS.ASSIGNED.getName().equals(status)) {
							idsAssigned.add(id);
						} else if (IdServiceRestClient.ID_STATUS.PUBLISHED.getName().equals(status)) {
							publishedIdCounter++;
						}
					}
					if (!idsAssigned.isEmpty()) {
						assignedIdCounter += idsAssigned.size();
						idRestClient.publishSchemeIds(idsAssigned, type, buildId);
					}
					batchJob = null;
				}
			}
			LOGGER.info("Found total {} ids {} in file {} with assigned status: {} and published status: {}", 
					type, result.get(type).size(), filename, assignedIdCounter, publishedIdCounter);
		}
	}

	private void publishSctIds(InputStream inputFileStream, String filename, String buildId) throws IOException, RestClientException {
		Set<Long> sctIds = getSctIdsFromFile(inputFileStream);
		List<Long> batchJob = null;
		int counter = 0;
		int publishedAlreadyCounter = 0;
		int assignedStatusCounter = 0;
		List<Long> otherStatusIds = new ArrayList<>();
		for (Long id : sctIds) {
			if (batchJob == null) {
				batchJob = new ArrayList<>();
			}
			batchJob.add(id);
			counter++;
			if (counter % BATCH_SIZE == 0 || counter == sctIds.size()) {
				Map<Long,String> sctIdStatusMap = idRestClient.getStatusForSctIds(batchJob);
				if (batchJob.size() != sctIdStatusMap.size()) {
					LOGGER.warn("Total sctids reqeusted {} but total status returned {}", batchJob.size(),sctIdStatusMap.size());
				}
				List<Long> assignedIds = new ArrayList<>();
				for (Long sctId : batchJob) {
					String status = sctIdStatusMap.get(sctId);
					if (IdServiceRestClient.ID_STATUS.ASSIGNED.getName().equals(status)) {
						assignedStatusCounter++;
						assignedIds.add(sctId);
					} else if (IdServiceRestClient.ID_STATUS.PUBLISHED.getName().equals(status)) {
						publishedAlreadyCounter++;
					} else {
						otherStatusIds.add(sctId);
					}
				}
				if (!assignedIds.isEmpty()) {
					//publishing sctId grouped in batch by namespace id
					Map<String,List<Long>> sctIdsByNamespaceMap = groupSctIdsByNamespace(assignedIds);
					for ( String namespace : sctIdsByNamespaceMap.keySet()) {
						boolean isSuccessful = idRestClient.publishSctIds(sctIdsByNamespaceMap.get(namespace), new Integer(namespace), buildId);
						if (!isSuccessful) {
							LOGGER.error("Publishing sctids for file {} is completed with error.", filename);
						}
					}
				}
				batchJob = null;
			}
		}
		LOGGER.info("Found total sctIds {} in file {} with assigned status {} , published status {} and other status {}", 
				sctIds.size(), filename, assignedStatusCounter, publishedAlreadyCounter, otherStatusIds.size());
		if (otherStatusIds.size() > 0) {
			StringBuilder msgBuilder = new StringBuilder("the following SctIds are not in assigned or published status:");
			boolean isFirstOne = true;
			int failureCounter = 0;
			for (Long id : otherStatusIds) {
				if (failureCounter > MAX_FAILURE) {
					break;
				}
				if (!isFirstOne) {
					msgBuilder.append(",");
				}
				if (isFirstOne) {
					isFirstOne = false;
				}
				msgBuilder.append(id);
				failureCounter++;
			}
			LOGGER.warn("Total ids have not been published {} in file {} for example {} ", otherStatusIds.size(), filename, msgBuilder.toString());
		}
		
	}
	
	private Map<String, List<Long>> groupSctIdsByNamespace(List<Long> assignedIds) {
		Map<String, List<Long>> result = new HashMap<String, List<Long>>();
		for (Long sctId : assignedIds) {
			int total = String.valueOf(sctId).length();
			String partitionId = String.valueOf(sctId).substring(total-3,total-1);
			String namespaceId = null;
			if (partitionId.charAt(0) == '0') {
				namespaceId = "0";
			} else if (partitionId.charAt(0) == '1') {
				namespaceId = String.valueOf(sctId).substring(total-10,total-3);
			} else {
				LOGGER.error("Invalid partition id:" + partitionId + " for sctId:" + sctId);
			}
			if (namespaceId != null) {
				List<Long> existing = result.get(namespaceId);
				if (existing != null) {
					existing.add(sctId);
					
				} else {
					existing = new ArrayList<>();
					existing.add(sctId);
					result.put(namespaceId, existing);
				}
			}
		}
		return result;
	}

	private Set<Long> getSctIdsFromFile(InputStream inputFileStream) throws IOException {
		Set<Long> sctIds = new HashSet<>();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputFileStream, RF2Constants.UTF_8))) {
			String line = null;
			boolean isFirstLine = true;
			while ((line = reader.readLine()) != null) {
				if (isFirstLine) {
					isFirstLine = false;
					continue;
				}
				String[] columnValues = line.split(RF2Constants.COLUMN_SEPARATOR,-1);
				sctIds.add(new Long(columnValues[0]));
			}
		} 
		return sctIds;
	}

	private String getPublishDirPath(final ReleaseCenter releaseCenter) {
		return s3PathHelper.getPublishJobDirectoryPath(releaseCenter);
	}

	private String getPublishFilePath(final ReleaseCenter releaseCenter, final String releaseFileName) {
		return getPublishDirPath(releaseCenter) + releaseFileName;
	}

	private void copyBuildToVersionedContentsStore(String releaseFileFullPath, String releaseFileName, String prefix) throws BusinessServiceException {
		try {
			StringBuilder outputPathBuilder = new StringBuilder(versionedContentPath);
			if(!versionedContentPath.endsWith("/")) outputPathBuilder.append("/");
			if(StringUtils.isNotBlank(prefix)) outputPathBuilder.append(prefix.toUpperCase() + "_");
			outputPathBuilder.append(releaseFileName);
			srsFileHelper.copyFile(releaseFileFullPath, versionedContentBucket, outputPathBuilder.toString());
		} catch (Exception e) {
			LOGGER.error("Failed to copy release file {} to versioned contents repository because of error: {}", releaseFileName, e.getMessage());
			throw new BusinessServiceException(String.format("Failed to copy release file %s to versioned contents repository", releaseFileName), e);
		}
	}

	private void backupPublishedBuild(Build build) {
		String originalBuildPath = s3PathHelper.getBuildPath(build).toString();
		List<String> buildFiles = srsFileHelper.listFiles(originalBuildPath);
		String buildBckUpPath = getPublishDirPath(build.getProduct().getReleaseCenter()) + PUBLISHED_BUILD + SEPARATOR
				+ build.getProduct().getBusinessKey() + SEPARATOR + build.getId() + SEPARATOR;
		for (String filename : buildFiles) {
			srsFileHelper.copyFile(originalBuildPath + filename, buildBckUpPath + filename);
		}
		LOGGER.info("Build: {} is copied to path: {}", build.getProduct().getBusinessKey() + build.getId(), buildBckUpPath);
	}

	private String getBuildUniqueKey(Build build) {
		return build.getProduct().getReleaseCenter().getBusinessKey() + "|" + build.getProduct().getBusinessKey() + "|" + build.getId();
	}
}

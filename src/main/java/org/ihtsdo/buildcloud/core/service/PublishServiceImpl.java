package org.ihtsdo.buildcloud.core.service;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.MDC;
import org.ihtsdo.buildcloud.core.dao.BuildDAO;
import org.ihtsdo.buildcloud.core.dao.ReleaseCenterDAO;
import org.ihtsdo.buildcloud.core.dao.helper.S3PathHelper;
import org.ihtsdo.buildcloud.core.entity.Build;
import org.ihtsdo.buildcloud.core.entity.BuildConfiguration;
import org.ihtsdo.buildcloud.core.entity.QATestConfig;
import org.ihtsdo.buildcloud.core.entity.ReleaseCenter;
import org.ihtsdo.buildcloud.core.service.build.RF2Constants;
import org.ihtsdo.buildcloud.core.service.helper.ProcessingStatus;
import org.ihtsdo.buildcloud.core.service.identifier.client.IdServiceRestClient;
import org.ihtsdo.buildcloud.core.service.identifier.client.SchemeIdType;
import org.ihtsdo.otf.dao.s3.S3Client;
import org.ihtsdo.otf.dao.s3.helper.FileHelper;
import org.ihtsdo.otf.rest.client.RestClientException;
import org.ihtsdo.otf.rest.client.terminologyserver.pojo.CodeSystem;
import org.ihtsdo.otf.rest.exception.BadRequestException;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.otf.rest.exception.EntityAlreadyExistsException;
import org.ihtsdo.otf.rest.exception.ResourceNotFoundException;
import org.ihtsdo.otf.utils.FileUtils;
import org.ihtsdo.snomed.util.rf2.schema.ComponentType;
import org.ihtsdo.snomed.util.rf2.schema.FileRecognitionException;
import org.ihtsdo.snomed.util.rf2.schema.SchemaFactory;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StreamUtils;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
@Transactional
public class PublishServiceImpl implements PublishService {

	private static final String PUBLISHED_BUILD = "published_build_backup";

	private static final Logger LOGGER = LoggerFactory.getLogger(PublishServiceImpl.class);

	private final FileHelper srsFileHelper;

	private static final Map<String, ProcessingStatus> concurrentPublishingBuildStatus = new ConcurrentHashMap<>();

	@Value("${srs.publish.job.useOwnBackupBucket}")
	private Boolean useOwnBackupBucket;

	@Value("${srs.publish.job.backup.storage.bucketName}")
	private String publishJobBackupStorageBucketName;

	@Value("${srs.published.releases.storage.path}")
	private String publishedReleasesStoragePath;

	@Value("${srs.publish.job.storage.path}")
	private String publishJobStoragePath;

	@Value("${srs.build.versioned-content.bucketName}")
	private String versionedContentBucket;

	@Value("${srs.build.versioned-content.path}")
	private String versionedContentPath;

	@Value("${srs.storage.bucketName}")
	private String storageBucketName;

	@Autowired
	private TermServerService termServerService;

	@Autowired
	private S3PathHelper s3PathHelper;

	private final S3Client s3Client;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private IdServiceRestClient idRestClient;

	@Autowired
	private BuildDAO buildDao;

	@Autowired
	private ReleaseCenterDAO releaseCenterDAO;
	
	@Autowired
	private SchemaFactory schemaFactory;
	
	private static final int BATCH_SIZE = 5000;
	
	private static final int MAX_FAILURE = 100;

	public enum Status {
		RUNNING, FAILED, COMPLETED
	}

	@Autowired
	public PublishServiceImpl(@Value("${srs.storage.bucketName}") final String storageBucketName,
			final S3Client s3Client) {
		this.srsFileHelper = new FileHelper(storageBucketName, s3Client);
		this.s3Client = s3Client;
	}

	@Override
	public List<String> getPublishedPackages(final ReleaseCenter releaseCenter) {
		List<String> allFiles = srsFileHelper.listFiles(s3PathHelper.getPublishJobDirectoryPath(releaseCenter.getBusinessKey()));
		return allFiles.stream().filter(file -> file.endsWith(RF2Constants.ZIP_FILE_EXTENSION)).collect(Collectors.toList());
	}

	@Override
	public List<Build> findPublishedBuilds(String releaseCenterKey, String productKey, boolean includeProdPublishedReleases) throws ResourceNotFoundException {
		List<Build> builds = new ArrayList<>();
		String buildBckUpPath = s3PathHelper.getPublishJobDirectoryPath(releaseCenterKey) + PUBLISHED_BUILD + S3PathHelper.SEPARATOR + productKey + S3PathHelper.SEPARATOR;
		findPublishedBuilds(this.storageBucketName, releaseCenterKey, productKey, builds, buildBckUpPath);
		if (includeProdPublishedReleases) {
			buildBckUpPath = s3PathHelper.getPublishedReleasesDirectoryPath(releaseCenterKey) + PUBLISHED_BUILD + S3PathHelper.SEPARATOR + productKey + S3PathHelper.SEPARATOR;
			findPublishedBuilds(this.storageBucketName, releaseCenterKey, productKey, builds, buildBckUpPath);
		}

		if (Boolean.TRUE.equals(useOwnBackupBucket)) {
			buildBckUpPath = s3PathHelper.getPublishJobBackupDirectoryPath(releaseCenterKey) + productKey + S3PathHelper.SEPARATOR;
			findPublishedBuilds(this.publishJobBackupStorageBucketName, releaseCenterKey, productKey, builds, buildBckUpPath);
			if (includeProdPublishedReleases) {
				buildBckUpPath = s3PathHelper.getPublishedReleasesBackupDirectoryPath(releaseCenterKey) + productKey + S3PathHelper.SEPARATOR;
				findPublishedBuilds(this.publishJobBackupStorageBucketName, releaseCenterKey, productKey, builds, buildBckUpPath);
			}
		}
		return builds;
	}

	@Override
	public Map<String, String> getPublishedBuildPathMap(String releaseCenterKey, String productKey) {
		Map<String, String> buildPathMap = new HashMap<>();
		String buildBckUpPath = s3PathHelper.getPublishJobDirectoryPath(releaseCenterKey) + PUBLISHED_BUILD + S3PathHelper.SEPARATOR + productKey + S3PathHelper.SEPARATOR;
		findPublishedBuildPathMap(this.storageBucketName, buildPathMap, buildBckUpPath);
		buildBckUpPath = s3PathHelper.getPublishedReleasesDirectoryPath(releaseCenterKey) + PUBLISHED_BUILD + S3PathHelper.SEPARATOR + productKey + S3PathHelper.SEPARATOR;
		findPublishedBuildPathMap(this.storageBucketName, buildPathMap, buildBckUpPath);

		if (Boolean.TRUE.equals(useOwnBackupBucket)) {
			buildBckUpPath = s3PathHelper.getPublishJobBackupDirectoryPath(releaseCenterKey) + productKey + S3PathHelper.SEPARATOR;
			findPublishedBuildPathMap(this.publishJobBackupStorageBucketName, buildPathMap, buildBckUpPath);
			buildBckUpPath = s3PathHelper.getPublishedReleasesBackupDirectoryPath(releaseCenterKey) + productKey + S3PathHelper.SEPARATOR;
			findPublishedBuildPathMap(this.publishJobBackupStorageBucketName, buildPathMap, buildBckUpPath);
		}
		return buildPathMap;
	}

	@Override
	@Async("securityContextAsyncTaskExecutor")
	public void publishBuildAsync(Build build, boolean publishComponentIds, String env) {
		try {
			this.publishBuild(build, publishComponentIds, env);
		} catch (Exception e) {
			LOGGER.error("An error occurs while publishing the build {}. Error message: {}", build.getId(), e.getMessage());
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
	public void publishBuild(final Build build, boolean publishComponentIds, String env) throws BusinessServiceException, IOException, DecoderException {
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

			if (releaseFileName == null) {
				String errorMessage = "No zip file found for build: " + build.getUniqueId();
				LOGGER.error(errorMessage);
				concurrentPublishingBuildStatus.put(getBuildUniqueKey(build), new ProcessingStatus(Status.FAILED.name(), errorMessage));
			} else {
				String fileLock = releaseFileName.intern();
				synchronized (fileLock) {

					// verify if the published file already exists for this product
					String publishFilePath = s3PathHelper.getPublishJobFilePath(build.getReleaseCenterKey(), releaseFileName);
					if (srsFileHelper.exists(publishFilePath)) {
						String errorMessage = publishFilePath + " has already been published for Release Center " + build.getReleaseCenterKey() + " (" + build.getCreationTime() + ")";
						concurrentPublishingBuildStatus.put(getBuildUniqueKey(build), new ProcessingStatus(Status.FAILED.name(), errorMessage));
						throw new EntityAlreadyExistsException(errorMessage);
					}

					// publish component ids
					if (publishComponentIds) {
						try {
							LOGGER.info("Start publishing component ids for product {}  with build id {} ", build.getProductKey(), build.getId());
							String buildOutputDir = s3PathHelper.getBuildOutputFilesPath(build).toString();
							boolean isBetaRelease = build.getConfiguration().isBetaRelease();
							publishComponentIds(srsFileHelper, buildOutputDir, isBetaRelease, releaseFileName);
							LOGGER.info("End publishing component ids for product {}  with build id {} ", build.getProductKey(), build.getId());
						} catch (Exception e) {
							concurrentPublishingBuildStatus.put(getBuildUniqueKey(build), new ProcessingStatus(Status.FAILED.name(), "Failed to publish build " + build.getUniqueId() + ". Error message: " + e.getMessage()));
							throw e;
						}
					}

					try {
						String outputFileFullPath = s3PathHelper.getBuildOutputFilePath(build, releaseFileName);
						srsFileHelper.copyFile(outputFileFullPath, publishFilePath);
						LOGGER.info("Release file: {} is copied to the published path: {}", releaseFileName, publishFilePath);
						publishExtractedVersionOfPackage(publishFilePath, srsFileHelper.getFileStream(publishFilePath));
						copyBuildToVersionedContentsStore(outputFileFullPath, releaseFileName, env);

						// mark the build as Published
						buildDao.addTag(build, Build.Tag.PUBLISHED);
					} catch (Exception e) {
						concurrentPublishingBuildStatus.put(getBuildUniqueKey(build), new ProcessingStatus(Status.FAILED.name(), "Failed to publish build " + build.getUniqueId() + ". Error message: " + e.getMessage()));
						throw e;
					}
				}

				// copy MD5 file if available
				if (md5FileName != null) {
					String source = s3PathHelper.getBuildOutputFilePath(build, md5FileName);
					String target = s3PathHelper.getPublishJobFilePath(build.getReleaseCenterKey(), md5FileName);
					srsFileHelper.copyFile(source, target);
					LOGGER.info("MD5 file: {} is copied to the published path: {}", md5FileName, target);
				}

				// copy build info to the backup published storage path
				try {
					backupPublishedBuild(build);
				} catch (Exception e) {
					concurrentPublishingBuildStatus.put(getBuildUniqueKey(build), new ProcessingStatus(Status.COMPLETED.name(), "The build has been published successfully but failed to copy to Backup published storage.  Error message: " + e.getMessage()));
					throw new BusinessServiceException("Failed to copy to Backup published storage", e);
				}

				// update the release package in code system
				if (!build.getConfiguration().isBetaRelease() && !StringUtils.isEmpty(build.getConfiguration().getBranchPath())) {
					try {
						List<CodeSystem> codeSystems = termServerService.getCodeSystems();
						ReleaseCenter releaseCenter = releaseCenterDAO.find(build.getReleaseCenterKey());
						CodeSystem codeSystem = codeSystems.stream().filter(item -> releaseCenter.getCodeSystem().equals(item.getShortName()))
								.findAny()
								.orElse(null);
						if (codeSystem != null && build.getConfiguration().getBranchPath().startsWith(codeSystem.getBranchPath())) {
							LOGGER.info("Update the release package for Code System Version: {}, {}, {}", codeSystem.getShortName(), build.getConfiguration().getEffectiveTimeSnomedFormat(), releaseFileName);
							termServerService.updateCodeSystemVersionPackage(codeSystem.getShortName(), build.getConfiguration().getEffectiveTimeSnomedFormat(), releaseFileName);
						}
					} catch (Exception e) {
						concurrentPublishingBuildStatus.put(getBuildUniqueKey(build), new ProcessingStatus(Status.COMPLETED.name(), "The build has been published successfully but failed to update Code System Version Package.  Error message: " + e.getMessage()));
						throw new BusinessServiceException("Failed to update Code System Version Package", e);
					}
				}
				concurrentPublishingBuildStatus.put(getBuildUniqueKey(build), new ProcessingStatus(Status.COMPLETED.name(), null));
			}
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
				// Does a published file already exist for this product?
				String publishFilePath = s3PathHelper.getPublishJobFilePath(releaseCenter.getBusinessKey(), originalFilename);
				if (srsFileHelper.exists(publishFilePath)) {
					throw new EntityAlreadyExistsException(publishFilePath + " has already been published for Release Center " + releaseCenter.getName());
				}
				
				LOGGER.debug("Reading stream to temp file");
				tempZipFile = Files.createTempFile(getClass().getCanonicalName(), ".zip").toFile();
				try (InputStream in = inputStream; OutputStream out = new FileOutputStream(tempZipFile)) {
					StreamUtils.copy(in, out);
				}
				
				// Upload file
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
		} catch (IOException | DecoderException e) {
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
	// Check if previously published release file exists
	// For scenarios in UAT and DEV where we use locally published release packages for a new build,
	// if the file is not found in ${srs.published.releases.storage.path}, then look in ${srs.publish.job.storage.path}
	public boolean exists(final ReleaseCenter releaseCenter, final String targetFileName) {
		String path = s3PathHelper.getPublishedReleasesFilePath(releaseCenter.getBusinessKey(), targetFileName);
		LOGGER.info("Check if published file exists for path {} in storage bucket", path);
		boolean exists = srsFileHelper.exists(path);

		if (!exists && !publishedReleasesStoragePath.equals(publishJobStoragePath)) {
			path = s3PathHelper.getPublishJobFilePath(releaseCenter.getBusinessKey(), targetFileName);
			LOGGER.info("Check if published file exists for path {} in storage bucket", path);
			exists = srsFileHelper.exists(path);
		}
		return exists;
	}

	// Publish extracted entries in a directory of the same name
	private void publishExtractedVersionOfPackage(final String publishFilePath, final InputStream fileStream) throws IOException, DecoderException {
		String zipExtractPath = publishFilePath.replace(".zip", S3PathHelper.SEPARATOR);
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
						if (fileName.contains(S3PathHelper.SEPARATOR)) {
							String[] splits = fileName.split(S3PathHelper.SEPARATOR);
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
		Map<SchemeIdType, Collection<String>> result = new HashMap<>();
		result.put(SchemeIdType.CTV3ID, new HashSet<>());
		result.put(SchemeIdType.SNOMEDID, new HashSet<>());
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, RF2Constants.UTF_8))) {
			String line;
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
						boolean isSuccessful = idRestClient.publishSctIds(sctIdsByNamespaceMap.get(namespace), Integer.valueOf(namespace), buildId);
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
		Map<String, List<Long>> result = new HashMap<>();
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
			String line;
			boolean isFirstLine = true;
			while ((line = reader.readLine()) != null) {
				if (isFirstLine) {
					isFirstLine = false;
					continue;
				}
				String[] columnValues = line.split(RF2Constants.COLUMN_SEPARATOR,-1);
				sctIds.add(Long.valueOf(columnValues[0]));
			}
		} 
		return sctIds;
	}

	private void copyBuildToVersionedContentsStore(String releaseFileFullPath, String releaseFileName, String prefix) throws BusinessServiceException {
		try {
			StringBuilder outputPathBuilder = new StringBuilder(versionedContentPath);
			if(!versionedContentPath.endsWith("/")) outputPathBuilder.append("/");
			if(StringUtils.isNotBlank(prefix)) outputPathBuilder.append(prefix.toUpperCase()).append("_");
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
		String buildBckUpPath;
		if (Boolean.TRUE.equals(useOwnBackupBucket)) {
			buildBckUpPath = s3PathHelper.getPublishJobBackupDirectoryPath(build.getReleaseCenterKey())
					+ build.getProductKey() + S3PathHelper.SEPARATOR
					+ build.getId() + S3PathHelper.SEPARATOR;
			for (String filename : buildFiles) {
				srsFileHelper.copyFile(originalBuildPath + filename, publishJobBackupStorageBucketName, buildBckUpPath + filename);
			}
		} else {
			buildBckUpPath = s3PathHelper.getPublishJobDirectoryPath(build.getReleaseCenterKey())
					+ PUBLISHED_BUILD + S3PathHelper.SEPARATOR
					+ build.getProductKey() + S3PathHelper.SEPARATOR
					+ build.getId() + S3PathHelper.SEPARATOR;
			for (String filename : buildFiles) {
				srsFileHelper.copyFile(originalBuildPath + filename, buildBckUpPath + filename);
			}
		}
		LOGGER.info("Build: {} is copied to path: {}", build.getProductKey() + build.getId(), buildBckUpPath);
	}

	private String getBuildUniqueKey(Build build) {
		return build.getReleaseCenterKey() + "|" + build.getProductKey() + "|" + build.getId();
	}

	private void findPublishedBuildPathMap(String storageBucketName, Map<String, String> buildPathMap, String buildBckUpPath) {
		ListObjectsRequest listObjectsRequest = ListObjectsRequest.builder().bucket(storageBucketName).prefix(buildBckUpPath).maxKeys(10000).build();

		boolean done = false;
		while (!done) {
			ListObjectsResponse listObjectsResponse = s3Client.listObjects(listObjectsRequest);
			for (S3Object s3Object : listObjectsResponse.contents()) {
				String key = s3Object.key();
				if (key.contains("/status:")) {
					String[] keyParts = key.split("/");
					String dateString = keyParts[keyParts.length - 2];
					if (!buildPathMap.containsKey(dateString)) {
						buildPathMap.put(dateString, storageBucketName + S3PathHelper.SEPARATOR + buildBckUpPath + dateString + S3PathHelper.SEPARATOR);
					}
				}
			}
			if (Boolean.TRUE.equals(listObjectsResponse.isTruncated())) {
				String nextMarker = listObjectsResponse.contents().get(listObjectsResponse.contents().size() - 1).key();
				listObjectsRequest = ListObjectsRequest.builder().bucket(storageBucketName).prefix(buildBckUpPath).maxKeys(10000).marker(nextMarker).build();
			} else {
				done = true;
			}
		}
	}

	private void findPublishedBuilds(String storageBucketName, String releaseCenterKey, String productKey, List<Build> builds, String buildBckUpPath) {
		List<Build> foundBuilds = new ArrayList<>();
		ListObjectsRequest listObjectsRequest = ListObjectsRequest.builder().bucket(storageBucketName).prefix(buildBckUpPath).maxKeys(10000).build();

		boolean done = false;
		while (!done) {
			ListObjectsResponse listObjectsResponse = s3Client.listObjects(listObjectsRequest);
			for (S3Object s3Object : listObjectsResponse.contents()) {
				String key = s3Object.key();
				if (key.contains("/status:")) {
					String[] keyParts = key.split("/");
					String dateString = keyParts[keyParts.length - 2];
					String status = keyParts[keyParts.length - 1].split(":")[1];
					if (builds.stream().noneMatch(b -> b.getId().equals(dateString))) {
						Build build = new Build(dateString, releaseCenterKey, productKey, status);
						foundBuilds.add(build);
					}
				}
			}

			if (Boolean.TRUE.equals(listObjectsResponse.isTruncated())) {
				String nextMarker = listObjectsResponse.contents().get(listObjectsResponse.contents().size() - 1).key();
				listObjectsRequest = ListObjectsRequest.builder().bucket(storageBucketName).prefix(buildBckUpPath).maxKeys(10000).marker(nextMarker).build();
			} else {
				done = true;
			}
		}

		foundBuilds.forEach(build -> {
			if (build.getStatus().equals(Build.Status.BUILT)
					|| build.getStatus().equals(Build.Status.RVF_QUEUED)
					|| build.getStatus().equals(Build.Status.RVF_RUNNING)
					|| build.getStatus().equals(Build.Status.RELEASE_COMPLETE)
					|| build.getStatus().equals(Build.Status.RELEASE_COMPLETE_WITH_WARNINGS)) {
				try (InputStream buildReportStream = getBuildReportFileStream(storageBucketName, buildBckUpPath, build)) {
					if (buildReportStream != null) {
						JSONParser jsonParser = new JSONParser();
						try {
							JSONObject jsonObject = (JSONObject) jsonParser.parse(new InputStreamReader(buildReportStream, StandardCharsets.UTF_8));
							if (jsonObject.containsKey("rvf_response")) {
								build.setRvfURL(jsonObject.get("rvf_response").toString());
							}
						} catch (IOException e) {
							LOGGER.error("Error reading rvf_url from build_report file. Error: {}", e.getMessage());
						} catch (ParseException e) {
							LOGGER.error("Error parsing build_report file. Error: {}", e.getMessage());
						}
					}
				} catch (IOException e) {
					LOGGER.error("Error retrieving RVF rport for build {}. Error: {}", build.getId(), e.getMessage());
                }
            }
			try {
				this.loadBuildConfiguration(storageBucketName, buildBckUpPath, build);
			} catch (IOException e) {
				LOGGER.error("Error retrieving Build Configuration for build {}", build.getId());
			}
			try {
				this.loadQaTestConfig(storageBucketName, buildBckUpPath, build);
			} catch (IOException e) {
				LOGGER.error("Error retrieving QA Configuration for build {}", build.getId());
			}
		});

		builds.addAll(foundBuilds);
	}

	private InputStream getBuildReportFileStream(final String storageBucketName, final String buildBckUpPath, final Build build) throws IOException {
		final String reportFilePath = getPublishedReleaseFilePath(buildBckUpPath, build, S3PathHelper.BUILD_REPORT_JSON);
		return getFileStream(storageBucketName, reportFilePath);
	}

	private void loadBuildConfiguration(final String storageBucketName, final String buildBckUpPath,final Build build) throws IOException {
		final String configFilePath = getPublishedReleaseFilePath(buildBckUpPath, build, S3PathHelper.CONFIG_JSON);
		try (InputStream inputStream = getFileStream(storageBucketName, configFilePath)) {
			if (inputStream == null) {
				throw new IOException("Configuration file not found for build " + build.getId());
			}
			final String configurationJson = FileCopyUtils.copyToString(new InputStreamReader(inputStream, RF2Constants.UTF_8));
			try (JsonParser jsonParser = objectMapper.getFactory().createParser(configurationJson)) {
				final BuildConfiguration buildConfiguration = jsonParser.readValueAs(BuildConfiguration.class);
				build.setConfiguration(buildConfiguration);
			}
		}
	}

	private void loadQaTestConfig(final String storageBucketName, final String buildBckUpPath, final Build build) throws IOException {
		final String configFilePath = getPublishedReleaseFilePath(buildBckUpPath, build, S3PathHelper.QA_CONFIG_JSON);
		try (InputStream inputStream = getFileStream(storageBucketName, configFilePath)) {
			if (inputStream == null) {
				throw new IOException("QA Configuration file not found for build " + build.getId());
			}
			final String configurationJson = FileCopyUtils.copyToString(new InputStreamReader(inputStream, RF2Constants.UTF_8));
			try (JsonParser jsonParser = objectMapper.getFactory().createParser(configurationJson)) {
				final QATestConfig qaTestConfig = jsonParser.readValueAs(QATestConfig.class);
				build.setQaTestConfig(qaTestConfig);
			}
		}
	}

	private InputStream getFileStream(String bucketName, String filePath) {
		return this.s3Client.getObject(bucketName, filePath);
	}

	private String getPublishedReleaseFilePath(String buildBckUpPath, Build build, String fileName) {
		return buildBckUpPath + build.getId() + S3PathHelper.SEPARATOR + fileName;
	}
}

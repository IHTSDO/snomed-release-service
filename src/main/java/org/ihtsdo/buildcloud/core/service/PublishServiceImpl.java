package org.ihtsdo.buildcloud.core.service;

import com.google.common.collect.Lists;
import io.swagger.v3.core.util.Constants;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.MDC;
import org.ihtsdo.buildcloud.core.dao.BuildDAO;
import org.ihtsdo.buildcloud.core.dao.ReleaseCenterDAO;
import org.ihtsdo.buildcloud.core.dao.helper.S3PathHelper;
import org.ihtsdo.buildcloud.core.entity.Build;
import org.ihtsdo.buildcloud.core.entity.ReleaseCenter;
import org.ihtsdo.buildcloud.core.service.build.RF2Constants;
import org.ihtsdo.buildcloud.core.service.helper.ProcessingStatus;
import org.ihtsdo.buildcloud.core.service.identifier.client.IdServiceRestClient;
import org.ihtsdo.buildcloud.core.service.identifier.client.SchemeIdType;
import org.ihtsdo.otf.dao.s3.S3Client;
import org.ihtsdo.otf.dao.s3.helper.FileHelper;
import org.ihtsdo.otf.exception.ScriptException;
import org.ihtsdo.otf.rest.client.RestClientException;
import org.ihtsdo.otf.rest.client.terminologyserver.pojo.Branch;
import org.ihtsdo.otf.rest.client.terminologyserver.pojo.CodeSystem;
import org.ihtsdo.otf.rest.exception.BadRequestException;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.otf.rest.exception.EntityAlreadyExistsException;
import org.ihtsdo.otf.rest.exception.ResourceNotFoundException;
import org.ihtsdo.otf.utils.FileUtils;
import org.ihtsdo.snomed.util.rf2.schema.ComponentType;
import org.ihtsdo.snomed.util.rf2.schema.FileRecognitionException;
import org.ihtsdo.snomed.util.rf2.schema.SchemaFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.module.storage.ModuleStorageCoordinator;
import org.snomed.module.storage.ModuleStorageCoordinatorException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.ihtsdo.buildcloud.core.service.PermissionServiceCache.BRANCH_ROOT;

@Service
@Transactional
public class PublishServiceImpl implements PublishService {

	private static final String SNOMEDCT = "SNOMEDCT";

	private static final Logger LOGGER = LoggerFactory.getLogger(PublishServiceImpl.class);

	private final FileHelper srsFileHelper;

	private static final Map<String, ProcessingStatus> concurrentPublishingBuildStatus = new ConcurrentHashMap<>();

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

	@Value("${srs.publish.batch.size}")
	private int BATCH_SIZE;

	@Autowired
	private TermServerService termServerService;

	@Autowired
	private S3PathHelper s3PathHelper;

	@Autowired
	private IdServiceRestClient idRestClient;

	@Autowired
	private BuildDAO buildDao;

	@Autowired
	private ReleaseCenterDAO releaseCenterDAO;
	
	@Autowired
	private SchemaFactory schemaFactory;

	@Autowired
	private ModuleStorageCoordinator moduleStorageCoordinator;

	@Autowired
	private ModuleStorageCoordinatorCache moduleStorageCoordinatorCache;

	private static final int MAX_FAILURE = 100;

	private record ReleaseFile(String releaseFileName, String md5FileName) {}

	public enum Status {
		RUNNING, FAILED, COMPLETED
	}

	@Autowired
	public PublishServiceImpl(@Value("${srs.storage.bucketName}") final String storageBucketName,
			final S3Client s3Client) {
		this.srsFileHelper = new FileHelper(storageBucketName, s3Client);
	}

	@Override
	public List<String> getPublishedPackages(final ReleaseCenter releaseCenter) {
		List<String> allFiles = srsFileHelper.listFiles(s3PathHelper.getPublishJobDirectoryPath(releaseCenter.getBusinessKey()));
		return allFiles.stream().filter(file -> file.endsWith(RF2Constants.ZIP_FILE_EXTENSION)).collect(Collectors.toList());
	}

	@Override
	public List<Build> findPublishedBuilds(String releaseCenterKey, String productKey) throws ResourceNotFoundException {
		List<Build> builds = buildDao.findAllDesc(releaseCenterKey, productKey, null, null, true, null);
		return builds.stream().filter(item -> item.getTags() != null && item.getTags().contains(Build.Tag.PUBLISHED)).toList();
	}

	@Override
	public Map<String, String> getPublishedBuildPathMap(String releaseCenterKey, String productKey) {
		Map<String, String> buildPathMap = new HashMap<>();
		List<Build> publishedBuilds = this.findPublishedBuilds(releaseCenterKey, productKey);
		for (Build build : publishedBuilds) {
			buildPathMap.put(build.getId(), storageBucketName + S3PathHelper.SEPARATOR + s3PathHelper.getBuildPath(releaseCenterKey, productKey, build.getId()));
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
			ReleaseFile releaseFiles = getReleaseFiles(filesFound);

			if (releaseFiles.releaseFileName() == null) {
				String errorMessage = "No zip file found for build: " + build.getUniqueId();
				LOGGER.error(errorMessage);
				concurrentPublishingBuildStatus.put(getBuildUniqueKey(build), new ProcessingStatus(Status.FAILED.name(), errorMessage));
			} else {
				String fileLock = releaseFiles.releaseFileName().intern();
				synchronized (fileLock) {
					String publishFilePath = s3PathHelper.getPublishJobFilePath(build.getReleaseCenterKey(), releaseFiles.releaseFileName());

					// validate the release file
					validateReleaseFile(build, publishFilePath);

					// publish component ids
					if (publishComponentIds) {
						publishComponentIds(build, releaseFiles.releaseFileName());
					}

					// copy the release package to published and versioned folders
					try {
						copyReleaseFileToPublishedAndVersionedDirectory(build, env, releaseFiles.releaseFileName(), publishFilePath);

						// mark the build as Published
						buildDao.addTag(build, Build.Tag.PUBLISHED);
					} catch (Exception e) {
						concurrentPublishingBuildStatus.put(getBuildUniqueKey(build), new ProcessingStatus(Status.FAILED.name(), "Failed to publish build " + build.getUniqueId() + ". Error message: " + e.getMessage()));
						throw e;
					}
				}

				// copy MD5 file if available
				if (releaseFiles.md5FileName() != null) {
					copyMd5FileToPublishedDirectory(build, releaseFiles.md5FileName());
				}

				// upload to new versioned content bucket which will be used by Module Storage Coordinator
				try {
					uploadReleaseFileToNewVersionedContentBucket(build, releaseFiles);
					moduleStorageCoordinatorCache.clearCachedReleases();
				} catch (Exception e) {
					concurrentPublishingBuildStatus.put(getBuildUniqueKey(build), new ProcessingStatus(Status.COMPLETED.name(), "The build has been published successfully but failed to upload to new versioned content bucket (Module Storage Coordinator).  Error message: " + e.getMessage()));
					throw new BusinessServiceException("Failed to to upload to new versioned content bucket (Module Storage Coordinator)", e);
				}

				// update the release package in code system
				if (!build.getConfiguration().isBetaRelease() && !StringUtils.isEmpty(build.getConfiguration().getBranchPath())) {
					updateCodeSystemVersion(build, releaseFiles.releaseFileName());
				}
				concurrentPublishingBuildStatus.put(getBuildUniqueKey(build), new ProcessingStatus(Status.COMPLETED.name(), null));
			}
		} finally {
			MDC.remove(BuildService.MDC_BUILD_KEY);
		}
	}

	private void uploadReleaseFileToNewVersionedContentBucket(Build build, ReleaseFile releaseFiles) throws IOException, RestClientException, ModuleStorageCoordinatorException.InvalidArgumentsException, ModuleStorageCoordinatorException.ResourceNotFoundException, ModuleStorageCoordinatorException.DuplicateResourceException, ModuleStorageCoordinatorException.OperationFailedException, ScriptException {
		List<CodeSystem> codeSystems = termServerService.getCodeSystems();
		ReleaseCenter releaseCenter = releaseCenterDAO.find(build.getReleaseCenterKey());
		CodeSystem codeSystem = codeSystems.stream().filter(item -> releaseCenter.getCodeSystem().equals(item.getShortName()))
				.findAny()
				.orElse(null);
		if (codeSystem != null) {
			File releaseFile = new File(releaseFiles.releaseFileName());
			try {
				try (InputStream inputFileStream = srsFileHelper.getFileStream(s3PathHelper.getBuildOutputFilePath(build, releaseFiles.releaseFileName()));
					 FileOutputStream out = new FileOutputStream(releaseFile)) {
					StreamUtils.copy(inputFileStream, out);
				}

				String moduleId = getModuleId(build);
				String codeSystemShortname = SNOMEDCT.equals(codeSystem.getShortName()) ? RF2Constants.INT : codeSystem.getShortName().substring(codeSystem.getShortName().indexOf('-') + 1);
				moduleStorageCoordinator.upload(codeSystemShortname, moduleId, build.getConfiguration().getEffectiveTimeSnomedFormat(), releaseFile);
			} finally {
				org.apache.commons.io.FileUtils.forceDelete(releaseFile);
			}
		}
	}

	public Map<String, Object> getBranchMetadataIncludeInherited(String path) throws RestClientException {
		Map<String, Object> mergedMetadata = null;
		List<String> stackPaths = getBranchPathStack(path);
		for (String stackPath : stackPaths) {
			final Branch branch = termServerService.getBranch(stackPath);
			final Map<String, Object> metadata = branch.getMetadata();
			if (mergedMetadata == null) {
				mergedMetadata = metadata;
			} else {
				// merge metadata
				for (String key : metadata.keySet()) {
					if (!key.equals("lock") || stackPath.equals(path)) { // Only copy lock info from the deepest branch
						mergedMetadata.put(key, metadata.get(key));
					}
				}
			}
		}
		return mergedMetadata;
	}

	private List<String> getBranchPathStack(String path) {
		List<String> paths = new ArrayList<>();
		paths.add(path);
		int index;
		while ((index = path.lastIndexOf("/")) != -1) {
			path = path.substring(0, index);
			paths.add(path);
		}
		return Lists.reverse(paths);
	}

	private static ReleaseFile getReleaseFiles(List<String> filesFound) {
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
		return new ReleaseFile(releaseFileName, md5FileName);
	}



	private void validateReleaseFile(Build build, String publishFilePath) throws EntityAlreadyExistsException {
		// verify if the published file already exists for this product
		if (srsFileHelper.exists(publishFilePath)) {
			String errorMessage = publishFilePath + " has already been published for Release Center " + build.getReleaseCenterKey() + " (" + build.getCreationTime() + ")";
			concurrentPublishingBuildStatus.put(getBuildUniqueKey(build), new ProcessingStatus(Status.FAILED.name(), errorMessage));
			throw new EntityAlreadyExistsException(errorMessage);
		}
	}

	private void publishComponentIds(Build build, String releaseFileName) throws BusinessServiceException {
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

	private void copyReleaseFileToPublishedAndVersionedDirectory(Build build, String env, String releaseFileName, String publishFilePath) throws IOException, DecoderException, BusinessServiceException {
		String outputFileFullPath = s3PathHelper.getBuildOutputFilePath(build, releaseFileName);
		srsFileHelper.copyFile(outputFileFullPath, publishFilePath);
		LOGGER.info("Release file: {} is copied to the published path: {}", releaseFileName, publishFilePath);
		publishExtractedVersionOfPackage(publishFilePath, srsFileHelper.getFileStream(publishFilePath));
		copyBuildToVersionedContentsStore(outputFileFullPath, releaseFileName, env);
	}

	private void copyMd5FileToPublishedDirectory(Build build, String md5FileName) {
		String source = s3PathHelper.getBuildOutputFilePath(build, md5FileName);
		String target = s3PathHelper.getPublishJobFilePath(build.getReleaseCenterKey(), md5FileName);
		srsFileHelper.copyFile(source, target);
		LOGGER.info("MD5 file: {} is copied to the published path: {}", md5FileName, target);
	}

	private void updateCodeSystemVersion(Build build, String releaseFileName) throws BusinessServiceException {
		try {
			List<CodeSystem> codeSystems = termServerService.getCodeSystems();
			ReleaseCenter releaseCenter = releaseCenterDAO.find(build.getReleaseCenterKey());
			CodeSystem codeSystem = codeSystems.stream().filter(item -> releaseCenter.getCodeSystem().equals(item.getShortName()))
					.findAny()
					.orElse(null);
			if (codeSystem != null && build.getConfiguration().getBranchPath().startsWith(codeSystem.getBranchPath())
			&& (!BRANCH_ROOT.equals(codeSystem.getBranchPath()) || !build.getConfiguration().getBranchPath().contains("SNOMEDCT-"))) {
				LOGGER.info("Update the release package for Code System Version: {}, {}, {}", codeSystem.getShortName(), build.getConfiguration().getEffectiveTimeSnomedFormat(), releaseFileName);
				termServerService.updateCodeSystemVersionPackage(codeSystem.getShortName(), build.getConfiguration().getEffectiveTimeSnomedFormat(), releaseFileName);
			}
		} catch (Exception e) {
			concurrentPublishingBuildStatus.put(getBuildUniqueKey(build), new ProcessingStatus(Status.COMPLETED.name(), "The build has been published successfully but failed to update Code System Version Package.  Error message: " + e.getMessage()));
			throw new BusinessServiceException("Failed to update Code System Version Package", e);
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
								if (ComponentType.REFSET != type && ComponentType.IDENTIFIER != type) {
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

	private String getModuleId(Build build) throws RestClientException {
		String moduleId = null;
		if (!StringUtils.isEmpty(build.getConfiguration().getBranchPath())) {
			Map<String, Object> metadata = getBranchMetadataIncludeInherited(build.getConfiguration().getBranchPath());
			moduleId = (String) metadata.get("defaultModuleId");
		}
		if (StringUtils.isEmpty(moduleId) && build.getConfiguration().getExtensionConfig() != null) {
			if (!StringUtils.isEmpty(build.getConfiguration().getExtensionConfig().getDefaultModuleId())) {
				moduleId = build.getConfiguration().getExtensionConfig().getDefaultModuleId();
			} else {
				String moduleIdsStr = build.getConfiguration().getExtensionConfig().getModuleIds();
				if (!StringUtils.isEmpty(moduleIdsStr)) {
					moduleId = moduleIdsStr.split(Constants.COMMA)[0].trim();
				}
			}
		}
		if (StringUtils.isEmpty(moduleId)) {
			moduleId = RF2Constants.INTERNATIONAL_CORE_MODULE_ID;
		}
		return moduleId;
	}

	private String getBuildUniqueKey(Build build) {
		return build.getReleaseCenterKey() + "|" + build.getProductKey() + "|" + build.getId();
	}
}

package org.ihtsdo.buildcloud.service;

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
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.log4j.MDC;
import org.ihtsdo.buildcloud.dao.BuildDAO;
import org.ihtsdo.buildcloud.dao.helper.BuildS3PathHelper;
import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.BuildConfiguration;
import org.ihtsdo.buildcloud.entity.ReleaseCenter;
import org.ihtsdo.buildcloud.service.build.RF2Constants;
import org.ihtsdo.buildcloud.service.identifier.client.IdServiceRestClient;
import org.ihtsdo.buildcloud.service.identifier.client.SchemeIdType;
import org.ihtsdo.otf.dao.s3.S3Client;
import org.ihtsdo.otf.dao.s3.helper.FileHelper;
import org.ihtsdo.otf.dao.s3.helper.S3ClientHelper;
import org.ihtsdo.otf.rest.client.RestClientException;
import org.ihtsdo.otf.rest.exception.BadRequestException;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.otf.rest.exception.EntityAlreadyExistsException;
import org.ihtsdo.otf.utils.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;

@Service
@Transactional
public class PublishServiceImpl implements PublishService {

	private static final String PUBLISHED_BUILD = "published_build_backup";

	private static final String SEPARATOR = "/";

	private static final Logger LOGGER = LoggerFactory.getLogger(PublishServiceImpl.class);

	private final FileHelper buildFileHelper;

	private final FileHelper publishedFileHelper;

	private final String publishedBucketName;

	@Autowired
	private BuildS3PathHelper buildS3PathHelper;

	@Autowired
	private IdServiceRestClient idRestClient;

	@Autowired
	private BuildDAO buildDao;
	
	private static final int BATCH_SIZE = 5000;


	@Autowired
	public PublishServiceImpl(final String buildBucketName, final String publishedBucketName,
			final S3Client s3Client, final S3ClientHelper s3ClientHelper) {
		buildFileHelper = new FileHelper(buildBucketName, s3Client, s3ClientHelper);
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
	public void publishBuild(final Build build) throws BusinessServiceException {
		MDC.put(BuildService.MDC_BUILD_KEY, build.getUniqueId());
		try {
			String pkgOutPutDir = buildS3PathHelper.getBuildOutputFilesPath(build).toString();
			List<String> filesFound = buildFileHelper.listFiles(pkgOutPutDir);
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
				LOGGER.error("No zip file found for build:{}", build.getUniqueId());
			} else {
				String fileLock = releaseFileName.intern();
				synchronized (fileLock) {
					//Does a published file already exist for this product?
					if (exists(releaseCenter, releaseFileName)) {
						throw new EntityAlreadyExistsException(releaseFileName + " has already been published for Release Center " + releaseCenter.getName() + " (" + build.getCreationTime() + ")");
					}

					String outputFileFullPath = buildS3PathHelper.getBuildOutputFilePath(build, releaseFileName);
					String publishedFilePath = getPublishFilePath(releaseCenter, releaseFileName);
					buildFileHelper.copyFile(outputFileFullPath, publishedBucketName, publishedFilePath);
					LOGGER.info("Release file:{} is copied to the published bucket:{}", releaseFileName, publishedBucketName);
					publishExtractedVersionOfPackage(publishedFilePath, publishedFileHelper.getFileStream(publishedFilePath));
				}
				// copy MD5 file if available
				if (md5FileName != null) {
					String source = buildS3PathHelper.getBuildOutputFilePath(build, md5FileName);
					String target = getPublishFilePath(releaseCenter, md5FileName);
					buildFileHelper.copyFile(source, publishedBucketName, target);
					LOGGER.info("MD5 file:{} is copied to the published bucket:{}", md5FileName, publishedBucketName);
				}
				
				//copy build info to published bucket
				backupPublishedBuild(build,publishedBucketName);
				LOGGER.info("Build:{} is copied to the published bucket:{}", build.getProduct().getBusinessKey() + build.getId(), publishedBucketName);
			}
		} catch (IOException e) {
			throw new BusinessServiceException("Failed to publish build " + build.getUniqueId(), e);
		} finally {
			MDC.remove(BuildService.MDC_BUILD_KEY);
		}

	}

	private void backupPublishedBuild(Build build, String publishedBucketName) {
		String orginalBuildPath = buildS3PathHelper.getBuildPath(build).toString();
		List<String> buildFiles =  buildFileHelper.listFiles(orginalBuildPath);
		String buildBckUpPath = getPublishDirPath(build.getProduct().getReleaseCenter()) + PUBLISHED_BUILD + SEPARATOR 
				+ build.getProduct().getBusinessKey() + SEPARATOR + build.getId() + SEPARATOR;
		for (String filename : buildFiles) {
			buildFileHelper.copyFile(orginalBuildPath + filename , publishedBucketName, buildBckUpPath  + filename);
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

	@Override
	public void publishComponentIds(Build build) throws BusinessServiceException {
		LOGGER.info("Start publishing component ids for product {}  with build id {} ", build.getProduct().getBusinessKey(), build.getId());
		MDC.put(BuildService.MDC_BUILD_KEY, build.getUniqueId());
		try {
			try {
				idRestClient.logIn();
			} catch (RestClientException e) {
				throw new BusinessServiceException("Failed to logIn to the id service",e);
			}
			String buildOutputDir = buildS3PathHelper.getBuildOutputFilesPath(build).toString();
			List<String> filesFound = buildFileHelper.listFiles(buildOutputDir);
			boolean isBetaRelease = build.getProduct().getBuildConfiguration().isBetaRelease();
			for (String fileName : filesFound) {
				String filenameToCheck = isBetaRelease ? fileName.replace(BuildConfiguration.BETA_PREFIX, RF2Constants.EMPTY_SPACE) : fileName;
					if (filenameToCheck.endsWith(RF2Constants.TXT_FILE_EXTENSION) && filenameToCheck.contains(RF2Constants.DELTA)) {
						if (filenameToCheck.startsWith(RF2Constants.SCT2)) {
							try {
								publishSctIds(buildDao.getOutputFileInputStream(build, fileName), fileName, build.getId());
							} catch (IOException | RestClientException e) {
								throw new BusinessServiceException("Failed to publish SctIDs for file:" + fileName, e);
							}
						}
						
						if (filenameToCheck.startsWith(RF2Constants.DER2) && filenameToCheck.contains(RF2Constants.SIMPLE_MAP_FILE_IDENTIFIER)) {
							try {
								publishLegacyIds(buildDao.getOutputFileInputStream(build, fileName), fileName, build.getId());
							} catch (IOException | RestClientException e) {
								throw new BusinessServiceException("Failed to publish LegacyIds for file:" + fileName, e);
							}
						}
					}
			}
		} finally {
			MDC.remove(BuildService.MDC_BUILD_KEY);
			try {
				idRestClient.logOut();
			} catch (RestClientException e) {
				LOGGER.warn("Failed to log out the id service", e);
			}
		}
		LOGGER.info("End publishing component ids for product {}  with build id {} ", build.getProduct().getBusinessKey(), build.getId());
	}

	private Map<SchemeIdType, Collection<String>> getLegacyIdsFromFile(InputStream inputStream) throws IOException {
		Map<SchemeIdType, Collection<String>> result = new HashMap<SchemeIdType, Collection<String>>();
		result.put(SchemeIdType.CTV3ID, new HashSet<String>());
		result.put(SchemeIdType.SNOMEDID, new HashSet<String>());
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, RF2Constants.UTF_8))) {
			String line = null;
			boolean isFirstLine = true;
			while (( line = reader.readLine()) != null) {
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
				if (RF2Constants.CTV3_ID_REFSET_ID.equals(refSetId) ) {
					result.get(SchemeIdType.CTV3ID).add(mapTarget);
				} else if (RF2Constants.SNOMED_ID_REFSET_ID.equals(refSetId)) {
					result.get(SchemeIdType.SNOMEDID).add(mapTarget);
				}
			}
		} 
		return result;
	}
	
	private void publishLegacyIds(InputStream inputFileStream, String filename, String buildId) throws IOException, RestClientException {
		Map<SchemeIdType, Collection<String>> result = getLegacyIdsFromFile(inputFileStream);
		for (SchemeIdType type : result.keySet()) {
			int publishedIdCounter = 0;
			Map<String,String> idStatusMap = idRestClient.getSchemeIdStatusMap(type, result.get(type));
			List<String> idsAssigned = new ArrayList<>();
			for (String id : idStatusMap.keySet()) {
				String status = idStatusMap.get(id);
				if (IdServiceRestClient.ID_STATUS.ASSIGNED.getName().equals(status)) {
					idsAssigned.add(id);
				} else if (IdServiceRestClient.ID_STATUS.PUBLISHED.getName().equals(status)) {
					publishedIdCounter++;
				}
			}
			LOGGER.info("Found total {} ids {} in file {} with assigned status: {} and published status: {}", 
					type, idStatusMap.size(), filename, idsAssigned.size(), publishedIdCounter);
			if (!idsAssigned.isEmpty()) {
				idRestClient.publishSchemeIds(idsAssigned, type, buildId);
			}
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
			if (counter % BATCH_SIZE == 0 || counter == batchJob.size()) {
				Map<Long,String> sctIdStatusMap = idRestClient.getSctidStatusMap(batchJob);
				if ( batchJob.size() != sctIdStatusMap.size()) {
					LOGGER.warn("Total sctids reqeusted {} but total status returned {}", batchJob.size(),sctIdStatusMap.size());
				}
				List<Long> assignedIds = new ArrayList<>();
				for (Long sctId : batchJob) {
					String status = sctIdStatusMap.get(sctId);
					if (IdServiceRestClient.ID_STATUS.ASSIGNED.getName().equals(status)) {
						assignedStatusCounter++;
						assignedIds.add(sctId);
					} else if (IdServiceRestClient.ID_STATUS.PUBLISHED.getName().equals(status)) {
						publishedAlreadyCounter ++;
					} else {
						otherStatusIds.add(sctId);
					}
				}
				if (!assignedIds.isEmpty()) {
					idRestClient.publishSctIds(assignedIds, RF2Constants.INTERNATIONAL_NAMESPACE_ID, buildId);
				}
				batchJob = null;
			}
		}
		LOGGER.info("Found total sctIds {} in file {} with assigned status {} , published statu {} and other status {}", 
				sctIds.size(), filename, assignedStatusCounter, publishedAlreadyCounter, otherStatusIds.size());
		if (otherStatusIds.size() > 0) {
			StringBuilder msgBuilder = new StringBuilder("the following SctIds are not in assigned or published status:");
			boolean isFirstOne = true;
			for (Long id : otherStatusIds) {
				if (!isFirstOne) {
					msgBuilder.append(",");
				}
				if (isFirstOne) {
					isFirstOne = false;
				}
				msgBuilder.append(id);
			}
			LOGGER.warn("Total ids have not been published {} in file {} as {} ", otherStatusIds.size(), filename, msgBuilder.toString());
		}
		
	}
	
	private Set<Long> getSctIdsFromFile(InputStream inputFileStream) throws IOException {
		Set<Long> sctIds = new HashSet<>();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputFileStream, RF2Constants.UTF_8))) {
			String line = null;
			boolean isFirstLine = true;
			while (( line = reader.readLine()) != null) {
				if (isFirstLine) {
					isFirstLine = false;
					continue;
				}
				String[] columnValues = line.split(RF2Constants.COLUMN_SEPARATOR,-1);
//				if (RF2Constants.BOOLEAN_TRUE.equals(columnValues[1])) {
					sctIds.add( new Long(columnValues[0]));
//				}
			}
		} 
		return sctIds;
	}
}

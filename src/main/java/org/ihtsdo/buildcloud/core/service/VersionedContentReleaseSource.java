package org.ihtsdo.buildcloud.core.service;

import org.apache.commons.io.IOUtils;
import org.ihtsdo.otf.dao.s3.S3Client;
import org.ihtsdo.otf.dao.s3.helper.FileHelper;
import org.ihtsdo.otf.utils.ZipFileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.List;

/**
 * Reads previously published release packages from the old versioned content directory
 * written by {@code PublishServiceImpl.uploadToOldVersionedContentDirectory}.
 */
@Component
public class VersionedContentReleaseSource {

	private static final Logger LOGGER = LoggerFactory.getLogger(VersionedContentReleaseSource.class);

	private final FileHelper versionedContentFileHelper;
	private final String versionedContentPath;

	@Autowired
	public VersionedContentReleaseSource(
			@Value("${srs.build.versioned-content.bucketName}") String bucketName,
			@Value("${srs.build.versioned-content.path}") String versionedContentPath,
			S3Client s3Client) {
		this(new FileHelper(bucketName, s3Client), versionedContentPath);
	}

	VersionedContentReleaseSource(FileHelper versionedContentFileHelper, String versionedContentPath) {
		this.versionedContentFileHelper = versionedContentFileHelper;
		this.versionedContentPath = versionedContentPath;
	}

	public boolean exists(String releasePackageFilename) {
		return resolveObjectKey(releasePackageFilename) != null;
	}

	/**
	 * Copies the release zip out of the old versioned content directory and extracts it into a temporary directory.
	 * The caller deletes that directory when the build finishes.
	 */
	public File readExtractedReleaseOrNull(String releasePackageFilename) {
		String objectKey = resolveObjectKey(releasePackageFilename);
		if (objectKey == null) {
			LOGGER.warn("Release package {} could not be found in the versioned content folder", releasePackageFilename);
			return null;
		}

		File releasePackage = null;
		File extractedDirectory = null;
		try (InputStream releaseStream = versionedContentFileHelper.getFileStream(objectKey)) {
			if (releaseStream == null) {
				LOGGER.warn("Release package {} could not be found in the versioned content folder", releasePackageFilename);
				return null;
			}
			releasePackage = Files.createTempFile("previous-release-", ".zip").toFile();
			try (OutputStream out = new FileOutputStream(releasePackage)) {
				IOUtils.copy(releaseStream, out);
			}
			extractedDirectory = Files.createTempDirectory("temp-rf2-unzip").toFile();
			ZipFileUtils.extractFilesFromZipToOneFolder(releasePackage, extractedDirectory.getAbsolutePath());
			LOGGER.info("Previous release {} loaded from the versioned content folder", objectKey);
			File result = extractedDirectory;
			extractedDirectory = null;
			return result;
		} catch (IOException e) {
			LOGGER.error("Error retrieving the release file {} from the versioned content folder: {}", releasePackageFilename, e.getMessage());
			return null;
		} finally {
			deleteQuietly(releasePackage);
			deleteDirectoryQuietly(extractedDirectory);
		}
	}

	private String resolveObjectKey(String releasePackageFilename) {
		if (!StringUtils.hasLength(releasePackageFilename)) {
			return null;
		}
		try {
			String exactKey = objectKey(releasePackageFilename);
			if (versionedContentFileHelper.exists(exactKey)) {
				return exactKey;
			}
			String directory = directory();
			String prefixedSuffix = "_" + releasePackageFilename;
			List<String> matches = versionedContentFileHelper.listFiles(directory).stream()
					.filter(name -> name.endsWith(prefixedSuffix) && !name.contains("/"))
					.toList();
			if (matches.size() == 1) {
				return directory + matches.get(0);
			}
			if (matches.size() > 1) {
				LOGGER.warn("Multiple versioned content packages found for {}: {}", releasePackageFilename, matches);
			}
		} catch (RuntimeException e) {
			LOGGER.error("Failed to look up {} in the versioned content folder: {}", releasePackageFilename, e.getMessage());
		}
		return null;
	}

	private String directory() {
		if (versionedContentPath.endsWith("/")) {
			return versionedContentPath;
		}
		return versionedContentPath + "/";
	}

	private String objectKey(String releaseFileName) {
		return directory() + releaseFileName;
	}

	private static void deleteQuietly(File file) {
		if (file == null) {
			return;
		}
		try {
			Files.deleteIfExists(file.toPath());
		} catch (IOException e) {
			LOGGER.warn("Failed to delete temporary file {}", file.getAbsolutePath());
		}
	}

	private static void deleteDirectoryQuietly(File directory) {
		if (directory == null) {
			return;
		}
		try {
			org.apache.commons.io.FileUtils.deleteDirectory(directory);
		} catch (IOException e) {
			LOGGER.warn("Failed to delete temporary directory {}", directory.getAbsolutePath());
		}
	}
}

package org.ihtsdo.buildcloud.service;

import org.ihtsdo.buildcloud.dao.BuildDAO;
import org.ihtsdo.buildcloud.dao.BuildInputFileDAO;
import org.ihtsdo.buildcloud.dao.helper.ExecutionS3PathHelper;
import org.ihtsdo.buildcloud.dao.helper.FileHelper;
import org.ihtsdo.buildcloud.dao.helper.S3ClientHelper;
import org.ihtsdo.buildcloud.dao.s3.S3Client;
import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.service.exception.ResourceNotFoundException;
import org.ihtsdo.buildcloud.service.execution.RF2Constants;
import org.ihtsdo.buildcloud.service.file.FileUtils;
import org.ihtsdo.buildcloud.service.security.SecurityHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
@Transactional
public class BuildInputFileServiceImpl implements BuildInputFileService {

	public static final Logger LOGGER = LoggerFactory.getLogger(BuildInputFileServiceImpl.class);

	private final FileHelper fileHelper;

	@Autowired
	private BuildDAO buildDAO;

	@Autowired
	private BuildInputFileDAO dao;

	@Autowired
	private ExecutionS3PathHelper s3PathHelper;

	@Autowired
	public BuildInputFileServiceImpl(final String executionBucketName, final S3Client s3Client, final S3ClientHelper s3ClientHelper) {
		fileHelper = new FileHelper(executionBucketName, s3Client, s3ClientHelper);
	}

	@Override
	public void putManifestFile(String centerKey, final String buildKey, final InputStream inputStream, final String originalFilename, final long fileSize) throws ResourceNotFoundException {
		Build build = getBuild(centerKey, buildKey);
		dao.putManifestFile(build, inputStream, originalFilename, fileSize);
	}

	@Override
	public String getManifestFileName(String centerKey, final String buildKey) throws ResourceNotFoundException {
		StringBuilder manifestDirectoryPathSB = s3PathHelper.getBuildManifestDirectoryPath(getBuild(centerKey, buildKey));
		List<String> files = fileHelper.listFiles(manifestDirectoryPathSB.toString());
		if (!files.isEmpty()) {
			return files.iterator().next();
		} else {
			return null;
		}
	}

	@Override
	public InputStream getManifestStream(String centerKey, final String buildKey) throws ResourceNotFoundException {
		return dao.getManifestStream(getBuild(centerKey, buildKey));
	}

	@Override
	public void putInputFile(String centerKey, final String buildKey, final InputStream inputStream, final String filename, final long fileSize) throws ResourceNotFoundException, IOException {
		Build build = getBuild(centerKey, buildKey);

		String buildInputFilesPath = s3PathHelper.getBuildInputFilesPath(build);
		if (filename.endsWith(RF2Constants.ZIP_FILE_EXTENSION)) {
			Path tempFile = Files.createTempFile(getClass().getCanonicalName(), ".zip");
			try (ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {
				ZipEntry entry;
				while ((entry = zipInputStream.getNextEntry()) != null) {
					String fileDestinationPath = buildInputFilesPath + FileUtils.getFilenameFromPath(entry.getName());
					Files.copy(zipInputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
					try (FileInputStream tempFileInputStream = new FileInputStream(tempFile.toFile())) {
						fileHelper.putFile(tempFileInputStream, tempFile.toFile().length(), fileDestinationPath);
					}
				}
			} finally {
				if (!tempFile.toFile().delete()) {
					LOGGER.warn("Failed to delete temp file {}", tempFile.toFile().getAbsolutePath());
				}
			}
		} else {
			String fileDestinationPath = buildInputFilesPath + filename;
			fileHelper.putFile(inputStream, fileSize, fileDestinationPath);
		}
	}

	@Override
	public InputStream getFileInputStream(String centerKey, final String buildKey, final String filename) throws ResourceNotFoundException {
		Build build = getBuild(centerKey, buildKey);
		return getFileInputStream(build, filename);
	}

	@Override
	public List<String> listInputFilePaths(String centerKey, final String buildKey) throws ResourceNotFoundException {
		Build build = getBuild(centerKey, buildKey);
		return dao.listRelativeInputFilePaths(build);
	}

	@Override
	public void deleteFile(String centerKey, final String buildKey, final String filename) throws ResourceNotFoundException {
		Build build = getBuild(centerKey, buildKey);
		fileHelper.deleteFile(s3PathHelper.getBuildInputFilesPath(build) + filename);
	}

	@Override
	public void deleteFilesByPattern(String centerKey, final String buildKey, final String inputFileNamePattern) throws ResourceNotFoundException {

		List<String> filesFound = listInputFilePaths(centerKey, buildKey);

		//Need to convert a standard file wildcard to a regex pattern
		String regexPattern = inputFileNamePattern.replace(".", "\\.").replace("*", ".*");
		Pattern pattern = Pattern.compile(regexPattern);
		for (String inputFileName : filesFound) {
			if (pattern.matcher(inputFileName).matches()) {
				deleteFile(centerKey, buildKey, inputFileName);
			}
		}
	}

	private InputStream getFileInputStream(final Build build, final String filename) {
		return fileHelper.getFileStream(s3PathHelper.getBuildInputFilesPath(build) + filename);
	}

	private Build getBuild(String centerKey,final String buildKey) throws ResourceNotFoundException {
		Build build = buildDAO.find(centerKey, buildKey, SecurityHelper.getRequiredUser());
		if (build == null) {
			throw new ResourceNotFoundException("Unable to find build: " + buildKey);
		}
		return build;
	}

}

package org.ihtsdo.buildcloud.dao;

import org.ihtsdo.buildcloud.dao.helper.ExecutionS3PathHelper;
import org.ihtsdo.buildcloud.dao.helper.FileHelper;
import org.ihtsdo.buildcloud.dao.helper.S3ClientHelper;
import org.ihtsdo.buildcloud.dao.s3.S3Client;
import org.ihtsdo.buildcloud.entity.Build;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.InputStream;
import java.util.List;

public class BuildInputFileDAOImpl implements BuildInputFileDAO {

	private static final Logger LOGGER = LoggerFactory.getLogger(BuildInputFileDAOImpl.class);

	private FileHelper fileHelper;

	@Autowired
	private ExecutionS3PathHelper s3PathHelper;

	@Autowired
	public BuildInputFileDAOImpl(String executionBucketName, S3Client s3Client, S3ClientHelper s3ClientHelper) {
		fileHelper = new FileHelper(executionBucketName, s3Client, s3ClientHelper);
	}

	@Override
	public InputStream getManifestStream(Build build) {
		String manifestPath = getManifestPath(build);
		if (manifestPath != null) {
			return fileHelper.getFileStream(manifestPath);
		} else {
			return null;
		}
	}

	@Override
	public List<String> listRelativeInputFilePaths(Build build) {
		String directoryPath = s3PathHelper.getBuildInputFilesPath(build);
		return fileHelper.listFiles(directoryPath);
	}

	@Override
	public String getManifestPath(Build build) {
		String manifestDirectoryPath = s3PathHelper.getBuildManifestDirectoryPath(build).toString();
		LOGGER.debug("manifestDirectoryPath '{}'", manifestDirectoryPath);
		List<String> files = fileHelper.listFiles(manifestDirectoryPath);
		//The first file in the manifest directory will be the manifest
		if (!files.isEmpty()) {
			return manifestDirectoryPath + files.iterator().next();
		} else {
			return null;
		}
	}

	@Override
	public void putManifestFile(Build build, InputStream inputStream, String originalFilename, long fileSize) {

		// Fist delete any existing manifest files
		deleteManifest(build);

		// Put new manifest file
		String filePath = getKnownManifestPath(build, originalFilename);
		fileHelper.putFile(inputStream, fileSize, filePath);
	}

	@Override
	public void deleteManifest(Build build) {
		StringBuilder manifestDirectoryPathSB = s3PathHelper.getBuildManifestDirectoryPath(build);
		List<String> files = fileHelper.listFiles(manifestDirectoryPathSB.toString());
		for (String file : files) {
			fileHelper.deleteFile(manifestDirectoryPathSB.toString() + file);
		}

	}

	@Override
	public String getKnownManifestPath(Build build, String filename) {
		return s3PathHelper.getBuildManifestDirectoryPath(build).append(filename).toString();
	}

}

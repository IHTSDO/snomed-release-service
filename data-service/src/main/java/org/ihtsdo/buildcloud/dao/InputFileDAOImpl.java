package org.ihtsdo.buildcloud.dao;

import org.ihtsdo.buildcloud.dao.helper.ExecutionS3PathHelper;
import org.ihtsdo.buildcloud.dao.helper.FileHelper;
import org.ihtsdo.buildcloud.dao.helper.S3ClientHelper;
import org.ihtsdo.buildcloud.dao.s3.S3Client;
import org.ihtsdo.buildcloud.entity.Package;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.InputStream;
import java.util.List;

public class InputFileDAOImpl implements InputFileDAO {

	private static final Logger LOGGER = LoggerFactory.getLogger(InputFileDAOImpl.class);

	private FileHelper fileHelper;

	@Autowired
	private ExecutionS3PathHelper s3PathHelper;

	@Autowired
	public InputFileDAOImpl(String executionBucketName, S3Client s3Client, S3ClientHelper s3ClientHelper) {
		fileHelper = new FileHelper(executionBucketName, s3Client, s3ClientHelper);
	}

	@Override
	public InputStream getManifestStream(Package aPackage) {
		String manifestPath = getManifestPath(aPackage);
		if (manifestPath != null) {
			return fileHelper.getFileStream(manifestPath);
		} else {
			return null;
		}
	}

	@Override
	public List<String> listInputFilePaths(Package aPackage) {
		String directoryPath = s3PathHelper.getPackageInputFilesPath(aPackage);
		return fileHelper.listFiles(directoryPath);
	}

	@Override
	//Version called when we're expecting a manifest to exist
	public String getManifestPath(Package aPackage) {
		String manifestDirectoryPath = s3PathHelper.getPackageManifestDirectoryPath(aPackage).toString();
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
	public void putManifestFile(Package pkg, InputStream inputStream, String originalFilename, long fileSize) {

		// Fist delete any existing manifest files
		deleteManifest(pkg);

		// Put new manifest file
		String filePath = getManifestPath(pkg, originalFilename);
		fileHelper.putFile(inputStream, fileSize, filePath);
	}

	@Override
	public void deleteManifest(Package pkg) {
		StringBuffer manifestDirectoryPathSB = s3PathHelper.getPackageManifestDirectoryPath(pkg);
		String directoryPath = manifestDirectoryPathSB.toString();
		List<String> files = fileHelper.listFiles(directoryPath);
		for (String file : files) {
			fileHelper.deleteFile(directoryPath + file);
		}

	}

	@Override
	//Version called when we have a manifest file and we want a path to upload it to
	public String getManifestPath(Package pkg, String filename) {
		return s3PathHelper.getPackageManifestDirectoryPath(pkg).append(filename).toString();
	}

}

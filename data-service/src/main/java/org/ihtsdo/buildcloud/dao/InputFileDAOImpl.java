package org.ihtsdo.buildcloud.dao;

import java.io.InputStream;
import java.util.List;

import org.ihtsdo.buildcloud.dao.helper.ExecutionS3PathHelper;
import org.ihtsdo.buildcloud.dao.helper.FileHelper;
import org.ihtsdo.buildcloud.dao.helper.S3ClientHelper;
import org.ihtsdo.buildcloud.dao.s3.S3Client;
import org.ihtsdo.buildcloud.entity.Package;
import org.springframework.beans.factory.annotation.Autowired;

public class InputFileDAOImpl implements InputFileDAO {

	private FileHelper fileHelper;
	
	@Autowired
	private ExecutionS3PathHelper s3PathHelper;
	
	@Autowired
	public InputFileDAOImpl(String executionBucketName, S3Client s3Client, S3ClientHelper s3ClientHelper) {
		fileHelper = new FileHelper (executionBucketName, s3Client, s3ClientHelper);
	}
	
	public List<String> listInputFilePaths(Package aPackage) {
		String directoryPath = s3PathHelper.getPackageInputFilesPath(aPackage);
		return fileHelper.listFiles(directoryPath);
	}

	@Override
	public InputStream getManifestStream(Package pkg) {
		StringBuffer manifestDirectoryPathSB = s3PathHelper.getPackageManifestDirectoryPath(pkg);

		String directoryPath = manifestDirectoryPathSB.toString();
		List<String> files = fileHelper.listFiles(directoryPath);
		//The first file in the manifest directory we'll call our manifest
		if (!files.isEmpty()) {
			String manifestFilePath = directoryPath + files.iterator().next();
			return fileHelper.getFileStream(manifestFilePath);
		} else {
			return null;
		}
	}
	
}

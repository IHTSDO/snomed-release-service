package org.ihtsdo.buildcloud.dao;

import org.ihtsdo.buildcloud.dao.helper.BuildS3PathHelper;
import org.ihtsdo.buildcloud.dao.helper.FileHelper;
import org.ihtsdo.buildcloud.dao.helper.S3ClientHelper;
import org.ihtsdo.buildcloud.dao.s3.S3Client;
import org.ihtsdo.buildcloud.entity.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.InputStream;
import java.util.List;

public class ProductInputFileDAOImpl implements ProductInputFileDAO {

	private static final Logger LOGGER = LoggerFactory.getLogger(ProductInputFileDAOImpl.class);

	private FileHelper fileHelper;

	@Autowired
	private BuildS3PathHelper s3PathHelper;

	@Autowired
	public ProductInputFileDAOImpl(String buildBucketName, S3Client s3Client, S3ClientHelper s3ClientHelper) {
		fileHelper = new FileHelper(buildBucketName, s3Client, s3ClientHelper);
	}

	@Override
	public InputStream getManifestStream(Product product) {
		String manifestPath = getManifestPath(product);
		if (manifestPath != null) {
			return fileHelper.getFileStream(manifestPath);
		} else {
			return null;
		}
	}

	@Override
	public List<String> listRelativeInputFilePaths(Product product) {
		String directoryPath = s3PathHelper.getProductInputFilesPath(product);
		return fileHelper.listFiles(directoryPath);
	}

	@Override
	public String getManifestPath(Product product) {
		String manifestDirectoryPath = s3PathHelper.getProductManifestDirectoryPath(product).toString();
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
	public void putManifestFile(Product product, InputStream inputStream, String originalFilename, long fileSize) {

		// Fist delete any existing manifest files
		deleteManifest(product);

		// Put new manifest file
		String filePath = getKnownManifestPath(product, originalFilename);
		fileHelper.putFile(inputStream, fileSize, filePath);
	}

	@Override
	public void deleteManifest(Product product) {
		StringBuilder manifestDirectoryPathSB = s3PathHelper.getProductManifestDirectoryPath(product);
		List<String> files = fileHelper.listFiles(manifestDirectoryPathSB.toString());
		for (String file : files) {
			fileHelper.deleteFile(manifestDirectoryPathSB.toString() + file);
		}

	}

	@Override
	public String getKnownManifestPath(Product product, String filename) {
		return s3PathHelper.getProductManifestDirectoryPath(product).append(filename).toString();
	}

}

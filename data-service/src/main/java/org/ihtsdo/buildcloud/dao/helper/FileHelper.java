package org.ihtsdo.buildcloud.dao.helper;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.model.*;
import org.apache.commons.codec.DecoderException;
import org.ihtsdo.buildcloud.dao.s3.S3Client;
import org.ihtsdo.buildcloud.service.file.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

@Repository
public class FileHelper {

	private static final Logger LOGGER = LoggerFactory.getLogger(FileHelper.class);

	private S3Client s3Client;

	public void setS3Client(S3Client s3Client) {
		this.s3Client = s3Client;
	}

	private final S3ClientHelper s3ClientHelper;

	private final String bucketName;

	public FileHelper(String bucketName, S3Client s3Client, S3ClientHelper s3ClientHelper) {
		this.bucketName = bucketName;
		this.s3Client = s3Client;
		this.s3ClientHelper = s3ClientHelper;
	}

	public void putFile(InputStream fileStream, long fileSize, String targetFilePath) {
		S3PutRequestBuilder s3PutRequestBuilder = s3ClientHelper.newPutRequest(bucketName, targetFilePath, fileStream);
		S3PutRequestBuilder length = s3PutRequestBuilder.length(fileSize);
		S3PutRequestBuilder putRequest = length.useBucketAcl();
		s3Client.putObject(putRequest);
	}

	/**
	 * This method causes a warning when using S3 because we don't know the file length up front.
	 * TODO: Investigate multipart upload to avoid the S3 library buffering the whole file.
	 */
	public void putFile(InputStream fileStream, String targetFilePath) {
		S3PutRequestBuilder putRequest = s3ClientHelper.newPutRequest(bucketName, targetFilePath, fileStream).useBucketAcl();
		LOGGER.debug("Putting file to {}/{}", bucketName, targetFilePath);
		s3Client.putObject(putRequest);
	}

	public String putFile(File file, String targetFilePath) throws NoSuchAlgorithmException, IOException, DecoderException {
		return putFile(file, targetFilePath, false);
	}


	public String putFile(File file, String targetFilePath, boolean calcMD5) throws NoSuchAlgorithmException, IOException, DecoderException {

		InputStream is = new FileInputStream(file);
		S3PutRequestBuilder putRequest = s3ClientHelper.newPutRequest(bucketName, targetFilePath, is).length(file.length()).useBucketAcl();
		String localMd5 = null;
		if (calcMD5) {
			localMd5 = FileUtils.calculateMD5(file);
			putRequest.withMD5(localMd5);
		}
		PutObjectResult putResult = s3Client.putObject(putRequest);
		String md5Received = (putResult == null ? null : putResult.getContentMd5());
		LOGGER.debug("S3Client put request returned MD5: " + md5Received);

		if (calcMD5) {
			//Also upload the hex encoded (ie normal) md5 digest in a file
			String md5TargetPath = targetFilePath + ".md5";
			File md5File = FileUtils.createMD5File(file, localMd5);
			InputStream isMD5 = new FileInputStream(md5File);
			S3PutRequestBuilder md5PutRequest = s3ClientHelper.newPutRequest(bucketName, md5TargetPath, isMD5).length(md5File.length()).useBucketAcl();
			s3Client.putObject(md5PutRequest);
		}

		return md5Received;
	}

	public InputStream getFileStream(String filePath) {
		try {
			S3Object s3Object = s3Client.getObject(bucketName, filePath);
			if (s3Object != null) {
				return s3Object.getObjectContent();
			}
		} catch (AmazonS3Exception e) {
			if (404 != e.getStatusCode()) {
				throw e;
			}
		}
		return null;
	}

	public List<String> listFiles(String directoryPath) {
		ArrayList<String> files = new ArrayList<>();
		try {
			ObjectListing objectListing = s3Client.listObjects(bucketName, directoryPath);
			for (S3ObjectSummary summary : objectListing.getObjectSummaries()) {
				files.add(summary.getKey().substring(directoryPath.length()));
			}
		} catch (AmazonServiceException e) {
			//Trying to list files in a directory that doesn't exist isn't a problem, we'll just return an empty array
			LOGGER.debug("Probable attempt to get listing on non-existent directory: {} error {}", directoryPath, e.getLocalizedMessage());
		}
		return files;
	}

	// TODO: User logging against file actions?
	public void deleteFile(String filePath) {
		s3Client.deleteObject(bucketName, filePath);
	}

	public void copyFile(String sourcePath, String targetPath) {
		LOGGER.debug("Copy file '{}' to '{}'", sourcePath, targetPath);
		s3Client.copyObject(bucketName, sourcePath, bucketName, targetPath);
	}


	/**
	 * @param sourcePath   source path
	 * @param targetBucket target bucket name
	 * @param targetPath   target path
	 */
	public void copyFile(String sourcePath, String targetBucket, String targetPath) {
		LOGGER.debug("Copy file '{}' to  bucket '{}' as file name'{}'", sourcePath, targetBucket, targetPath);
		s3Client.copyObject(bucketName, sourcePath, targetBucket, targetPath);
	}

	/**
	 * @param targetFilePath
	 * @return true if the target file actually exists in the fileStore (online or offline)
	 */
	public boolean exists(String targetFilePath) {
		try {
			s3Client.getObjectMetadata(bucketName, targetFilePath);
			return true; // No 404 exception .. object exists
		} catch (AmazonS3Exception e) {
			if (e.getStatusCode() == 404) {
				return false;
			} else {
				throw e;
			}
		}
	}

}

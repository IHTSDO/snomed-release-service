package org.ihtsdo.buildcloud.dao;

import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.digest.DigestUtils;
import org.ihtsdo.buildcloud.dao.helper.S3ClientHelper;
import org.ihtsdo.buildcloud.dao.helper.S3PutRequestBuilder;
import org.ihtsdo.buildcloud.dao.s3.S3Client;
import org.ihtsdo.buildcloud.service.FileServiceImpl;
import org.ihtsdo.buildcloud.service.file.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.stereotype.Repository;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

@Repository
public class FileDAOImpl implements FileDAO {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(FileDAOImpl.class);

	@Autowired
	private S3Client s3Client;

	@Autowired
	private S3ClientHelper s3ClientHelper;

	private String executionS3BucketName;

	@Override
	public void putFile(InputStream fileStream, long fileSize, String targetFilePath) {
		S3PutRequestBuilder putRequest = s3ClientHelper.newPutRequest(executionS3BucketName, targetFilePath, fileStream).length(fileSize).useBucketAcl();
		s3Client.putObject(putRequest);
	}

	@Override
	/**
	 * This method causes a warning when using S3 because we don't know the file length up front.
	 * TODO: Investigate multipart upload to avoid the S3 library buffering the whole file.
	 */
	public void putFile(InputStream fileStream, String targetFilePath) {
		S3PutRequestBuilder putRequest = s3ClientHelper.newPutRequest(executionS3BucketName, targetFilePath, fileStream).useBucketAcl();
		s3Client.putObject(putRequest);
	}
	
	@Override
	public String putFile(File file, String targetFilePath, boolean calcMD5) throws NoSuchAlgorithmException, IOException, DecoderException {

		InputStream is = new FileInputStream (file);
		S3PutRequestBuilder putRequest = s3ClientHelper.newPutRequest(executionS3BucketName, targetFilePath, is).length(file.length()).useBucketAcl();
		if (calcMD5){
			String md5 = FileUtils.calculateMD5(file);
			putRequest.withMD5(md5);
		}		
		PutObjectResult putResult = s3Client.putObject(putRequest);
		String md5Received = (putResult == null ? null : putResult.getContentMd5());
		LOGGER.debug ("S3Client put request returned MD5: " + md5Received);
		return md5Received;
	}

	@Override
	public InputStream getFileStream(String filePath) {

		try {
			S3Object s3Object = s3Client.getObject(executionS3BucketName, filePath);
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

	@Override
	public List<String> listFiles(String directoryPath) {
		ObjectListing objectListing = s3Client.listObjects(executionS3BucketName, directoryPath);
		ArrayList<String> files = new ArrayList<>();
		for (S3ObjectSummary summary : objectListing.getObjectSummaries()) {
			files.add(summary.getKey().substring(directoryPath.length()));
		}
		return files;
	}

	@Override
	// TODO: User logging against file actions?
	public void deleteFile(String filePath) {
		s3Client.deleteObject(executionS3BucketName, filePath);
	}

	@Override
	public void copyFile(String sourcePath, String targetPath) {
		s3Client.copyObject(executionS3BucketName, sourcePath, executionS3BucketName, targetPath);
	}

	@Required
	public void setExecutionS3BucketName(String executionS3BucketName) {
		this.executionS3BucketName = executionS3BucketName;
	}

}

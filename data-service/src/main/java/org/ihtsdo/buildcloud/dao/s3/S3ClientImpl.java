package org.ihtsdo.buildcloud.dao.s3;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.google.common.io.Files;

public class S3ClientImpl extends AmazonS3Client implements S3Client {

	public S3ClientImpl(AWSCredentials awsCredentials) {
		super(awsCredentials);
	}

	public PutObjectResult putObject(String bucketName, String key, InputStream input, ObjectMetadata metadata)
			throws AmazonClientException, AmazonServiceException {
		// Memory problems with large files necessitate writing to disk before
		// uploading to AWS
		File cachedFile = cacheLocally(input, key);
		PutObjectRequest putRequest = new PutObjectRequest(bucketName, key, cachedFile);
		putRequest.setMetadata(metadata);
		PutObjectResult result = super.putObject(putRequest);
		cachedFile.delete();
		return result;
	}

	public PutObjectResult putObject(PutObjectRequest putRequest) throws AmazonClientException, AmazonServiceException {
		// If we have an inputstream, modify the putRequest with a file of known size instead
		if (putRequest.getFile() == null && putRequest.getInputStream() != null) {
			File cachedFile = cacheLocally(putRequest.getInputStream(), putRequest.getKey());
			putRequest.setInputStream(null);
			putRequest.setFile(cachedFile);
			PutObjectResult result = super.putObject(putRequest);
			cachedFile.delete();
			return result;
		}
		return super.putObject(putRequest);
	}

	private File cacheLocally(InputStream inputStream, String key) throws AmazonClientException {
		try {
			File cachedFile = File.createTempFile(key, ".cached");
			FileUtils.copyInputStreamToFile(inputStream, cachedFile);
			return cachedFile; // Make sure this gets deleted after use!
		} catch (IOException e) {
			throw new AmazonClientException("Failed to cache input stream locally", e);
		}
	}

}

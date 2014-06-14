package org.ihtsdo.buildcloud.dao.s3;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.model.*;

import java.io.File;
import java.io.InputStream;

public interface S3Client {

	ObjectListing listObjects(String bucketName, String prefix) throws AmazonClientException, AmazonServiceException;

	ObjectListing listObjects(ListObjectsRequest listObjectsRequest) throws AmazonClientException, AmazonServiceException;

	S3Object getObject(String bucketName, String key) throws AmazonClientException, AmazonServiceException;

	PutObjectResult putObject(String bucketName, String key, File file) throws AmazonClientException, AmazonServiceException;

	PutObjectResult putObject(String bucketName, String key, InputStream input, ObjectMetadata metadata) throws AmazonClientException, AmazonServiceException;

	PutObjectResult putObject(PutObjectRequest putRequest) throws AmazonClientException, AmazonServiceException;

	CopyObjectResult copyObject(String sourceBucketName, String sourceKey, String destinationBucketName, String destinationKey) throws AmazonClientException, AmazonServiceException;

	void deleteObject(String bucketName, String key) throws AmazonClientException, AmazonServiceException;

	AccessControlList getBucketAcl(String bucketName);

}

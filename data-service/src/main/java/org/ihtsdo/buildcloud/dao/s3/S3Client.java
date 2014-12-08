package org.ihtsdo.buildcloud.dao.s3;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.model.*;

import java.io.File;
import java.io.InputStream;

public interface S3Client {

	ObjectListing listObjects(String bucketName, String prefix) throws AmazonClientException;

	ObjectListing listObjects(ListObjectsRequest listObjectsRequest) throws AmazonClientException;

	S3Object getObject(String bucketName, String key) throws AmazonClientException;

	PutObjectResult putObject(String bucketName, String key, File file) throws AmazonClientException;

	PutObjectResult putObject(String bucketName, String key, InputStream input, ObjectMetadata metadata) throws AmazonClientException;

	PutObjectResult putObject(PutObjectRequest putRequest) throws AmazonClientException;

	CopyObjectResult copyObject(String sourceBucketName, String sourceKey, String destinationBucketName, String destinationKey) throws AmazonClientException;

	void deleteObject(String bucketName, String key) throws AmazonClientException;

	AccessControlList getBucketAcl(String bucketName);

	ObjectMetadata getObjectMetadata(String bucketName, String key) throws AmazonClientException;

	ObjectListing listNextBatchOfObjects(ObjectListing objectListing);

}

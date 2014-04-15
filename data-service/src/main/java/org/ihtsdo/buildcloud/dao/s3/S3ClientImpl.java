package org.ihtsdo.buildcloud.dao.s3;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;

public class S3ClientImpl extends AmazonS3Client implements S3Client {

	public S3ClientImpl(AWSCredentials awsCredentials) {
		super(awsCredentials);
	}

}

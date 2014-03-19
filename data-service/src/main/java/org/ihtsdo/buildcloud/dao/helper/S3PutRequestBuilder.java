package org.ihtsdo.buildcloud.dao.helper;

import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;

import java.io.InputStream;

/**
 * Wraps the JetS3t ObjectMetadata enabling builder pattern.
 */
public class S3PutRequestBuilder extends PutObjectRequest {

	private S3ClientHelper helper;

	protected S3PutRequestBuilder(String bucketName, String key, InputStream input, S3ClientHelper helper) {
		super(bucketName, key, input, new ObjectMetadata());
		this.helper = helper;
	}

	public S3PutRequestBuilder length(long contentLength) {
		this.getMetadata().setContentLength(contentLength);
		return this;
	}

	public S3PutRequestBuilder useBucketAcl() {
		helper.useBucketAcl(this);
		return this;
	}

	public void setHelper(S3ClientHelper helper) {
		this.helper = helper;
	}
}

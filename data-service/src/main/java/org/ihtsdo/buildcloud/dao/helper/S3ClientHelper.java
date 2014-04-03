package org.ihtsdo.buildcloud.dao.helper;

import com.amazonaws.services.s3.model.AccessControlList;
import com.amazonaws.services.s3.model.PutObjectRequest;
import org.ihtsdo.buildcloud.dao.s3.S3Client;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class S3ClientHelper {

	@Autowired
	private S3Client s3Client;

	private Map<String, AccessControlList> bucketAclCache;

	public S3ClientHelper() {
		bucketAclCache = new HashMap<>();
	}

	public void useBucketAcl(PutObjectRequest putObjectRequest) {
		String bucketName = putObjectRequest.getBucketName();
		AccessControlList bucketAcl = getAccessControlList(bucketName);
		putObjectRequest.setAccessControlList(bucketAcl);
	}

	private AccessControlList getAccessControlList(String bucketName) {
		if (!bucketAclCache.containsKey(bucketName)) {
			// Synchronization omitted for simplicity
			AccessControlList bucketAcl = s3Client.getBucketAcl(bucketName);
			bucketAclCache.put(bucketName, bucketAcl);
		}
		return bucketAclCache.get(bucketName);
	}

	public S3PutRequestBuilder newPutRequest(String bucketName, String key, InputStream input) {
		return new S3PutRequestBuilder(bucketName, key, input, this);
	}

}

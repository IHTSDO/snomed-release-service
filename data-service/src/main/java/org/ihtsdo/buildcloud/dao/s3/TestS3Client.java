package org.ihtsdo.buildcloud.dao.s3;

import java.io.IOException;

public interface TestS3Client {

	void freshBucketStore() throws IOException;

	void createBucket(String bucketName);

}

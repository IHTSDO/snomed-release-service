package org.ihtsdo.buildcloud.dao.s3;

import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class OfflineS3Object extends S3Object {

	private File contents;

	public OfflineS3Object(String bucketName, String key, File contents) {
		setBucketName(bucketName);
		setKey(key);
		this.contents = contents;
	}

	@Override
	public S3ObjectInputStream getObjectContent() {
		try {
			return new S3ObjectInputStream(new FileInputStream(contents), null);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}
}

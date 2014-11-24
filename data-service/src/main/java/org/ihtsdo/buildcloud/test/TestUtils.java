package org.ihtsdo.buildcloud.test;

import org.ihtsdo.buildcloud.dao.helper.BuildS3PathHelper;
import org.ihtsdo.buildcloud.dao.s3.S3Client;
import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.User;
import org.ihtsdo.buildcloud.service.security.SecurityHelper;
import org.springframework.beans.factory.annotation.Autowired;

public class TestUtils {

	public static final User TEST_USER = new User(1L, "test");

	@Autowired
	private BuildS3PathHelper pathHelper;

	@Autowired
	private S3Client s3Client;

	@Autowired
	private String buildBucketName;

	/**
	 * Deletes the entire structure under an build.  To ONLY BE USED with unit tests,
	 * as in the normal course of events we expect build files to be immutable
	 *
	 * @param build
	 */
	public void scrubBuild(Build build) {
		String directoryPath = pathHelper.getBuildPath(build).toString();
		try {
			s3Client.deleteObject(buildBucketName, directoryPath);
		} catch (Exception e) {
		}  //That's fine if the thing to delete doesn't exist.
	}

	public static void setTestUser() {
		SecurityHelper.setUser(TEST_USER);
	}
}

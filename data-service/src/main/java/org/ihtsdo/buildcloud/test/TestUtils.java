package org.ihtsdo.buildcloud.test;

import org.ihtsdo.buildcloud.dao.helper.ExecutionS3PathHelper;
import org.ihtsdo.buildcloud.dao.s3.S3Client;
import org.ihtsdo.buildcloud.entity.Execution;
import org.ihtsdo.buildcloud.entity.User;
import org.ihtsdo.buildcloud.service.security.SecurityHelper;
import org.springframework.beans.factory.annotation.Autowired;

public class TestUtils {

	public static final User TEST_USER = new User(1L, "test");

	@Autowired
	private ExecutionS3PathHelper pathHelper;

	@Autowired
	private S3Client s3Client;

	@Autowired
	private String executionBucketName;

	/**
	 * Deletes the entire structure under an execution.  To ONLY BE USED with unit tests,
	 * as in the normal course of events we expect execution files to be immutable
	 *
	 * @param execution
	 */
	public void scrubExecution(Execution execution) {
		String directoryPath = pathHelper.getExecutionPath(execution).toString();
		try {
			s3Client.deleteObject(executionBucketName, directoryPath);
		} catch (Exception e) {
		}  //That's fine if the thing to delete doesn't exist.
	}

	public static void setTestUser() {
		SecurityHelper.setUser(TEST_USER);
	}
}

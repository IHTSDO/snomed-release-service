package org.ihtsdo.buildcloud.test;

import org.ihtsdo.buildcloud.dao.DAOFactory;
import org.ihtsdo.buildcloud.dao.helper.ExecutionS3PathHelper;
import org.ihtsdo.buildcloud.entity.Execution;

public class TestUtils {

	/**
	 * Deletes the entire structure under an execution.  To ONLY BE USED with unit tests, 
	 * as in the normal course of events we expect execution files to be immutable
	 * @param execution
	 */
	public static void scrubExecution (Execution execution) {
		ExecutionS3PathHelper pathHelper = DAOFactory.getExecutionPathHelper();
		String directoryPath = pathHelper.getExecutionPath(execution).toString();
		try {
			DAOFactory.getS3Client().deleteObject(DAOFactory.getExecutionBucketName(), directoryPath);
		} catch (Exception e) {}  //That's fine if the thing to delete doesn't exist.
	}
}

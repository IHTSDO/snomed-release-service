package org.ihtsdo.buildcloud.dao.helper;

import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.Execution;

public class ExecutionS3PathHelper {

	public static final String SEPARATOR = "/";
	private static final String CONFIG_JSON = "configuration.json";
	private static final String STATUS_PREFIX = "status:";

	public StringBuffer getBuildPath(Build build) {
		String releaseCentreBusinessKey = build.getProduct().getExtension().getReleaseCentre().getBusinessKey();
		StringBuffer path = new StringBuffer();
		path.append(releaseCentreBusinessKey);
		path.append(SEPARATOR);
		path.append(build.getCompositeKey());
		path.append(SEPARATOR);
		return path;
	}

	public StringBuffer getExecutionPath(Execution execution) {
		return getExecutionPath(execution.getBuild(), execution.getId());
	}

	public StringBuffer getExecutionPath(Build build, String executionId) {
		StringBuffer path = getBuildPath(build);
		path.append(executionId);
		path.append(SEPARATOR);
		return path;
	}

	public StringBuffer getBuildScriptsPath(Execution execution) {
		return getExecutionPath(execution).append("build-scripts").append(SEPARATOR);
	}

	public String getConfigFilePath(Execution execution) {
		return getFilePath(execution, CONFIG_JSON);
	}

	public String getStatusFilePath(Execution execution, String status) {
		return getExecutionPath(execution).append(STATUS_PREFIX).append(status).toString();
	}

	private String getFilePath(Execution execution, String relativePath) {
		return getExecutionPath(execution).append(relativePath).toString();
	}
}

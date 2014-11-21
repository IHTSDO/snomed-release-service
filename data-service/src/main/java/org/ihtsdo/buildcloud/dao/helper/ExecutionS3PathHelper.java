package org.ihtsdo.buildcloud.dao.helper;

import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.Execution;
import org.ihtsdo.buildcloud.entity.ReleaseCenter;

public class ExecutionS3PathHelper {

	public static final String SEPARATOR = "/";
	private static final String CONFIG_JSON = "configuration.json";
	private static final String STATUS_PREFIX = "status:";
	private static final String OUTPUT_FILES = "output-files";
	private static final String INPUT_FILES = "input-files";
	private static final String BUILD_FILES = "build-files";
	private static final String MANIFEST = "manifest";
	private static final String TRANSFORMED_FILES = "transformed-files";
	public static final String LOG = "log";
	public static final String EXECUTION_LOG_TXT = "execution_log.txt";

	public StringBuilder getBuildPath(Build build) {
		return getReleaseCenterPath(build.getReleaseCenter()).append(build.getBusinessKey()).append(SEPARATOR);
	}

	public StringBuilder getReleaseCenterPath(ReleaseCenter releaseCenter) {
		StringBuilder path = new StringBuilder();
		path.append(releaseCenter.getBusinessKey());
		path.append(SEPARATOR);
		return path;
	}

	public StringBuilder getBuildManifestDirectoryPath(Build build) {
		return getBuildPath(build).append(BUILD_FILES).append(SEPARATOR).append(MANIFEST).append(SEPARATOR);
	}

	public StringBuilder getExecutionInputFilesPath(Execution execution) {
		return getExecutionPath(execution.getBuild(), execution.getId()).append(INPUT_FILES).append(SEPARATOR);
	}

	public String getExecutionInputFilePath(Execution execution, String inputFile) {
		return getExecutionInputFilesPath(execution).append(inputFile).toString();
	}

	public StringBuilder getExecutionOutputFilesPath(Execution execution) {
		return getExecutionPath(execution.getBuild(), execution.getId()).append(OUTPUT_FILES).append(SEPARATOR);
	}

	public String getExecutionOutputFilePath(Execution execution, String relativeFilePath) {
		return getExecutionOutputFilesPath(execution).append(relativeFilePath).toString();
	}

	public String getExecutionLogFilePath(Execution execution, String relativeFilePath) {
		return getExecutionLogFilesPath(execution).append(relativeFilePath).toString();
	}

	public StringBuilder getExecutionLogFilesPath(Execution execution) {
		return getExecutionPath(execution.getBuild(), execution.getId()).append(LOG).append(SEPARATOR);
	}

	public String getMainExecutionLogFilePath(Execution execution) {
		return getExecutionLogFilesPath(execution).append(EXECUTION_LOG_TXT).toString();
	}

	public StringBuilder getExecutionPath(Execution execution) {
		return getExecutionPath(execution.getBuild(), execution.getId());
	}

	public StringBuilder getExecutionPath(Build build, String executionId) {
		return getBuildPath(build).append(executionId).append(SEPARATOR);
	}

	public String getConfigFilePath(Execution execution) {
		return getFilePath(execution, CONFIG_JSON);
	}

	public String getStatusFilePath(Execution execution, Execution.Status status) {
		return getExecutionPath(execution).append(STATUS_PREFIX).append(status.toString()).toString();
	}

	public String getOutputFilesPath(Execution execution) {
		return getExecutionPath(execution).append("output-files").append(SEPARATOR).toString();
	}

	private String getFilePath(Execution execution, String relativePath) {
		return getExecutionPath(execution).append(relativePath).toString();
	}

	public StringBuilder getExecutionTransformedFilesPath(Execution execution) {
		return getExecutionPath(execution.getBuild(), execution.getId()).append(TRANSFORMED_FILES).append(SEPARATOR);
	}

	public String getTransformedFilePath(Execution execution, String relativeFilePath) {
		return getExecutionTransformedFilesPath(execution).append(relativeFilePath).toString();
	}

	public String getPublishedFilePath(ReleaseCenter releaseCenter, String publishedFileName) {
		return getReleaseCenterPath(releaseCenter).append(publishedFileName).toString();
	}

	public String getReportPath(Execution execution) {
		return getExecutionPath(execution.getBuild(), execution.getId()).append("execution_report.json").toString();
	}

	public String getBuildInputFilesPath(Build build) {
		return getBuildPath(build).append(BUILD_FILES).append(SEPARATOR).append(INPUT_FILES).append(SEPARATOR).toString();
	}

	public String getExecutionManifestDirectoryPath(Execution execution) {
		return getExecutionPath(execution).append(MANIFEST).append(SEPARATOR).toString();
	}

}

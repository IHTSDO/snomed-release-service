package org.ihtsdo.buildcloud.dao.helper;

import org.ihtsdo.buildcloud.entity.*;
import org.ihtsdo.buildcloud.entity.Package;

public class ExecutionS3PathHelper {

	public static final String SEPARATOR = "/";
	private static final String CONFIG_JSON = "configuration.json";
	private static final String STATUS_PREFIX = "status:";
	private static final String OUTPUT = "output/";
	private static final String INPUT_FILES = "input-files";
	private static final String BUILD_FILES = "build-files";
	private static final String MANIFEST = "manifest";

	public StringBuffer getBuildPath(Build build) {
		String releaseCenterBusinessKey = build.getProduct().getExtension().getReleaseCenter().getBusinessKey();
		StringBuffer path = new StringBuffer();
		path.append(releaseCenterBusinessKey);
		path.append(SEPARATOR);
		path.append(build.getCompositeKey());
		path.append(SEPARATOR);
		return path;
	}

	public String getPackageInputFilesPath(Package aPackage) {
		return getPackageInputFilesPathAsStringBuffer(aPackage).toString();
	}

	public String getPackageInputFilePath(Package aPackage, String filename) {
		return getPackageInputFilesPathAsStringBuffer(aPackage).append(filename).toString();
	}

	public StringBuffer getPackageManifestDirectoryPathPath(Package aPackage) {
		StringBuffer buildPath = getBuildPath(aPackage.getBuild());
		buildPath.append(BUILD_FILES).append(SEPARATOR).append(MANIFEST).append(SEPARATOR);
		return buildPath;
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

	public String getStatusFilePath(Execution execution, Execution.Status status) {
		return getExecutionPath(execution).append(STATUS_PREFIX).append(status.toString()).toString();
	}
	
	public String getOutputPath(Execution execution, String outputRelativePath) {
		return getFilePath(execution, OUTPUT);
	}

	public String getOutputFilePath(Execution execution, String outputRelativePath) {
		return getFilePath(execution, OUTPUT + outputRelativePath);
	}

	private StringBuffer getPackageInputFilesPathAsStringBuffer(Package aPackage) {
		StringBuffer buildPath = getBuildPath(aPackage.getBuild());
		buildPath.append(BUILD_FILES).append(SEPARATOR).append(INPUT_FILES).append(SEPARATOR).append(aPackage.getBusinessKey()).append(SEPARATOR);
		return buildPath;
	}

	private String getFilePath(Execution execution, String relativePath) {
		return getExecutionPath(execution).append(relativePath).toString();
	}
}

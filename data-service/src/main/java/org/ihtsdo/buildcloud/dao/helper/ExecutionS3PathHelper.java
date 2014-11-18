package org.ihtsdo.buildcloud.dao.helper;

import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.Execution;
import org.ihtsdo.buildcloud.entity.Package;
import org.ihtsdo.buildcloud.entity.Product;

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
		String releaseCenterBusinessKey = build.getProduct().getExtension().getReleaseCenter().getBusinessKey();
		StringBuilder path = new StringBuilder();
		path.append(releaseCenterBusinessKey);
		path.append(SEPARATOR);
		path.append(build.getCompositeKey());
		path.append(SEPARATOR);
		return path;
	}

	public StringBuilder getProductPath(Product product) {
		StringBuilder path = new StringBuilder();
		path.append(product.getExtension().getReleaseCenter().getBusinessKey());
		path.append(SEPARATOR);
		path.append(product.getExtension().getBusinessKey());
		path.append(SEPARATOR);
		path.append(product.getBusinessKey());
		path.append(SEPARATOR);
		return path;
	}

	public String getPackageInputFilesPath(Package aPackage) {
		return getPackageFilesPathAsStringBuilder(aPackage, INPUT_FILES).toString();
	}

	public String getPackageInputFilePath(Package aPackage, String filename) {
		return getPackageFilesPathAsStringBuilder(aPackage, INPUT_FILES).append(filename).toString();
	}

	//Note that getPackageOutputFilePath(Package aPackage, String filename) should not exist because all output 
	//files are specific to the execution that creates them.  See getExecutionOutputFilePath instead.

	public StringBuilder getPackageManifestDirectoryPath(Package aPackage) {
		StringBuilder buildPath = getBuildPath(aPackage.getBuild());
		buildPath.append(BUILD_FILES).append(SEPARATOR).append(aPackage.getBusinessKey()).append(SEPARATOR).append(MANIFEST).append(SEPARATOR);
		return buildPath;
	}

	//There will also be a copy of the manifest for each execution's package
	public StringBuilder getExecutionManifestDirectoryPath(Execution execution, Package pkg) {
		return getExecutionPath(execution.getBuild(), execution.getId()).append(pkg.getBusinessKey()).append(SEPARATOR).append(MANIFEST).append(SEPARATOR);
	}

	public StringBuilder getExecutionPath(Execution execution) {
		return getExecutionPath(execution.getBuild(), execution.getId());
	}

	public StringBuilder getExecutionInputFilesPath(Execution execution, Package aPackage) {
		String businessKey = aPackage.getBusinessKey();
		return getExecutionInputFilesPath(execution, businessKey);
	}

	public StringBuilder getExecutionInputFilesPath(Execution execution, String packageBusinessKey) {
		return getExecutionPath(execution.getBuild(), execution.getId()).append(packageBusinessKey).append(SEPARATOR).append(INPUT_FILES).append(SEPARATOR);
	}

	public String getExecutionInputFilePath(Execution execution, String packageBusinessKey, String inputFile) {
		return getExecutionInputFilesPath(execution, packageBusinessKey).append(inputFile).toString();
	}

	public StringBuilder getExecutionOutputFilesPath(Execution execution, String packageBusinessKey) {
		return getExecutionPath(execution.getBuild(), execution.getId()).append(packageBusinessKey).append(SEPARATOR).append(OUTPUT_FILES).append(SEPARATOR);
	}

	public String getExecutionOutputFilePath(Execution execution, String packageBusinessKey, String relativeFilePath) {
		return getExecutionOutputFilesPath(execution, packageBusinessKey).append(relativeFilePath).toString();
	}

	public StringBuilder getExecutionPackageLogFilesPath(Execution execution, String packageBusinessKey) {
		return getExecutionPath(execution.getBuild(), execution.getId()).append(packageBusinessKey).append(SEPARATOR).append(LOG).append(SEPARATOR);
	}

	public String getExecutionLogFilePath(Execution execution, String packageBusinessKey, String relativeFilePath) {
		return getExecutionPackageLogFilesPath(execution, packageBusinessKey).append(relativeFilePath).toString();
	}

	public String getExecutionLogFilePath(Execution execution, String relativeFilePath) {
		return getExecutionLogFilesPath(execution).append(relativeFilePath).toString();
	}

	public StringBuilder getExecutionLogFilesPath(Execution execution) {
		return getExecutionPath(execution.getBuild(), execution.getId()).append(LOG).append(SEPARATOR);
	}

	public String getExecutionLogFilePath(Execution execution) {
		return getExecutionLogFilesPath(execution).append(EXECUTION_LOG_TXT).toString();
	}

	public StringBuilder getExecutionPath(Build build, String executionId) {
		StringBuilder path = getBuildPath(build);
		path.append(executionId);
		path.append(SEPARATOR);
		return path;
	}

	public String getConfigFilePath(Execution execution) {
		return getFilePath(execution, CONFIG_JSON);
	}

	public String getStatusFilePath(Execution execution, Execution.Status status) {
		return getExecutionPath(execution).append(STATUS_PREFIX).append(status.toString()).toString();
	}

	public String getOutputFilesPath(Execution execution, String packageId) {
		return getExecutionPath(execution).append(packageId).append(SEPARATOR).append("output-files").append(SEPARATOR).toString();
	}

	private StringBuilder getPackageFilesPathAsStringBuilder(Package aPackage, String directionModifier) {
		StringBuilder buildPath = getBuildPath(aPackage.getBuild());
		buildPath.append(BUILD_FILES).append(SEPARATOR).append(aPackage.getBusinessKey()).append(SEPARATOR).append(directionModifier).append(SEPARATOR);
		return buildPath;
	}

	private String getFilePath(Execution execution, String relativePath) {
		return getExecutionPath(execution).append(relativePath).toString();
	}

	public StringBuilder getExecutionTransformedFilesPath(Execution execution, String packageBusinessKey) {
		return getExecutionPath(execution.getBuild(), execution.getId()).append(packageBusinessKey).append(SEPARATOR).append(TRANSFORMED_FILES).append(SEPARATOR);
	}

	public String getTransformedFilePath(Execution execution,
			String packageBusinessKey, String relativeFilePath) {
		return getExecutionTransformedFilesPath(execution, packageBusinessKey).append(relativeFilePath).toString();
	}

	public String getPublishedFilePath(Product product, String publishedFileName) {
		StringBuilder productPath = getProductPath(product);
		productPath.append(publishedFileName);
		return productPath.toString();
	}

	public String getReportPath(Execution execution) {
		return getExecutionPath(execution.getBuild(), execution.getId()).append("execution_report.json").toString();
	}

}

package org.ihtsdo.buildcloud.dao.helper;

import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.Product;
import org.ihtsdo.buildcloud.entity.ReleaseCenter;

public class BuildS3PathHelper {

	public static final String SEPARATOR = "/";
	private static final String CONFIG_JSON = "configuration.json";
	private static final String STATUS_PREFIX = "status:";
	private static final String OUTPUT_FILES = "output-files";
	private static final String INPUT_FILES = "input-files";
	private static final String BUILD_FILES = "product-files";
	private static final String MANIFEST = "manifest";
	private static final String TRANSFORMED_FILES = "transformed-files";
	public static final String LOG = "log";
	public static final String BUILD_LOG_TXT = "build_log.txt";

	public StringBuilder getProductPath(Product product) {
		return getReleaseCenterPath(product.getReleaseCenter()).append(product.getBusinessKey()).append(SEPARATOR);
	}

	public StringBuilder getReleaseCenterPath(ReleaseCenter releaseCenter) {
		StringBuilder path = new StringBuilder();
		path.append(releaseCenter.getBusinessKey());
		path.append(SEPARATOR);
		return path;
	}

	public StringBuilder getProductManifestDirectoryPath(Product product) {
		return getProductPath(product).append(BUILD_FILES).append(SEPARATOR).append(MANIFEST).append(SEPARATOR);
	}

	public StringBuilder getBuildInputFilesPath(Build build) {
		return getBuildPath(build.getProduct(), build.getId()).append(INPUT_FILES).append(SEPARATOR);
	}

	public String getBuildInputFilePath(Build build, String inputFile) {
		return getBuildInputFilesPath(build).append(inputFile).toString();
	}

	public StringBuilder getBuildOutputFilesPath(Build build) {
		return getBuildPath(build.getProduct(), build.getId()).append(OUTPUT_FILES).append(SEPARATOR);
	}

	public String getBuildOutputFilePath(Build build, String relativeFilePath) {
		return getBuildOutputFilesPath(build).append(relativeFilePath).toString();
	}

	public String getBuildLogFilePath(Build build, String relativeFilePath) {
		return getBuildLogFilesPath(build).append(relativeFilePath).toString();
	}

	public StringBuilder getBuildLogFilesPath(Build build) {
		return getBuildPath(build.getProduct(), build.getId()).append(LOG).append(SEPARATOR);
	}

	public String getMainBuildLogFilePath(Build build) {
		return getBuildLogFilesPath(build).append(BUILD_LOG_TXT).toString();
	}

	public StringBuilder getBuildPath(Build build) {
		return getBuildPath(build.getProduct(), build.getId());
	}

	public StringBuilder getBuildPath(Product product, String buildId) {
		return getProductPath(product).append(buildId).append(SEPARATOR);
	}

	public String getConfigFilePath(Build build) {
		return getFilePath(build, CONFIG_JSON);
	}

	public String getStatusFilePath(Build build, Build.Status status) {
		return getBuildPath(build).append(STATUS_PREFIX).append(status.toString()).toString();
	}

	public String getOutputFilesPath(Build build) {
		return getBuildPath(build).append("output-files").append(SEPARATOR).toString();
	}

	private String getFilePath(Build build, String relativePath) {
		return getBuildPath(build).append(relativePath).toString();
	}

	public StringBuilder getBuildTransformedFilesPath(Build build) {
		return getBuildPath(build.getProduct(), build.getId()).append(TRANSFORMED_FILES).append(SEPARATOR);
	}

	public String getTransformedFilePath(Build build, String relativeFilePath) {
		return getBuildTransformedFilesPath(build).append(relativeFilePath).toString();
	}

	public String getPublishedFilePath(ReleaseCenter releaseCenter, String publishedFileName) {
		return getReleaseCenterPath(releaseCenter).append(publishedFileName).toString();
	}

	public String getReportPath(Build build) {
		return getBuildPath(build.getProduct(), build.getId()).append("build_report.json").toString();
	}

	public String getProductInputFilesPath(Product product) {
		return getProductPath(product).append(BUILD_FILES).append(SEPARATOR).append(INPUT_FILES).append(SEPARATOR).toString();
	}

	public String getBuildManifestDirectoryPath(Build build) {
		return getBuildPath(build).append(MANIFEST).append(SEPARATOR).toString();
	}

}

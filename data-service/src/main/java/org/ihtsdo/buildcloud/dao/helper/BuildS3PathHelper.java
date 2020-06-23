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
	private static final String CLASSIFICATION_RESULT_OUTPUT_FILES = "classification-result-output-files";
	private static final String BUILD_FILES = "product-files";
	private static final String SOURCES_FILES = "sources";
	private static final String MANIFEST = "manifest";
	private static final String TRANSFORMED_FILES = "transformed-files";
	public static final String LOG = "log";
	public static final String BUILD_LOG_TXT = "build_log.txt";
	private static final String QA_CONFIG_JSON = "qa-test-config.json";
	private static final String INPUT_PREPARE_REPORT_JSON = "input-prepare-report.json";
	private static final String INPUT_PREPARE_REPORT_DIR = "input-prepare-report";
	private static final String INPUT_GATHER_REPORT_DIR = "input-gather-report";
	private static final String INPUT_GATHER_REPORT_JSON = "input-gather-report.json";
	private static final String BUILD_RELEASE_LOG_JSON = "full-log.json";
	private static final String PRE_CONDITION_CHECKS_REPORT = "pre-condition-checks-report.json";
	private static final String POST_CONDITION_CHECKS_REPORT = "post-condition-checks-report.json";

	public StringBuilder getProductPath(final Product product) {
		return getReleaseCenterPath(product.getReleaseCenter()).append(product.getBusinessKey()).append(SEPARATOR);
	}

	public StringBuilder getReleaseCenterPath(final ReleaseCenter releaseCenter) {
		final StringBuilder path = new StringBuilder();
		path.append(releaseCenter.getBusinessKey());
		path.append(SEPARATOR);
		return path;
	}

	public StringBuilder getProductManifestDirectoryPath(final Product product) {
		return getProductPath(product).append(BUILD_FILES).append(SEPARATOR).append(MANIFEST).append(SEPARATOR);
	}

	public StringBuilder getBuildInputFilesPath(final Build build) {
		return getBuildPath(build.getProduct(), build.getId()).append(INPUT_FILES).append(SEPARATOR);
	}

	public String getBuildInputFilePath(final Build build, final String inputFile) {
		return getBuildInputFilesPath(build).append(inputFile).toString();
	}

	public StringBuilder getBuildOutputFilesPath(final Build build) {
		return getBuildPath(build.getProduct(), build.getId()).append(OUTPUT_FILES).append(SEPARATOR);
	}

	public String getBuildOutputFilePath(final Build build, final String relativeFilePath) {
		return getBuildOutputFilesPath(build).append(relativeFilePath).toString();
	}

	public String getBuildLogFilePath(final Build build, final String relativeFilePath) {
		return getBuildLogFilesPath(build).append(relativeFilePath).toString();
	}

	public StringBuilder getBuildLogFilesPath(final Build build) {
		return getBuildPath(build.getProduct(), build.getId()).append(LOG).append(SEPARATOR);
	}

	public String getMainBuildLogFilePath(final Build build) {
		return getBuildLogFilesPath(build).append(BUILD_LOG_TXT).toString();
	}

	public StringBuilder getBuildPath(final Build build) {
		return getBuildPath(build.getProduct(), build.getId());
	}

	public StringBuilder getBuildPath(final Product product, final String buildId) {
		return getProductPath(product).append(buildId).append(SEPARATOR);
	}

	public String getBuildConfigFilePath(final Build build) {
		return getFilePath(build, CONFIG_JSON);
	}

	public String getQATestConfigFilePath(final Build build) {
		return getFilePath(build, QA_CONFIG_JSON);
	} 
	public String getStatusFilePath(final Build build, final Build.Status status) {
		return getBuildPath(build).append(STATUS_PREFIX).append(status.toString()).toString();
	}

	public String getOutputFilesPath(final Build build) {
		return getBuildPath(build).append("output-files").append(SEPARATOR).toString();
	}

	private String getFilePath(final Build build, final String relativePath) {
		return getBuildPath(build).append(relativePath).toString();
	}

	public StringBuilder getBuildTransformedFilesPath(final Build build) {
		return getBuildPath(build.getProduct(), build.getId()).append(TRANSFORMED_FILES).append(SEPARATOR);
	}

	public String getTransformedFilePath(final Build build, final String relativeFilePath) {
		return getBuildTransformedFilesPath(build).append(relativeFilePath).toString();
	}

	public String getPublishedFilePath(final ReleaseCenter releaseCenter, final String publishedFileName) {
		return getReleaseCenterPath(releaseCenter).append(publishedFileName).toString();
	}

	public String getReportPath(final Build build) {
		return getBuildPath(build.getProduct(), build.getId()).append("build_report.json").toString();
	}

	public String getProductInputFilesPath(final Product product) {
		return getProductPath(product).append(BUILD_FILES).append(SEPARATOR).append(INPUT_FILES).append(SEPARATOR).toString();
	}

	public String getBuildManifestDirectoryPath(final Build build) {
		return getBuildPath(build).append(MANIFEST).append(SEPARATOR).toString();
	}

	public StringBuilder getProductSourcesPath(final Product product) {
		return getProductPath(product).append(BUILD_FILES).append(SEPARATOR).append(SOURCES_FILES).append(SEPARATOR);
	}

	public StringBuilder getProductSourceSubDirectoryPath(final Product product, final String sourceName) {
		return getProductSourcesPath(product).append(sourceName).append(SEPARATOR);
	}

	public String getInputFilePrepareLogPath(final Product product) {
		return getProductPath(product).append(BUILD_FILES).append(SEPARATOR).append(INPUT_PREPARE_REPORT_DIR).append(SEPARATOR).append(INPUT_PREPARE_REPORT_JSON).toString();
	}

	public String getBuildInputFilePrepareReportPath(Build build) {
		return getBuildPath(build).append(INPUT_PREPARE_REPORT_JSON).toString();
	}

	public String getInputGatherReportLogPath(final Product product) {
		return getProductPath(product).append(BUILD_FILES).append(SEPARATOR).append(INPUT_GATHER_REPORT_DIR).append(SEPARATOR).append(INPUT_GATHER_REPORT_JSON).toString();
	}

	public String getBuildInputGatherReportPath(Build build) {
		return getBuildPath(build).append(INPUT_GATHER_REPORT_JSON).toString();
	}

	public String getBuildFullLogJsonFromBuild(Build build) {
		return getBuildLogFilePath(build, BUILD_RELEASE_LOG_JSON);
	}

	public String getBuildFullLogJsonFromProduct(Product product) {
		return getProductPath(product).append(BUILD_FILES).append(SEPARATOR).append(LOG).append(SEPARATOR).append(BUILD_RELEASE_LOG_JSON).toString();
	}

	public String getBuildPreConditionCheckReportPath(Build build) {
		return getBuildPath(build).append(PRE_CONDITION_CHECKS_REPORT).toString();
	}

	public String getPostConditionCheckReportPath(Build build) {
		return getBuildPath(build).append(POST_CONDITION_CHECKS_REPORT).toString();
	}

	public StringBuilder getClassificationResultOutputFilePath(Build build) {
		return getBuildPath(build.getProduct(), build.getId()).append(CLASSIFICATION_RESULT_OUTPUT_FILES).append(SEPARATOR);
	}

	public String getClassificationResultOutputPath(final Build build, final String relativeFilePath) {
		return getClassificationResultOutputFilePath(build).append(relativeFilePath).toString();
	}
}

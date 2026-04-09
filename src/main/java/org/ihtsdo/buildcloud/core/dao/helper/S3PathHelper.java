package org.ihtsdo.buildcloud.core.dao.helper;

import org.ihtsdo.buildcloud.core.entity.Build;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.function.Predicate;

@Service
public class S3PathHelper {

	@Value("${srs.build.storage.path}")
	private String buildStoragePath;


	@Value("${srs.regression-build.storage.path}")
	private String regressionBuildStoragePath;

	@Value("${srs.regression-test-report.storage.path}")
	private String regressionTestReportStoragePath;

	@Value("${srs.product.manifest.storage.path}")
	private String productManifestStoragePath;

	@Value("${srs.published.releases.storage.path}")
	private String publishedReleasesStoragePath;

	@Value("${srs.published.releases.backup.storage.path}")
	private String publishedReleasesBackupStoragePath;

	@Value("${srs.publish.job.storage.path}")
	private String publishJobStoragePath;

	@Value("${srs.externally-maintained.storage.path}")
	private String externallyMaintainedStoragePath;

	public static final String SEPARATOR = "/";
	public static final String CONFIG_JSON = "configuration.json";
	private static final String STATUS_PREFIX = "status:";
	private static final String VISIBILITY_PREFIX = "visibility:";
	private static final String TAG_PREFIX = "tag:";
	private static final String USER_PREFIX = "user:";
	private static final String USER_ROLES_PREFIX = "user-roles:";
	public static final String OUTPUT_FILES = "output-files";
	private static final String INPUT_FILES = "input-files";
	private static final String CLASSIFICATION_RESULT_OUTPUT_FILES = "classification-result-output-files";
	private static final String BUILD_FILES = "product-files";
	private static final String SOURCES_FILES = "sources";
	private static final String MANIFEST = "manifest";
	private static final String TRANSFORMED_FILES = "transformed-files";
	public static final String LOG = "log";
	public static final String MARK_AS_DELETED = "MARK_AS_DELETED";
	public static final String BUILD_LOG_TXT = "build_log.txt";
	public static final String QA_CONFIG_JSON = "qa-test-config.json";
	public static final String MANIFEST_CONFIG_JSON = "manifest-config.json";
	private static final String INPUT_PREPARE_REPORT_JSON = "input-prepare-report.json";
	private static final String INPUT_GATHER_REPORT_JSON = "input-gather-report.json";
	public static final String PRE_CONDITION_CHECKS_REPORT = "pre-condition-checks-report.json";
	public static final String POST_CONDITION_CHECKS_REPORT = "post-condition-checks-report.json";
	public static final String BUILD_REPORT_JSON = "build_report.json";
	public static final String BUILD_COMPARISON_REPORT = "build-comparison-reports";
	public static final String FILE_COMPARISON_REPORT = "file-comparison-reports";

	public StringBuilder getReleaseCenterPath(final String releaseCenterKey, final String storagePath) {
		final StringBuilder path = new StringBuilder(storagePath);

		if (!storagePath.endsWith(SEPARATOR))
			path.append(SEPARATOR);

		path.append(releaseCenterKey);
		path.append(SEPARATOR);
		return path;
	}

	public String getBuildStoragePath() {
		return buildStoragePath;
	}

	public String getRegressionBuildStoragePath() {
		return regressionBuildStoragePath;
	}

	/**
	 * Resolves whether build content is under {@link #buildStoragePath} or {@link #regressionBuildStoragePath}
	 * by checking for {@code configuration.json}. Falls back to primary when neither exists (e.g. new build).
	 */
	public String resolveContentStorageRoot(final String releaseCenterKey, final String productKey, final String buildId,
			final Predicate<String> pathExists) {
		String primaryConfig = getBuildPath(releaseCenterKey, productKey, buildId, buildStoragePath).append(CONFIG_JSON).toString();
		if (pathExists.test(primaryConfig)) {
			return buildStoragePath;
		}
		String regressionConfig = getBuildPath(releaseCenterKey, productKey, buildId, regressionBuildStoragePath).append(CONFIG_JSON).toString();
		if (pathExists.test(regressionConfig)) {
			return regressionBuildStoragePath;
		}
		return buildStoragePath;
	}

	public StringBuilder getProductPath(final String releaseCenterKey, final String productKey) {
		return getProductPath(releaseCenterKey, productKey, buildStoragePath);
	}

	public StringBuilder getProductPath(final String releaseCenterKey, final String productKey, final String storageRoot) {
		return getReleaseCenterPath(releaseCenterKey, storageRoot).append(productKey).append(SEPARATOR);
	}

	public StringBuilder getBuildInputFilesPath(final String releaseCenterKey, final String productKey, final String buildId) {
		return getBuildInputFilesPath(releaseCenterKey, productKey, buildId, buildStoragePath);
	}

	public StringBuilder getBuildInputFilesPath(final String releaseCenterKey, final String productKey, final String buildId, final String storageRoot) {
		return getBuildPath(releaseCenterKey, productKey, buildId, storageRoot).append(INPUT_FILES).append(SEPARATOR);
	}

	public StringBuilder getBuildInputFilesPath(final Build build) {
		return getBuildPath(build).append(INPUT_FILES).append(SEPARATOR);
	}

	public String getBuildInputFilePath(final Build build, final String inputFile) {
		return getBuildInputFilesPath(build).append(inputFile).toString();
	}

	public StringBuilder getBuildOutputFilesPath(final Build build) {
		return getBuildPath(build).append(OUTPUT_FILES).append(SEPARATOR);
	}

	public String getBuildOutputFilePath(final Build build, final String relativeFilePath) {
		return getBuildOutputFilesPath(build).append(relativeFilePath).toString();
	}

	public String getBuildLogFilePath(final Build build, final String relativeFilePath) {
		return getBuildLogFilesPath(build).append(relativeFilePath).toString();
	}

	public StringBuilder getBuildLogFilesPath(final Build build) {
		return getBuildPath(build).append(LOG).append(SEPARATOR);
	}

	public String getMainBuildLogFilePath(final Build build) {
		return getBuildLogFilesPath(build).append(BUILD_LOG_TXT).toString();
	}

	public StringBuilder getBuildPath(final Build build) {
		return getBuildPath(build.getReleaseCenterKey(), build.getProductKey(), build.getId(), storageRootFor(build));
	}

	public StringBuilder getBuildPath(final String releaseCenterKey, final String productKey, final String buildId) {
		return getBuildPath(releaseCenterKey, productKey, buildId, buildStoragePath);
	}

	public StringBuilder getBuildPath(final String releaseCenterKey, final String productKey, final String buildId, final String storageRoot) {
		return getProductPath(releaseCenterKey, productKey, storageRoot).append(buildId).append(SEPARATOR);
	}

	private String storageRootFor(final Build build) {
		return build.getContentStoragePath() != null ? build.getContentStoragePath() : buildStoragePath;
	}

	public String getBuildConfigFilePath(final Build build) {
		return getFilePath(build, CONFIG_JSON);
	}

	public String getBuildConfigFilePath(final String releaseCenterKey, final String productKey, final String buildId, final String storageRoot) {
		return getBuildPath(releaseCenterKey, productKey, buildId, storageRoot).append(CONFIG_JSON).toString();
	}

	public String getQATestConfigFilePath(final Build build) {
		return getFilePath(build, QA_CONFIG_JSON);
	} 

	public String getStatusFilePath(final Build build, final Build.Status status) {
		return getBuildPath(build).append(STATUS_PREFIX).append(status.toString()).toString();
	}

	public String getVisibilityFilePath(final Build build, final boolean visibility) {
		return getBuildPath(build).append(VISIBILITY_PREFIX).append(visibility).toString();
	}

	public String getTagFilePath(final Build build, final String tags) {
		return getBuildPath(build).append(TAG_PREFIX).append(tags).toString();
	}

	public String getBuildUserFilePath(final Build build, final String user) {
		return getBuildPath(build).append(USER_PREFIX).append(user).toString();
	}

	public String getBuildUserRolesFilePath(final Build build, final List<String> roles) {
		return getBuildPath(build).append(USER_ROLES_PREFIX).append(String.join(",", roles)).toString();
	}

	public String getOutputFilesPath(final Build build) {
		return getBuildPath(build).append(OUTPUT_FILES).append(SEPARATOR).toString();
	}

	private String getFilePath(final Build build, final String relativePath) {
		return getBuildPath(build).append(relativePath).toString();
	}

	public StringBuilder getBuildTransformedFilesPath(final Build build) {
		return getBuildPath(build).append(TRANSFORMED_FILES).append(SEPARATOR);
	}

	public String getTransformedFilePath(final Build build, final String relativeFilePath) {
		return getBuildTransformedFilesPath(build).append(relativeFilePath).toString();
	}

	public String getPublishJobDirectoryPath(final String releaseCenterKey) {
		return getReleaseCenterPath(releaseCenterKey, publishJobStoragePath).toString();
	}

	public String getPublishJobFilePath(final String releaseCenterKey, final String fileName) {
		return getReleaseCenterPath(releaseCenterKey, publishJobStoragePath).append(fileName).toString();
	}

	public String getPublishedReleasesDirectoryPath(final String releaseCenterKey) {
		return getReleaseCenterPath(releaseCenterKey, publishedReleasesStoragePath).toString();
	}

	public String getPublishedReleasesBackupDirectoryPath(final String releaseCenterKey) {
		return getReleaseCenterPath(releaseCenterKey, publishedReleasesBackupStoragePath).toString();
	}


	public String getPublishedReleasesFilePath(final String releaseCenterKey, final String fileName) {
		return getReleaseCenterPath(releaseCenterKey, publishedReleasesStoragePath).append(fileName).toString();
	}

	public String getReportPath(final Build build) {
		return getBuildPath(build).append(BUILD_REPORT_JSON).toString();
	}

	public String getBuildManifestDirectoryPath(final Build build) {
		return getBuildPath(build).append(MANIFEST).append(SEPARATOR).toString();
	}

	public String getBuildManifestDirectoryPath(final String releaseCenterKey, final String productKey, final String buildId) {
		return getBuildPath(releaseCenterKey, productKey, buildId).append(MANIFEST).append(SEPARATOR).toString();
	}

	public String getBuildManifestDirectoryPath(final String releaseCenterKey, final String productKey, final String buildId, final String storageRoot) {
		return getBuildPath(releaseCenterKey, productKey, buildId, storageRoot).append(MANIFEST).append(SEPARATOR).toString();
	}

	public String getProductManifestDirectoryPath(final String releaseCenterKey, final String productKey) {
		return getReleaseCenterPath(releaseCenterKey, productManifestStoragePath)
				.append(productKey)
				.append(SEPARATOR).toString();
	}

	public StringBuilder getProductSourcesPath(final String releaseCenterKey, final String productKey) {
		return getProductPath(releaseCenterKey, productKey).append(BUILD_FILES).append(SEPARATOR).append(SOURCES_FILES).append(SEPARATOR);
	}

	public StringBuilder getBuildSourcesPath(final String releaseCenterKey, final String productKey, final String buildId) {
		return getBuildSourcesPath(releaseCenterKey, productKey, buildId, buildStoragePath);
	}

	public StringBuilder getBuildSourcesPath(final String releaseCenterKey, final String productKey, final String buildId, final String storageRoot) {
		return getProductPath(releaseCenterKey, productKey, storageRoot).append(buildId).append(SEPARATOR).append(SOURCES_FILES).append(SEPARATOR);
	}

	public StringBuilder getBuildSourceSubDirectoryPath(final Build build, final String sourceName) {
		return getBuildPath(build).append(SOURCES_FILES).append(SEPARATOR).append(sourceName).append(SEPARATOR);
	}

	public StringBuilder getBuildSourceSubDirectoryPath(final String releaseCenterKey, final String productKey, final String buildId, final String sourceName) {
		return getBuildSourcesPath(releaseCenterKey, productKey, buildId).append(sourceName).append(SEPARATOR);
	}

	public StringBuilder getBuildSourceSubDirectoryPath(final String releaseCenterKey, final String productKey, final String buildId, final String sourceName, final String storageRoot) {
		return getBuildSourcesPath(releaseCenterKey, productKey, buildId, storageRoot).append(sourceName).append(SEPARATOR);
	}

	public String getBuildInputFilePrepareReportPath(final String releaseCenterKey, final String productKey, final String buildId) {
		return getBuildInputFilePrepareReportPath(releaseCenterKey, productKey, buildId, buildStoragePath);
	}

	public String getBuildInputFilePrepareReportPath(final String releaseCenterKey, final String productKey, final String buildId, final String storageRoot) {
		return getBuildPath(releaseCenterKey, productKey, buildId, storageRoot).append(INPUT_PREPARE_REPORT_JSON).toString();
	}

	public String getBuildInputFilePrepareReportPath(final Build build) {
		return getBuildPath(build).append(INPUT_PREPARE_REPORT_JSON).toString();
	}

	public String getBuildInputGatherReportPath(final String releaseCenterKey, final String productKey, final String buildId) {
		return getBuildInputGatherReportPath(releaseCenterKey, productKey, buildId, buildStoragePath);
	}

	public String getBuildInputGatherReportPath(final String releaseCenterKey, final String productKey, final String buildId, final String storageRoot) {
		return getBuildPath(releaseCenterKey, productKey, buildId, storageRoot).append(INPUT_GATHER_REPORT_JSON).toString();
	}

	public String getBuildInputGatherReportPath(final Build build) {
		return getBuildPath(build).append(INPUT_GATHER_REPORT_JSON).toString();
	}

	public String getBuildPreConditionCheckReportPath(final Build build) {
		return getBuildPath(build).append(PRE_CONDITION_CHECKS_REPORT).toString();
	}

	public String getPostConditionCheckReportPath(final Build build) {
		return getBuildPath(build).append(POST_CONDITION_CHECKS_REPORT).toString();
	}

	public StringBuilder getClassificationResultOutputFilePath(final Build build) {
		return getBuildPath(build).append(CLASSIFICATION_RESULT_OUTPUT_FILES).append(SEPARATOR);
	}

	public String getClassificationResultOutputPath(final Build build, final String relativeFilePath) {
		return getClassificationResultOutputFilePath(build).append(relativeFilePath).toString();
	}

	public String getBuildComparisonReportPath(final String releaseCenterKey, final String productKey, final String compareId) {
		StringBuilder builder = getReleaseCenterPath(releaseCenterKey, regressionTestReportStoragePath)
				.append(productKey)
				.append(SEPARATOR)
				.append(BUILD_COMPARISON_REPORT)
				.append(SEPARATOR);
		if (StringUtils.hasLength(compareId)) {
			builder.append(compareId).append(".json");
		}
		return builder.toString();
	}

	/**
	 * Legacy location under default build storage path. Kept for backward-compatible reads/deletes.
	 */
	public String getLegacyBuildComparisonReportPath(final String releaseCenterKey, final String productKey, final String compareId) {
		StringBuilder builder = getProductPath(releaseCenterKey, productKey).append(BUILD_COMPARISON_REPORT).append(SEPARATOR);
		if (StringUtils.hasLength(compareId)) {
			builder.append(compareId).append(".json");
		}
		return builder.toString();
	}

	public String getFileComparisonReportPath(final String releaseCenterKey, final String productKey, final String compareId, final String fileName) {
		return getReleaseCenterPath(releaseCenterKey, regressionTestReportStoragePath)
				.append(productKey)
				.append(SEPARATOR)
				.append(FILE_COMPARISON_REPORT).append(SEPARATOR).append(compareId).append(SEPARATOR)
				.append(fileName).toString();
	}

	/**
	 * Legacy location under default build storage path. Kept for backward-compatible reads/deletes.
	 */
	public String getLegacyFileComparisonReportPath(final String releaseCenterKey, final String productKey, final String compareId, final String fileName) {
		return getProductPath(releaseCenterKey, productKey)
				.append(FILE_COMPARISON_REPORT).append(SEPARATOR).append(compareId).append(SEPARATOR)
				.append(fileName).toString();
	}

	public String getExternallyMaintainedDirectoryPath(final String releaseCenterKey, final String dateString) {
		return getReleaseCenterPath(releaseCenterKey, externallyMaintainedStoragePath)
				.append(dateString)
				.append(SEPARATOR).toString();
	}

	public String getManifestConfigFilePath(Build build) {
		return getFilePath(build, MANIFEST_CONFIG_JSON);
	}
}

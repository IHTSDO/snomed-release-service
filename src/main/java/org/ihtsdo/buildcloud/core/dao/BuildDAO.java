package org.ihtsdo.buildcloud.core.dao;

import org.ihtsdo.buildcloud.core.dao.io.AsyncPipedStreamBean;
import org.ihtsdo.buildcloud.core.entity.*;
import org.ihtsdo.buildcloud.core.service.BuildService;
import org.ihtsdo.buildcloud.core.service.build.compare.BuildComparisonReport;
import org.ihtsdo.buildcloud.core.service.build.compare.FileDiffReport;
import org.ihtsdo.buildcloud.rest.pojo.BuildPage;
import org.ihtsdo.otf.rest.exception.BadConfigurationException;
import org.springframework.data.domain.PageRequest;

import java.io.*;
import java.util.List;

public interface BuildDAO {

	void copyManifestFileFromProduct(Build build);

	void save(Build build) throws IOException;

	List<Build> findAllDesc(Product product, Boolean includeBuildConfiguration, Boolean includeQAConfiguration, Boolean includeRvfURL, Boolean visibility);

	BuildPage<Build> findAllDescPage(Product product, Boolean includeBuildConfiguration, Boolean includeQAConfiguration, Boolean includeRvfURL, Boolean visibility, BuildService.View viewMode, PageRequest pageRequest);

	Build find(Product product, String buildId, Boolean includeBuildConfiguration, Boolean includeQAConfiguration, Boolean includeRvfURL, Boolean visibility);

	void delete(Product product, String buildId);

	void loadConfiguration(Build build) throws IOException;

	void loadBuildConfiguration(Build build) throws IOException;

	void updateStatus(Build build, Build.Status newStatus);

	void addTag(Build build, Build.Tag tag);

	void saveTags(Build build, List<Build.Tag> tags);

	void assertStatus(Build build, Build.Status ensureStatus) throws BadConfigurationException;

	InputStream getOutputFileStream(Build build, String filePath);

	List<String> listInputFileNames(Build build);

	InputStream getInputFileStream(Build build, String relativeFilePath);

	InputStream getLocalInputFileStream(Build build, String relativeFilePath) throws FileNotFoundException;

	AsyncPipedStreamBean getOutputFileOutputStream(Build build, String relativeFilePath) throws IOException;

	AsyncPipedStreamBean getLogFileOutputStream(Build build, String relativeFilePath) throws IOException;

	void copyInputFileToOutputFile(Build build, String relativeFilePath);

	void copyBuildToAnother(Build sourceBuild, Build destBuild, String folder);

	InputStream getOutputFileInputStream(Build build, String name);

	String putOutputFile(Build build, File file, boolean calcMD5) throws IOException;

	String putOutputFile(Build build, File file)
			throws IOException;

	String putInputFile(Build build, File file, final boolean calcMD5) throws IOException;

	void putTransformedFile(Build build, File file) throws IOException;

	InputStream getManifestStream(Build build);

	List<String> listTransformedFilePaths(Build build);

	List<String> listOutputFilePaths(Build build);

	List<String> listLogFilePaths(Build build);

	List<String> listBuildLogFilePaths(Build build);

	InputStream getLogFileStream(Build build, String logFileName);

	InputStream getBuildLogFileStream(Build build, String logFileName);

	String getTelemetryBuildLogFilePath(Build build);

	AsyncPipedStreamBean getTransformedFileOutputStream(Build build, String relativeFilePath) throws IOException;

	OutputStream getLocalTransformedFileOutputStream(Build build, String relativeFilePath) throws FileNotFoundException;

	InputStream getTransformedFileAsInputStream(Build build, String relativeFilePath);

	InputStream getPublishedFileArchiveEntry(ReleaseCenter releaseCenter, String targetFileName, String previousPublishedPackage) throws IOException;

	void persistReport(Build build);

	void renameTransformedFile(Build build, String sourceFileName, String targetFileName, boolean deleteOriginal);

	void loadQaTestConfig(Build build) throws IOException;

	String getOutputFilePath(Build build, String filename);

	String getManifestFilePath(Build build);

	InputStream getBuildReportFileStream(Build build);

	InputStream getBuildInputFilesPrepareReportStream(Build build);

	boolean isBuildCancelRequested(Build build);

	void deleteOutputFiles(Build build);

	InputStream getBuildInputGatherReportStream(Build build);

	boolean isDerivativeProduct(Build build);

	void updatePreConditionCheckReport(Build build) throws IOException;

	void updatePostConditionCheckReport(Build build, Object object) throws IOException;

	InputStream getPreConditionCheckReportStream(Build build);

	List<PreConditionCheckReport> getPreConditionCheckReport(final Build build) throws IOException;

	InputStream getPostConditionCheckReportStream(Build build);

	List<PostConditionCheckReport> getPostConditionCheckReport(final Build build) throws IOException;

	List<String> listClassificationResultOutputFileNames(Build build);

	String putClassificationResultOutputFile(Build build, File file) throws IOException;

	InputStream getClassificationResultOutputFileStream(Build build, String relativeFilePath);

	void updateVisibility(Build build, boolean visibility);

	void putManifestFile(Product product, String buildId, InputStream inputStream);

	void saveBuildComparisonReport(Product product, String compareId, BuildComparisonReport report) throws IOException;

	List<String> listBuildComparisonReportPaths(Product product);

	BuildComparisonReport getBuildComparisonReport(Product product, String compareId) throws IOException;

	void saveFileComparisonReport(Product product, String compareId, FileDiffReport report) throws IOException;

	FileDiffReport getFileComparisonReport(Product product, String compareId, String fileName) throws IOException;
}
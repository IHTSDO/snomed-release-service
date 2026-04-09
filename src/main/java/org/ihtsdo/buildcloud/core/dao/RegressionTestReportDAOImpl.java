package org.ihtsdo.buildcloud.core.dao;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.awspring.cloud.s3.ObjectMetadata;
import org.ihtsdo.buildcloud.core.dao.helper.S3PathHelper;
import org.ihtsdo.buildcloud.core.service.build.RF2Constants;
import org.ihtsdo.buildcloud.core.service.build.compare.BuildComparisonReport;
import org.ihtsdo.buildcloud.core.service.build.compare.FileDiffReport;
import org.ihtsdo.otf.dao.s3.S3Client;
import org.ihtsdo.otf.dao.s3.helper.FileHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class RegressionTestReportDAOImpl implements RegressionTestReportDAO {

	private final FileHelper srsFileHelper;

	private final ObjectMapper objectMapper;

	private final S3Client s3Client;

	private final S3PathHelper pathHelper;

	private final String buildBucketName;

	@Autowired
	public RegressionTestReportDAOImpl(@Value("${srs.storage.bucketName}") final String storageBucketName,
			final S3Client s3Client,
			final ObjectMapper objectMapper,
			final S3PathHelper pathHelper) {
		this.buildBucketName = storageBucketName;
		this.srsFileHelper = new FileHelper(storageBucketName, s3Client);
		this.s3Client = s3Client;
		this.objectMapper = objectMapper;
		this.pathHelper = pathHelper;
	}

	@Override
	public void saveBuildComparisonReport(String releaseCenterKey, String productKey, String compareId, BuildComparisonReport report) throws IOException {
		File reportFile = toJson(report);
		try (FileInputStream reportInputStream = new FileInputStream(reportFile)) {
			s3Client.putObject(buildBucketName, pathHelper.getBuildComparisonReportPath(releaseCenterKey, productKey, compareId), reportInputStream, ObjectMetadata.builder().build(), reportFile.length());
		} finally {
			if (reportFile != null) {
				Files.deleteIfExists(reportFile.toPath());
			}
		}
	}

	@Override
	public List<String> listBuildComparisonReportPaths(String releaseCenterKey, String productKey) {
		final String reportPath = pathHelper.getBuildComparisonReportPath(releaseCenterKey, productKey, null);
		final String legacyReportPath = pathHelper.getLegacyBuildComparisonReportPath(releaseCenterKey, productKey, null);
		Set<String> allPaths = new LinkedHashSet<>(srsFileHelper.listFiles(reportPath));
		allPaths.addAll(srsFileHelper.listFiles(legacyReportPath));
		return new ArrayList<>(allPaths);
	}

	@Override
	public BuildComparisonReport getBuildComparisonReport(String releaseCenterKey, String productKey, String compareId) throws IOException {
		String filePath = pathHelper.getBuildComparisonReportPath(releaseCenterKey, productKey, compareId);
		BuildComparisonReport report = getBuildComparisonReportAtPath(filePath);
		if (report != null) {
			return report;
		}
		String legacyFilePath = pathHelper.getLegacyBuildComparisonReportPath(releaseCenterKey, productKey, compareId);
		return getBuildComparisonReportAtPath(legacyFilePath);
	}

	@Override
	public void deleteBuildComparisonReport(String releaseCenterKey, String productKey, String compareId) {
		String filePath = pathHelper.getBuildComparisonReportPath(releaseCenterKey, productKey, compareId);
		s3Client.deleteObject(buildBucketName, filePath);
		String legacyFilePath = pathHelper.getLegacyBuildComparisonReportPath(releaseCenterKey, productKey, compareId);
		s3Client.deleteObject(buildBucketName, legacyFilePath);
	}

	@Override
	public void saveFileComparisonReport(String releaseCenterKey, String productKey, String compareId, boolean ignoreIdComparison, FileDiffReport report) throws IOException {
		File reportFile = toJson(report);
		try (FileInputStream reportInputStream = new FileInputStream(reportFile)) {
			String reportFileName = report.getFileName().replace(".txt", ".diff.json") + "-" + ignoreIdComparison;
			s3Client.putObject(buildBucketName, pathHelper.getFileComparisonReportPath(releaseCenterKey, productKey, compareId, reportFileName), reportInputStream, ObjectMetadata.builder().build(), reportFile.length());
		} finally {
			if (reportFile != null) {
				Files.deleteIfExists(reportFile.toPath());
			}
		}
	}

	@Override
	public FileDiffReport getFileComparisonReport(String releaseCenterKey, String productKey, String compareId, String fileName, boolean ignoreIdComparison) throws IOException {
		String reportFileName = fileName.replace(".txt", ".diff.json") + "-" + ignoreIdComparison;
		String filePath = pathHelper.getFileComparisonReportPath(releaseCenterKey, productKey, compareId, reportFileName);
		FileDiffReport report = getFileDiffReportAtPath(filePath);
		if (report != null) {
			return report;
		}
		String legacyFilePath = pathHelper.getLegacyFileComparisonReportPath(releaseCenterKey, productKey, compareId, reportFileName);
		return getFileDiffReportAtPath(legacyFilePath);
	}

	private BuildComparisonReport getBuildComparisonReportAtPath(String filePath) throws IOException {
		try (final InputStream s3Object = s3Client.getObject(buildBucketName, filePath)) {
			if (s3Object != null) {
				final String reportJson = FileCopyUtils.copyToString(new InputStreamReader(s3Object, RF2Constants.UTF_8));
				try (JsonParser jsonParser = objectMapper.getFactory().createParser(reportJson)) {
					return jsonParser.readValueAs(BuildComparisonReport.class);
				}
			}
		} catch (S3Exception e) {
			if (e.statusCode() == 404) {
				return null;
			}
			throw e;
		}
		return null;
	}

	private FileDiffReport getFileDiffReportAtPath(String filePath) throws IOException {
		try (final InputStream s3Object = s3Client.getObject(buildBucketName, filePath)) {
			if (s3Object != null) {
				final String reportJson = FileCopyUtils.copyToString(new InputStreamReader(s3Object, RF2Constants.UTF_8));
				try (JsonParser jsonParser = objectMapper.getFactory().createParser(reportJson)) {
					return jsonParser.readValueAs(FileDiffReport.class);
				}
			}
		} catch (S3Exception e) {
			if (e.statusCode() == 404) {
				return null;
			}
			throw e;
		}
		return null;
	}

	private File toJson(final Object obj) throws IOException {
		final File temp = File.createTempFile("tempJson", ".tmp");
		objectMapper.disable(SerializationFeature.INDENT_OUTPUT);
		final JsonFactory jsonFactory = objectMapper.getFactory();
		try (JsonGenerator jsonGenerator = jsonFactory.createGenerator(temp, JsonEncoding.UTF8)) {
			jsonGenerator.writeObject(obj);
		}
		return temp;
	}
}

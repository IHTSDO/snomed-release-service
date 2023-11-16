package org.ihtsdo.buildcloud.core.dao;

import org.apache.commons.codec.CharEncoding;
import org.apache.commons.io.IOUtils;
import org.ihtsdo.buildcloud.core.dao.helper.S3PathHelper;
import org.ihtsdo.buildcloud.core.service.inputfile.gather.InputGatherReport;
import org.ihtsdo.buildcloud.core.service.inputfile.prepare.SourceFileProcessingReport;
import org.ihtsdo.otf.dao.s3.S3Client;
import org.ihtsdo.otf.dao.s3.helper.FileHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
@Service
public class InputFileDAOImpl implements InputFileDAO {

	private static final Logger LOGGER = LoggerFactory.getLogger(InputFileDAOImpl.class);

	private final FileHelper fileHelper;

	@Autowired
	private S3PathHelper s3PathHelper;

	@Autowired
	public InputFileDAOImpl(@Value("${srs.storage.bucketName}") final String storageBucketName,
							final S3Client s3Client) {
		fileHelper = new FileHelper(storageBucketName, s3Client);
	}

	@Override
	public InputStream getManifestStream(String releaseCenterKey, String productKey) {
		final String manifestPath = getManifestPath(releaseCenterKey, productKey);
		if (manifestPath != null) {
			return fileHelper.getFileStream(manifestPath);
		} else {
			return null;
		}
	}

	@Override
	public InputStream getManifestStream(String releaseCenterKey, String productKey, String buildId) {
		final String manifestPath = getManifestPath(releaseCenterKey, productKey, buildId);
		if (manifestPath != null) {
			return fileHelper.getFileStream(manifestPath);
		} else {
			return null;
		}
	}

	@Override
	public String getManifestPath(final String releaseCenterKey, final String productKey) {
		final String manifestDirectoryPath = s3PathHelper.getProductManifestDirectoryPath(releaseCenterKey, productKey);
		LOGGER.debug("manifestDirectoryPath '{}'", manifestDirectoryPath);
		final List<String> xmlFiles = fileHelper.listFiles(manifestDirectoryPath).stream().filter(file -> file.endsWith(".xml")).toList();
		if (xmlFiles.isEmpty()) {
			return null;
		}
		if (xmlFiles.size() > 1) {
			throw new IllegalStateException(String.format("Expecting just one manifest file but found %d in the manifest folder %s", xmlFiles.size(), manifestDirectoryPath));
		}
		return manifestDirectoryPath + xmlFiles.get(0);
	}

	public String getManifestPath(final String releaseCenterKey, final String productKey, final String buildId) {
		final String manifestDirectoryPath = s3PathHelper.getBuildManifestDirectoryPath(releaseCenterKey, productKey, buildId);
		LOGGER.debug("manifestDirectoryPath '{}'", manifestDirectoryPath);
		final List<String> xmlFiles = fileHelper.listFiles(manifestDirectoryPath).stream().filter(file -> file.endsWith(".xml")).toList();
		if (xmlFiles.isEmpty()) {
			return null;
		}
		if (xmlFiles.size() > 1) {
			throw new IllegalStateException(String.format("Expecting just one manifest file but found %d in the manifest folder %s", xmlFiles.size(), manifestDirectoryPath));
		}
		return manifestDirectoryPath + xmlFiles.get(0);
	}

	@Override
	public void putManifestFile(final String releaseCenterKey, final String productKey, final InputStream inputStream, final String originalFilename, final long fileSize) throws IOException {
		// Fist delete any existing manifest files
		deleteManifest(releaseCenterKey, productKey);
		// Put new manifest file
		final String filePath = getKnownManifestPath(releaseCenterKey, productKey, originalFilename);
		fileHelper.putFile(inputStream, fileSize, filePath);
	}

	@Override
	public void putManifestFile(final String releaseCenterKey, final String productKey, final String buildId, final InputStream inputStream, final String originalFilename, final long fileSize) throws IOException {
		final String filePath = s3PathHelper.getBuildManifestDirectoryPath(releaseCenterKey, productKey, buildId) + "manifest.xml";
		fileHelper.putFile(inputStream, fileSize, filePath);
	}

	@Override
	public void deleteManifest(final String releaseCenterKey, final String productKey) {
		final String manifestDirectoryPath = s3PathHelper.getProductManifestDirectoryPath(releaseCenterKey, productKey);
		final List<String> files = fileHelper.listFiles(manifestDirectoryPath);
		for (final String file : files) {
			fileHelper.deleteFile(manifestDirectoryPath + file);
		}
	}

	@Override
	public String getKnownManifestPath(final String releaseCenterKey, final String productKey, final String filename) {
		return s3PathHelper.getProductManifestDirectoryPath(releaseCenterKey, productKey) + filename;
	}

	@Override
	public List<String> listRelativeSourceFilePaths(final String releaseCenterKey, final String productKey, final String buildId) {
		String sourcesPath = s3PathHelper.getBuildSourcesPath(releaseCenterKey, productKey, buildId).toString();
		return fileHelper.listFiles(sourcesPath);
	}

	@Override
	public List<String> listRelativeSourceFilePaths(final String releaseCenterKey, final String productKey, final String buildId, final Set<String> subDirectories) {
		List<String> filesPath = new ArrayList<>();
		if(subDirectories != null && !subDirectories.isEmpty()) {
			for (String subDirectory : subDirectories) {
				String sourcePath = s3PathHelper.getBuildSourceSubDirectoryPath(releaseCenterKey, productKey, buildId, subDirectory).toString();
				filesPath.addAll(fileHelper.listFiles(sourcePath));
			}
		}
		return filesPath;
	}

	@Override
	public void persistInputPrepareReport(final String releaseCenterKey, final String productKey, final String buildId, final SourceFileProcessingReport fileProcessingReport) throws IOException {
		String reportPath = s3PathHelper.getBuildInputFilePrepareReportPath(releaseCenterKey, productKey, buildId);
		fileHelper.putFile(IOUtils.toInputStream(fileProcessingReport.toString(), CharEncoding.UTF_8), reportPath);
	}

	@Override
	public void persistSourcesGatherReport(final String releaseCenterKey, final String productKey, final String buildId, InputGatherReport inputGatherReport) throws IOException {
		String reportPath = s3PathHelper.getBuildInputGatherReportPath(releaseCenterKey, productKey, buildId);
		fileHelper.putFile(IOUtils.toInputStream(inputGatherReport.toString(), CharEncoding.UTF_8), reportPath);
	}
}

package org.ihtsdo.buildcloud.core.dao;

import org.apache.commons.codec.CharEncoding;
import org.apache.commons.io.IOUtils;
import org.ihtsdo.buildcloud.core.dao.helper.BuildS3PathHelper;
import org.ihtsdo.buildcloud.core.entity.Build;
import org.ihtsdo.buildcloud.core.entity.Product;
import org.ihtsdo.buildcloud.core.service.inputfile.gather.InputGatherReport;
import org.ihtsdo.buildcloud.core.service.inputfile.prepare.SourceFileProcessingReport;
import org.ihtsdo.otf.dao.s3.S3Client;
import org.ihtsdo.otf.dao.s3.helper.FileHelper;
import org.ihtsdo.otf.dao.s3.helper.S3ClientHelper;
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
import java.util.stream.Collectors;

@Service
public class InputFileDAOImpl implements InputFileDAO {

	private static final Logger LOGGER = LoggerFactory.getLogger(InputFileDAOImpl.class);

	private final FileHelper fileHelper;

	@Autowired
	private BuildS3PathHelper s3PathHelper;

	@Autowired
	public InputFileDAOImpl(@Value("${srs.build.bucketName}") final String buildBucketName,
							final S3Client s3Client,
							final S3ClientHelper s3ClientHelper) {
		fileHelper = new FileHelper(buildBucketName, s3Client, s3ClientHelper);
	}

	@Override
	public InputStream getManifestStream(final Product product) {
		final String manifestPath = getManifestPath(product);
		if (manifestPath != null) {
			return fileHelper.getFileStream(manifestPath);
		} else {
			return null;
		}
	}


	@Override
	public InputStream getManifestStream(final Product product, String buildId) {
		final String manifestPath = getManifestPath(product, buildId);
		if (manifestPath != null) {
			return fileHelper.getFileStream(manifestPath);
		} else {
			return null;
		}
	}

	@Override
	public String getManifestPath(final Product product) {
		final String manifestDirectoryPath = s3PathHelper.getProductManifestDirectoryPath(product).toString();
		LOGGER.debug("manifestDirectoryPath '{}'", manifestDirectoryPath);
		final List<String> xmlFiles = fileHelper.listFiles(manifestDirectoryPath).stream().filter(file -> file.endsWith(".xml")).collect(Collectors.toList());
		if (xmlFiles.isEmpty()) {
			return null;
		}
		if (xmlFiles.size() > 1) {
			throw new IllegalStateException(String.format("Expecting just one manifest file but found %d in the manifest folder %s", xmlFiles.size(), manifestDirectoryPath));
		}
		return manifestDirectoryPath + xmlFiles.get(0);
	}

	public String getManifestPath(final Product product, String buildId) {
		final String manifestDirectoryPath = s3PathHelper.getBuildManifestDirectoryPath(product, buildId);
		LOGGER.debug("manifestDirectoryPath '{}'", manifestDirectoryPath);
		final List<String> xmlFiles = fileHelper.listFiles(manifestDirectoryPath).stream().filter(file -> file.endsWith(".xml")).collect(Collectors.toList());
		if (xmlFiles.isEmpty()) {
			return null;
		}
		if (xmlFiles.size() > 1) {
			throw new IllegalStateException(String.format("Expecting just one manifest file but found %d in the manifest folder %s", xmlFiles.size(), manifestDirectoryPath));
		}
		return manifestDirectoryPath + xmlFiles.get(0);
	}

	@Override
	public void putManifestFile(final Product product, final InputStream inputStream, final String originalFilename, final long fileSize) {
		// Fist delete any existing manifest files
		deleteManifest(product);
		// Put new manifest file
		final String filePath = getKnownManifestPath(product, originalFilename);
		fileHelper.putFile(inputStream, fileSize, filePath);
	}


	@Override
	public void putManifestFile(final Product product, final String buildId, final InputStream inputStream, final String originalFilename, final long fileSize) {
		final String filePath = s3PathHelper.getBuildManifestDirectoryPath(product, buildId) + "manifest.xml";
		fileHelper.putFile(inputStream, fileSize, filePath);
	}

	@Override
	public void deleteManifest(final Product product) {
		final StringBuilder manifestDirectoryPathSB = s3PathHelper.getProductManifestDirectoryPath(product);
		final List<String> files = fileHelper.listFiles(manifestDirectoryPathSB.toString());
		for (final String file : files) {
			fileHelper.deleteFile(manifestDirectoryPathSB.toString() + file);
		}

	}

	@Override
	public String getKnownManifestPath(final Product product, final String filename) {
		return s3PathHelper.getProductManifestDirectoryPath(product).append(filename).toString();
	}

	@Override
	public List<String> listRelativeSourceFilePaths(final Product product, final String buildId) {
		String sourcesPath = s3PathHelper.getBuildSourcesPath(product, buildId).toString();
		return fileHelper.listFiles(sourcesPath);
	}

	@Override
	public List<String> listRelativeSourceFilePaths(final Product product, String buildId, final Set<String> subDirectories) {
		List<String> filesPath = new ArrayList<>();
		if(subDirectories != null && !subDirectories.isEmpty()) {
			for (String subDirectory : subDirectories) {
				String sourcePath = s3PathHelper.getBuildSourceSubDirectoryPath(product, buildId, subDirectory).toString();
				filesPath.addAll(fileHelper.listFiles(sourcePath));
			}
		}
		return filesPath;
	}

	@Override
	public List<String> listRelativeSourceFilePaths(Product product, String buildId, String subDirectory) {
		List<String> filesPath = new ArrayList<>();
		String sourcePath = s3PathHelper.getProductSourceSubDirectoryPath(product, subDirectory).toString();
		filesPath.addAll(fileHelper.listFiles(sourcePath));
		return filesPath;
	}

	@Override
	public void persistInputPrepareReport(final Build build, final SourceFileProcessingReport fileProcessingReport) throws IOException {
		String reportPath = s3PathHelper.getBuildInputFilePrepareReportPath(build);
		fileHelper.putFile(IOUtils.toInputStream(fileProcessingReport.toString(), CharEncoding.UTF_8), reportPath);
	}

	@Override
	public void persistSourcesGatherReport(Build build, InputGatherReport inputGatherReport) throws IOException {

		String reportPath = s3PathHelper.getBuildInputGatherReportPath(build);

		fileHelper.putFile(IOUtils.toInputStream(inputGatherReport.toString(), CharEncoding.UTF_8), reportPath);
	}

	@Override
	public InputStream getInputGatherReport(Product product, String buildId) {
		String reportPath = s3PathHelper.getInputGatherReportLogPath(product);
		return fileHelper.getFileStream(reportPath);
	}
}

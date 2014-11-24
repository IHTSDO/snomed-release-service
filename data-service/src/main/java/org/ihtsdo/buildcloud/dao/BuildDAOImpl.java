package org.ihtsdo.buildcloud.dao;

import com.amazonaws.services.s3.model.*;
import com.google.common.io.Files;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.ihtsdo.buildcloud.dao.helper.BuildS3PathHelper;
import org.ihtsdo.buildcloud.dao.helper.FileHelper;
import org.ihtsdo.buildcloud.dao.helper.S3ClientHelper;
import org.ihtsdo.buildcloud.dao.io.AsyncPipedStreamBean;
import org.ihtsdo.buildcloud.dao.s3.S3Client;
import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.Product;
import org.ihtsdo.buildcloud.entity.Build.Status;
import org.ihtsdo.buildcloud.entity.ReleaseCenter;
import org.ihtsdo.buildcloud.service.exception.BadConfigurationException;
import org.ihtsdo.buildcloud.service.build.RF2Constants;
import org.ihtsdo.buildcloud.service.file.FileUtils;
import org.ihtsdo.buildcloud.service.file.Rf2FileNameTransformation;
import org.ihtsdo.telemetry.core.TelemetryStreamPathBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.util.FileCopyUtils;

import java.io.*;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class BuildDAOImpl implements BuildDAO {

	private static final TypeReference<HashMap<String, Object>> MAP_TYPE_REF = new TypeReference<HashMap<String, Object>>() {
	};

	private static final String BLANK = "";

	private static final Logger LOGGER = LoggerFactory.getLogger(BuildDAOImpl.class);

	private final ExecutorService executorService;

	private final FileHelper buildFileHelper;

	private final ObjectMapper objectMapper;

	private final File tempDir;

	private final FileHelper publishedFileHelper;

	private final Rf2FileNameTransformation rf2FileNameTransformation;

	private S3Client s3Client;

	@Autowired
	private BuildS3PathHelper pathHelper;

	@Autowired
	private String buildBucketName;

	@Autowired
	private ProductInputFileDAO productInputFileDAO;

	@Autowired
	public BuildDAOImpl(final String buildBucketName, final String publishedBucketName, final S3Client s3Client, final S3ClientHelper s3ClientHelper) {
		objectMapper = new ObjectMapper();
		executorService = Executors.newCachedThreadPool();
		buildFileHelper = new FileHelper(buildBucketName, s3Client, s3ClientHelper);
		publishedFileHelper = new FileHelper(publishedBucketName, s3Client, s3ClientHelper);

		this.s3Client = s3Client;
		this.tempDir = Files.createTempDir();
		rf2FileNameTransformation = new Rf2FileNameTransformation();
	}

	@Override
	public void save(final Build build, final String jsonConfig) {
		// Save config file
		final String configPath = pathHelper.getConfigFilePath(build);
		putFile(configPath, jsonConfig);
		// Save status file
		final Status status = build.getStatus() == null ? Build.Status.BEFORE_TRIGGER : build.getStatus();
		updateStatus(build, status);
	}

	@Override
	public List<Build> findAllDesc(final Product product) {
		final String productDirectoryPath = pathHelper.getProductPath(product).toString();
		return findBuildsDesc(productDirectoryPath, product);
	}

	@Override
	public Build find(final Product product, final String buildId) {
		final String buildDirectoryPath = pathHelper.getBuildPath(product, buildId).toString();
		final List<Build> builds = findBuildsDesc(buildDirectoryPath, product);
		if (!builds.isEmpty()) {
			return builds.get(0);
		} else {
			return null;
		}
	}

	@Override
	public String loadConfiguration(final Build build) throws IOException {
		final String configFilePath = pathHelper.getConfigFilePath(build);
		final S3Object s3Object = s3Client.getObject(buildBucketName, configFilePath);
		if (s3Object != null) {
			final S3ObjectInputStream objectContent = s3Object.getObjectContent();
			return FileCopyUtils.copyToString(new InputStreamReader(objectContent, RF2Constants.UTF_8)); // Closes stream
		} else {
			return null;
		}
	}

	@Override
	public Map<String, Object> loadConfigurationMap(final Build build) throws IOException {
		final String jsonConfigString = loadConfiguration(build);
		if (jsonConfigString != null) {
			return objectMapper.readValue(jsonConfigString, MAP_TYPE_REF);
		} else {
			return null;
		}
	}

	@Override
	public void updateStatus(final Build build, final Build.Status newStatus) {
		final Build.Status origStatus = build.getStatus();
		build.setStatus(newStatus);
		final String newStatusFilePath = pathHelper.getStatusFilePath(build, build.getStatus());
		// Put new status before deleting old to avoid there being none.
		putFile(newStatusFilePath, BLANK);
		if (origStatus != null && origStatus != newStatus) {
			final String origStatusFilePath = pathHelper.getStatusFilePath(build, origStatus);
			s3Client.deleteObject(buildBucketName, origStatusFilePath);
		}
	}

	@Override
	public void assertStatus(final Build build, final Build.Status ensureStatus) throws BadConfigurationException {
		if (build.getStatus() != ensureStatus) {
			throw new BadConfigurationException("Build " + build.getCreationTime() + " is at status: " + build.getStatus().name()
					+ " and is expected to be at status:" + ensureStatus.name());
		}
	}

	@Override
	public InputStream getOutputFileStream(final Build build, final String filePath) {
		final String outputFilePath = pathHelper.getOutputFilesPath(build) + filePath;
		return buildFileHelper.getFileStream(outputFilePath);
	}

	@Override
	public List<String> listInputFileNames(final Build build) {
		final String buildInputFilesPath = pathHelper.getBuildInputFilesPath(build).toString();
		return buildFileHelper.listFiles(buildInputFilesPath);
	}

	@Override
	public InputStream getInputFileStream(final Build build, final String inputFile) {
		final String path = pathHelper.getBuildInputFilePath(build, inputFile);
		return buildFileHelper.getFileStream(path);
	}

	@Override
	public InputStream getLocalInputFileStream(final Build build, final String relativeFilePath) throws FileNotFoundException {
		final String transformedFilePath = pathHelper.getTransformedFilePath(build, relativeFilePath);
		final File localFile = getLocalFile(transformedFilePath);
		return new FileInputStream(localFile);
	}

	@Override
	public AsyncPipedStreamBean getOutputFileOutputStream(final Build build, final String relativeFilePath) throws IOException {
		final String buildOutputFilePath = pathHelper.getBuildOutputFilePath(build, relativeFilePath);
		return getFileAsOutputStream(buildOutputFilePath);
	}

	@Override
	public AsyncPipedStreamBean getLogFileOutputStream(final Build build, final String relativeFilePath) throws IOException {
		final String buildLogFilePath = pathHelper.getBuildLogFilePath(build, relativeFilePath);
		return getFileAsOutputStream(buildLogFilePath);
	}

	@Override
	public void copyInputFileToOutputFile(final Build build, final String relativeFilePath) {
		final String buildInputFilePath = pathHelper.getBuildInputFilePath(build, relativeFilePath);
		final String buildOutputFilePath = pathHelper.getBuildOutputFilePath(build, relativeFilePath);
		buildFileHelper.copyFile(buildInputFilePath, buildOutputFilePath);
	}

	@Override
	public void copyAll(final Product productSource, final Build build) {
		// Copy input files
		final String productInputFilesPath = pathHelper.getProductInputFilesPath(productSource);
		final String buildInputFilesPath = pathHelper.getBuildInputFilesPath(build).toString();
		final List<String> filePaths = productInputFileDAO.listRelativeInputFilePaths(productSource);
		for (final String filePath : filePaths) {
			buildFileHelper.copyFile(productInputFilesPath + filePath, buildInputFilesPath + filePath);
		}

		// Copy manifest file
		final String manifestPath = productInputFileDAO.getManifestPath(productSource);
		if (manifestPath != null) { // Let the packages with manifests product
			final String buildManifestDirectoryPath = pathHelper.getBuildManifestDirectoryPath(build);
			buildFileHelper.copyFile(manifestPath, buildManifestDirectoryPath + "manifest.xml");
		}
	}

	@Override
	public InputStream getOutputFileInputStream(final Build build, final String name) {
		final String path = pathHelper.getBuildOutputFilePath(build, name);
		return buildFileHelper.getFileStream(path);
	}

	@Override
	public String putOutputFile(final Build build, final File file, final boolean calcMD5) throws IOException {
		String filename = file.getName();
		final String outputFilePath = pathHelper.getBuildOutputFilePath(build, filename);
		try {
			return buildFileHelper.putFile(file, outputFilePath, calcMD5);
		} catch (NoSuchAlgorithmException | DecoderException e) {
			throw new IOException("Problem creating checksum while uploading " + filename, e);
		}
	}

	@Override
	public String putOutputFile(final Build build, final File file) throws IOException {
		return putOutputFile(build, file, false);
	}

	@Override
	public void putTransformedFile(final Build build, final File file) throws IOException {
		final String name = file.getName();
		final String outputPath = pathHelper.getBuildTransformedFilesPath(build).append(name).toString();
		try {
			buildFileHelper.putFile(file, outputPath);
		} catch (NoSuchAlgorithmException | DecoderException e) {
			throw new IOException("Problem creating checksum while uploading transformed file " + name, e);
		}
	}

	@Override
	public InputStream getManifestStream(final Build build) {
		final String directoryPath = pathHelper.getBuildManifestDirectoryPath(build);
		final List<String> files = buildFileHelper.listFiles(directoryPath);
		//The first file in the manifest directory we'll call our manifest
		if (!files.isEmpty()) {
			final String manifestFilePath = directoryPath + files.iterator().next();
			return buildFileHelper.getFileStream(manifestFilePath);
		} else {
			return null;
		}
	}

	@Override
	public List<String> listTransformedFilePaths(final Build build) {
		final String transformedFilesPath = pathHelper.getBuildTransformedFilesPath(build).toString();
		return buildFileHelper.listFiles(transformedFilesPath);
	}

	@Override
	public List<String> listOutputFilePaths(final Build build) {
		final String outputFilesPath = pathHelper.getOutputFilesPath(build);
		return buildFileHelper.listFiles(outputFilesPath);
	}

	@Override
	public List<String> listLogFilePaths(final Build build) {
		final String logFilesPath = pathHelper.getBuildLogFilesPath(build).toString();
		return buildFileHelper.listFiles(logFilesPath);
	}

	@Override
	public List<String> listBuildLogFilePaths(final Build build) {
		final String logFilesPath = pathHelper.getBuildLogFilesPath(build).toString();
		return buildFileHelper.listFiles(logFilesPath);
	}

	@Override
	public InputStream getLogFileStream(final Build build, final String logFileName) {
		final String logFilePath = pathHelper.getBuildLogFilePath(build, logFileName);
		return buildFileHelper.getFileStream(logFilePath);
	}

	@Override
	public InputStream getBuildLogFileStream(final Build build, final String logFileName) {
		final String logFilePath = pathHelper.getBuildLogFilePath(build, logFileName);
		return buildFileHelper.getFileStream(logFilePath);
	}

	@Override
	public String getTelemetryBuildLogFilePath(final Build build) {
		final String buildLogFilePath = pathHelper.getMainBuildLogFilePath(build);
		return TelemetryStreamPathBuilder.getS3StreamDestinationPath(buildBucketName, buildLogFilePath);
	}

	@Override
	public AsyncPipedStreamBean getTransformedFileOutputStream(final Build build, final String relativeFilePath) throws IOException {
		final String transformedFilePath = pathHelper.getTransformedFilePath(build, relativeFilePath);
		return getFileAsOutputStream(transformedFilePath);
	}

	@Override
	public OutputStream getLocalTransformedFileOutputStream(final Build build, final String relativeFilePath) throws FileNotFoundException {
		final String transformedFilePath = pathHelper.getTransformedFilePath(build, relativeFilePath);
		final File localFile = getLocalFile(transformedFilePath);
		localFile.getParentFile().mkdirs();
		return new FileOutputStream(localFile);
	}

	@Override
	public InputStream getTransformedFileAsInputStream(final Build build,
			final String relativeFilePath) {
		final String transformedFilePath = pathHelper.getTransformedFilePath(build, relativeFilePath);
		return buildFileHelper.getFileStream(transformedFilePath);
	}

	@Override
	public InputStream getPublishedFileArchiveEntry(final ReleaseCenter releaseCenter, final String targetFileName, final String previousPublishedPackage) throws IOException {
		final String publishedZipPath = pathHelper.getPublishedFilePath(releaseCenter, previousPublishedPackage);
		final String publishedExtractedZipPath = publishedZipPath.replace(".zip", "/");
		final String targetFileNameStripped = rf2FileNameTransformation.transformFilename(targetFileName);
		final List<String> filePaths = publishedFileHelper.listFiles(publishedExtractedZipPath);
		for (final String filePath : filePaths) {
			final String filename = FileUtils.getFilenameFromPath(filePath);
			if (filename.startsWith(targetFileNameStripped)) {
				return publishedFileHelper.getFileStream(publishedExtractedZipPath + filePath);
			}
		}
		return null;
	}

	@Override
	public void persistReport(final Build build) {
		final String reportPath = pathHelper.getReportPath(build);
		try {
			// Get the build report as a string we can write to disk/S3 synchronously because it's small
			final String buildReportJSON = build.getBuildReport().toString();
			final InputStream is = IOUtils.toInputStream(buildReportJSON, "UTF-8");
			buildFileHelper.putFile(is, buildReportJSON.length(), reportPath);
		} catch (final IOException e) {
			LOGGER.error("Unable to persist build report", e);
		}
	}

	@Override
	public void renameTransformedFile(final Build build, final String sourceFileName, final String targetFileName) {
		final String soureFilePath = pathHelper.getTransformedFilePath(build, sourceFileName);
		final String targetFilePath = pathHelper.getTransformedFilePath(build, targetFileName);
		buildFileHelper.copyFile(soureFilePath, targetFilePath);
	}

	private List<Build> findBuildsDesc(final String productDirectoryPath, final Product product) {
		final List<Build> builds = new ArrayList<>();
		LOGGER.info("List s3 objects {}, {}", buildBucketName, productDirectoryPath);

		// Not easy to make this efficient because our timestamp immediately under the product name means that we can only prefix
		// with the product name. The S3 API doesn't allow us to pattern match just the status files.
		// I think an "index" directory might be the solution

		final ListObjectsRequest listObjectsRequest = new ListObjectsRequest(buildBucketName, productDirectoryPath, null, null, 10000);
		ObjectListing objectListing = s3Client.listObjects(listObjectsRequest);

		boolean firstPass = true;
		while (firstPass || objectListing.isTruncated()) {
			if (!firstPass) {
				objectListing = s3Client.listNextBatchOfObjects(objectListing);
			}
			final List<S3ObjectSummary> objectSummaries = objectListing.getObjectSummaries();
			findBuilds(product, objectSummaries, builds);
			firstPass = false;
		}

		LOGGER.debug("Found {} Builds", builds.size());
		Collections.reverse(builds);
		return builds;
	}

	private void findBuilds(final Product product, final List<S3ObjectSummary> objectSummaries, final List<Build> builds) {
		for (final S3ObjectSummary objectSummary : objectSummaries) {
			final String key = objectSummary.getKey();
			if (key.contains("/status:")) {
				LOGGER.debug("Found status key {}", key);
				final String[] keyParts = key.split("/");
				final String dateString = keyParts[2];
				final String status = keyParts[3].split(":")[1];
				final Build build = new Build(dateString, status, product);
				builds.add(build);
			}
		}
	}

	private AsyncPipedStreamBean getFileAsOutputStream(final String buildOutputFilePath) throws IOException {
		// Stream file to buildFileHelper as it's written to the OutputStream
		final PipedInputStream pipedInputStream = new PipedInputStream();
		final PipedOutputStream outputStream = new PipedOutputStream(pipedInputStream);

		final Future<String> future = executorService.submit(new Callable<String>() {
			@Override
			public String call() throws Exception {
				buildFileHelper.putFile(pipedInputStream, buildOutputFilePath);
				LOGGER.debug("Build outputfile stream ended: {}", buildOutputFilePath);
				return buildOutputFilePath;
			}
		});

		return new AsyncPipedStreamBean(outputStream, future, buildOutputFilePath);
	}

	private PutObjectResult putFile(final String filePath, final String contents) {
		return s3Client.putObject(buildBucketName, filePath,
				new ByteArrayInputStream(contents.getBytes()), new ObjectMetadata());
	}

	@Required
	public void setBuildBucketName(final String buildBucketName) {
		this.buildBucketName = buildBucketName;
	}

	private File getLocalFile(final String transformedFilePath) throws FileNotFoundException {
		return new File(tempDir, transformedFilePath);
	}

	// Just for testing
	protected void setS3Client(final S3Client s3Client) {
		this.s3Client = s3Client;
		this.buildFileHelper.setS3Client(s3Client);
	}

}

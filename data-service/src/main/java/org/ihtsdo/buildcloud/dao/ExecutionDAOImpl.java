package org.ihtsdo.buildcloud.dao;

import com.amazonaws.services.s3.model.*;
import com.google.common.io.Files;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.ihtsdo.buildcloud.dao.helper.ExecutionS3PathHelper;
import org.ihtsdo.buildcloud.dao.helper.FileHelper;
import org.ihtsdo.buildcloud.dao.helper.S3ClientHelper;
import org.ihtsdo.buildcloud.dao.io.AsyncPipedStreamBean;
import org.ihtsdo.buildcloud.dao.s3.S3Client;
import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.Execution;
import org.ihtsdo.buildcloud.entity.Execution.Status;
import org.ihtsdo.buildcloud.entity.Package;
import org.ihtsdo.buildcloud.entity.Product;
import org.ihtsdo.buildcloud.service.exception.BadConfigurationException;
import org.ihtsdo.buildcloud.service.execution.RF2Constants;
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

public class ExecutionDAOImpl implements ExecutionDAO {

	private static final TypeReference<HashMap<String, Object>> MAP_TYPE_REF = new TypeReference<HashMap<String, Object>>() {
	};

	private static final String BLANK = "";

	private static final Logger LOGGER = LoggerFactory.getLogger(ExecutionDAOImpl.class);

	private final ExecutorService executorService;

	private final FileHelper executionFileHelper;

	private final ObjectMapper objectMapper;

	private final File tempDir;

	private final FileHelper publishedFileHelper;

	private final Rf2FileNameTransformation rf2FileNameTransformation;

	private S3Client s3Client;

	@Autowired
	private ExecutionS3PathHelper pathHelper;

	@Autowired
	private String executionBucketName;

	@Autowired
	private InputFileDAO inputFileDAO;

	@Autowired
	public ExecutionDAOImpl(final String executionBucketName, final String publishedBucketName, final S3Client s3Client, final S3ClientHelper s3ClientHelper) {
		objectMapper = new ObjectMapper();
		executorService = Executors.newCachedThreadPool();
		executionFileHelper = new FileHelper(executionBucketName, s3Client, s3ClientHelper);
		publishedFileHelper = new FileHelper(publishedBucketName, s3Client, s3ClientHelper);

		this.s3Client = s3Client;
		this.tempDir = Files.createTempDir();
		rf2FileNameTransformation = new Rf2FileNameTransformation();
	}

	@Override
	public void save(final Execution execution, final String jsonConfig) {
		// Save config file
		final String configPath = pathHelper.getConfigFilePath(execution);
		putFile(configPath, jsonConfig);
		// Save status file
		final Status status = execution.getStatus() == null ? Execution.Status.BEFORE_TRIGGER : execution.getStatus();
		updateStatus(execution, status);
	}

	@Override
	public ArrayList<Execution> findAllDesc(final Build build) {
		final String buildDirectoryPath = pathHelper.getBuildPath(build).toString();
		return findExecutionsDesc(buildDirectoryPath, build);
	}

	@Override
	public Execution find(final Build build, final String executionId) {
		final String executionDirectoryPath = pathHelper.getExecutionPath(build, executionId).toString();
		final ArrayList<Execution> executions = findExecutionsDesc(executionDirectoryPath, build);
		if (!executions.isEmpty()) {
			return executions.get(0);
		} else {
			return null;
		}
	}

	@Override
	public String loadConfiguration(final Execution execution) throws IOException {
		final String configFilePath = pathHelper.getConfigFilePath(execution);
		final S3Object s3Object = s3Client.getObject(executionBucketName, configFilePath);
		if (s3Object != null) {
			final S3ObjectInputStream objectContent = s3Object.getObjectContent();
			return FileCopyUtils.copyToString(new InputStreamReader(objectContent, RF2Constants.UTF_8)); // Closes stream
		} else {
			return null;
		}
	}

	@Override
	public Map<String, Object> loadConfigurationMap(final Execution execution) throws IOException {
		final String jsonConfigString = loadConfiguration(execution);
		if (jsonConfigString != null) {
			return objectMapper.readValue(jsonConfigString, MAP_TYPE_REF);
		} else {
			return null;
		}
	}

	@Override
	public void updateStatus(final Execution execution, final Execution.Status newStatus) {
		final Execution.Status origStatus = execution.getStatus();
		execution.setStatus(newStatus);
		final String newStatusFilePath = pathHelper.getStatusFilePath(execution, execution.getStatus());
		// Put new status before deleting old to avoid there being none.
		putFile(newStatusFilePath, BLANK);
		if (origStatus != null && origStatus != newStatus) {
			final String origStatusFilePath = pathHelper.getStatusFilePath(execution, origStatus);
			s3Client.deleteObject(executionBucketName, origStatusFilePath);
		}
	}

	@Override
	public void assertStatus(final Execution execution, final Execution.Status ensureStatus) throws BadConfigurationException {
		if (execution.getStatus() != ensureStatus) {
			throw new BadConfigurationException("Execution " + execution.getCreationTime() + " is at status: " + execution.getStatus().name()
					+ " and is expected to be at status:" + ensureStatus.name());
		}
	}

	@Override
	public InputStream getOutputFileStream(final Execution execution, final String packageId, final String filePath) {
		final String outputFilePath = pathHelper.getOutputFilesPath(execution, packageId) + filePath;
		return executionFileHelper.getFileStream(outputFilePath);
	}

	@Override
	public List<String> listInputFileNames(final Execution execution, final String packageId) {
		final String executionInputFilesPath = pathHelper.getExecutionInputFilesPath(execution, packageId).toString();
		return executionFileHelper.listFiles(executionInputFilesPath);
	}

	@Override
	public InputStream getInputFileStream(final Execution execution, final String packageBusinessKey, final String inputFile) {
		final String path = pathHelper.getExecutionInputFilePath(execution, packageBusinessKey, inputFile);
		return executionFileHelper.getFileStream(path);
	}

	@Override
	public InputStream getLocalInputFileStream(final Execution execution, final String packageBusinessKey, final String relativeFilePath) throws FileNotFoundException {
		final String transformedFilePath = pathHelper.getTransformedFilePath(execution, packageBusinessKey, relativeFilePath);
		final File localFile = getLocalFile(transformedFilePath);
		return new FileInputStream(localFile);
	}

	@Override
	public AsyncPipedStreamBean getOutputFileOutputStream(final Execution execution, final String packageBusinessKey, final String relativeFilePath) throws IOException {
		final String executionOutputFilePath = pathHelper.getExecutionOutputFilePath(execution, packageBusinessKey, relativeFilePath);
		return getFileAsOutputStream(executionOutputFilePath);
	}

	@Override
	public AsyncPipedStreamBean getLogFileOutputStream(final Execution execution, final String packageBusinessKey, final String relativeFilePath) throws IOException {
		final String executionLogFilePath = pathHelper.getExecutionLogFilePath(execution, packageBusinessKey, relativeFilePath);
		return getFileAsOutputStream(executionLogFilePath);
	}

	@Override
	public void copyInputFileToOutputFile(final Execution execution, final String packageBusinessKey, final String relativeFilePath) {
		final String executionInputFilePath = pathHelper.getExecutionInputFilePath(execution, packageBusinessKey, relativeFilePath);
		final String executionOutputFilePath = pathHelper.getExecutionOutputFilePath(execution, packageBusinessKey, relativeFilePath);
		executionFileHelper.copyFile(executionInputFilePath, executionOutputFilePath);
	}

	@Override
	public void copyAll(final Build buildSource, final Execution execution) {
		for (final Package buildPackage : buildSource.getPackages()) {
			// Copy input files
			final String buildPackageInputFilesPath = pathHelper.getPackageInputFilesPath(buildPackage);
			final String executionPackageInputFilesPath = pathHelper.getExecutionInputFilesPath(execution, buildPackage).toString();
			final List<String> filePaths = inputFileDAO.listInputFilePaths(buildPackage);
			for (final String filePath : filePaths) {
				executionFileHelper.copyFile(buildPackageInputFilesPath + filePath, executionPackageInputFilesPath + filePath);
			}

			// Copy manifest file
			final String manifestPath = inputFileDAO.getManifestPath(buildPackage);
			if (manifestPath != null) { // Let the packages with manifests build
				final String executionPackageManifestDirectoryPath = pathHelper.getExecutionManifestDirectoryPath(execution, buildPackage).toString();
				executionFileHelper.copyFile(manifestPath, executionPackageManifestDirectoryPath + "manifest.xml");
			}
		}
	}

	@Override
	public InputStream getOutputFileInputStream(final Execution execution, final String packageBusinessKey, final String name) {
		final String path = pathHelper.getExecutionOutputFilePath(execution, packageBusinessKey, name);
		return executionFileHelper.getFileStream(path);
	}

	@Override
	public InputStream getOutputFileInputStream(final Execution execution, final Package pkg, final String name) {
		return getOutputFileInputStream(execution, pkg.getBusinessKey(), name);
	}

	@Override
	public String putOutputFile(final Execution execution, final Package aPackage, final File file, final String targetRelativePath, final boolean calcMD5) throws IOException {
		final String outputFilePath = pathHelper.getExecutionOutputFilePath(execution, aPackage.getBusinessKey(), targetRelativePath + file.getName());
		try {
			return executionFileHelper.putFile(file, outputFilePath, calcMD5);
		} catch (NoSuchAlgorithmException | DecoderException e) {
			throw new IOException("Problem creating checksum while uploading " + targetRelativePath, e);
		}
	}

	@Override
	public String putOutputFile(final Execution execution, final Package aPackage, final File file) throws IOException {
		return putOutputFile(execution, aPackage, file, "/", false);
	}

	@Override
	public void putTransformedFile(final Execution execution, final Package pkg, final File file) throws IOException {
		final String name = file.getName();
		final String outputPath = pathHelper.getExecutionTransformedFilesPath(execution, pkg.getBusinessKey()).append(name).toString();
		try {
			executionFileHelper.putFile(file, outputPath);
		} catch (NoSuchAlgorithmException | DecoderException e) {
			throw new IOException("Problem creating checksum while uploading transformed file " + name, e);
		}
	}

	@Override
	public InputStream getManifestStream(final Execution execution, final Package pkg) {
		final StringBuffer manifestDirectoryPathSB = pathHelper.getExecutionManifestDirectoryPath(execution, pkg);

		final String directoryPath = manifestDirectoryPathSB.toString();
		final List<String> files = executionFileHelper.listFiles(directoryPath);
		//The first file in the manifest directory we'll call our manifest
		if (!files.isEmpty()) {
			final String manifestFilePath = directoryPath + files.iterator().next();
			return executionFileHelper.getFileStream(manifestFilePath);
		} else {
			return null;
		}
	}

	@Override
	public List<String> listTransformedFilePaths(final Execution execution,
			final String packageId) {

		final String transformedFilesPath = pathHelper.getExecutionTransformedFilesPath(execution, packageId).toString();
		return executionFileHelper.listFiles(transformedFilesPath);
	}

	@Override
	public List<String> listOutputFilePaths(final Execution execution,
			final String packageId) {
		final String outputFilesPath = pathHelper.getOutputFilesPath(execution, packageId);
		return executionFileHelper.listFiles(outputFilesPath);
	}

	@Override
	public List<String> listLogFilePaths(final Execution execution, final String packageId) {
		final String logFilesPath = pathHelper.getExecutionPackageLogFilesPath(execution, packageId).toString();
		return executionFileHelper.listFiles(logFilesPath);
	}

	@Override
	public List<String> listExecutionLogFilePaths(final Execution execution) {
		final String logFilesPath = pathHelper.getExecutionLogFilesPath(execution).toString();
		return executionFileHelper.listFiles(logFilesPath);
	}

	@Override
	public InputStream getLogFileStream(final Execution execution, final String packageId, final String logFileName) {
		final String logFilePath = pathHelper.getExecutionLogFilePath(execution, packageId, logFileName);
		return executionFileHelper.getFileStream(logFilePath);
	}

	@Override
	public InputStream getExecutionLogFileStream(final Execution execution, final String logFileName) {
		final String logFilePath = pathHelper.getExecutionLogFilePath(execution, logFileName);
		return executionFileHelper.getFileStream(logFilePath);
	}

	@Override
	public String getTelemetryExecutionLogFilePath(final Execution execution) {
		final String executionLogFilePath = pathHelper.getExecutionLogFilePath(execution);
		return TelemetryStreamPathBuilder.getS3StreamDestinationPath(executionBucketName, executionLogFilePath);
	}

	@Override
	public AsyncPipedStreamBean getTransformedFileOutputStream(final Execution execution, final String packageBusinessKey, final String relativeFilePath) throws IOException {
		final String transformedFilePath = pathHelper.getTransformedFilePath(execution, packageBusinessKey, relativeFilePath);
		return getFileAsOutputStream(transformedFilePath);
	}

	@Override
	public OutputStream getLocalTransformedFileOutputStream(final Execution execution, final String packageBusinessKey, final String relativeFilePath) throws FileNotFoundException {
		final String transformedFilePath = pathHelper.getTransformedFilePath(execution, packageBusinessKey, relativeFilePath);
		final File localFile = getLocalFile(transformedFilePath);
		localFile.getParentFile().mkdirs();
		return new FileOutputStream(localFile);
	}

	@Override
	public void copyTransformedFileToOutput(final Execution execution,
			final String packageBusinessKey, final String sourceFileName,
			final String targetFileName) {
		final String transformedFilePath = pathHelper.getTransformedFilePath(execution, packageBusinessKey, sourceFileName);
		final String executionOutputFilePath = pathHelper.getExecutionOutputFilePath(execution, packageBusinessKey, targetFileName);
		executionFileHelper.copyFile(transformedFilePath, executionOutputFilePath);

	}

	@Override
	public void copyTransformedFileToOutput(final Execution execution,
			final String packageBusinessKey, final String relativeFilePath) {
		copyTransformedFileToOutput(execution, packageBusinessKey, relativeFilePath, relativeFilePath);
	}

	@Override
	public InputStream getTransformedFileAsInputStream(final Execution execution,
			final String businessKey, final String relativeFilePath) {
		final String transformedFilePath = pathHelper.getTransformedFilePath(execution, businessKey, relativeFilePath);
		return executionFileHelper.getFileStream(transformedFilePath);
	}

	@Override
	public InputStream getPublishedFileArchiveEntry(final Product product, final String targetFileName, final String previousPublishedPackage) throws IOException {
		final String publishedZipPath = pathHelper.getPublishedFilePath(product, previousPublishedPackage);
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
	public void persistReport(final Execution execution) {

		final String reportPath = pathHelper.getReportPath(execution);
		try {
			// Get the execution report as a string we can write to disk/S3 synchronously because it's small
			final String executionReportJSON = execution.getExecutionReport().toString();
			final InputStream is = IOUtils.toInputStream(executionReportJSON, "UTF-8");
			executionFileHelper.putFile(is, executionReportJSON.length(), reportPath);
		} catch (final IOException e) {
			LOGGER.error("Unable to persist execution report", e);
		}
	}

	@Override
	public void renameTransformedFile(final Execution execution, final String packageBusinessKey, final String sourceFileName, final String targetFileName) {
		final String soureFilePath = pathHelper.getTransformedFilePath(execution, packageBusinessKey, sourceFileName);
		final String targetFilePath = pathHelper.getTransformedFilePath(execution, packageBusinessKey, targetFileName);
		executionFileHelper.copyFile(soureFilePath, targetFilePath);
	}

	private ArrayList<Execution> findExecutionsDesc(final String buildDirectoryPath, final Build build) {
		final ArrayList<Execution> executions = new ArrayList<>();
		LOGGER.info("List s3 objects {}, {}", executionBucketName, buildDirectoryPath);

		// Not easy to make this efficient because our timestamp immediately under the build name means that we can only prefix
		// with the build name. The S3 API doesn't allow us to pattern match just the status files.
		// I think an "index" directory might be the solution

		final ListObjectsRequest listObjectsRequest = new ListObjectsRequest(executionBucketName, buildDirectoryPath, null, null, 10000);
		ObjectListing objectListing = s3Client.listObjects(listObjectsRequest);

		boolean firstPass = true;
		while (firstPass || objectListing.isTruncated()) {
			if (!firstPass) {
				objectListing = s3Client.listNextBatchOfObjects(objectListing);
			}
			final List<S3ObjectSummary> objectSummaries = objectListing.getObjectSummaries();
			findExecutions(build, objectSummaries, executions);
			firstPass = false;
		}

		LOGGER.debug("Found {} Executions", executions.size());
		Collections.reverse(executions);
		return executions;
	}

	private void findExecutions(final Build build, final List<S3ObjectSummary> objectSummaries, final ArrayList<Execution> executions) {
		for (final S3ObjectSummary objectSummary : objectSummaries) {
			final String key = objectSummary.getKey();
			if (key.contains("/status:")) {
				LOGGER.debug("Found status key {}", key);
				final String[] keyParts = key.split("/");
				final String dateString = keyParts[2];
				final String status = keyParts[3].split(":")[1];
				final Execution execution = new Execution(dateString, status, build);
				executions.add(execution);
			}
		}
	}

	private AsyncPipedStreamBean getFileAsOutputStream(final String executionOutputFilePath) throws IOException {
		// Stream file to executionFileHelper as it's written to the OutputStream
		final PipedInputStream pipedInputStream = new PipedInputStream();
		final PipedOutputStream outputStream = new PipedOutputStream(pipedInputStream);

		final Future<String> future = executorService.submit(new Callable<String>() {
			@Override
			public String call() throws Exception {
				executionFileHelper.putFile(pipedInputStream, executionOutputFilePath);
				LOGGER.debug("Execution outputfile stream ended: {}", executionOutputFilePath);
				return executionOutputFilePath;
			}
		});

		return new AsyncPipedStreamBean(outputStream, future, executionOutputFilePath);
	}

	private PutObjectResult putFile(final String filePath, final String contents) {
		return s3Client.putObject(executionBucketName, filePath,
				new ByteArrayInputStream(contents.getBytes()), new ObjectMetadata());
	}

	@Required
	public void setExecutionBucketName(final String executionBucketName) {
		this.executionBucketName = executionBucketName;
	}

	private File getLocalFile(final String transformedFilePath) throws FileNotFoundException {
		return new File(tempDir, transformedFilePath);
	}

	// Just for testing
	protected void setS3Client(final S3Client s3Client) {
		this.s3Client = s3Client;
		this.executionFileHelper.setS3Client(s3Client);
	}

}

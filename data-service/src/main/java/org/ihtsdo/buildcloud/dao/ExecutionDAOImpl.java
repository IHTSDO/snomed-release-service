package org.ihtsdo.buildcloud.dao;

import com.amazonaws.services.s3.model.*;
import com.google.common.io.Files;
import org.apache.commons.codec.DecoderException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.ihtsdo.buildcloud.dao.helper.ExecutionS3PathHelper;
import org.ihtsdo.buildcloud.dao.helper.FileHelper;
import org.ihtsdo.buildcloud.dao.helper.S3ClientHelper;
import org.ihtsdo.buildcloud.dao.io.AsyncPipedStreamBean;
import org.ihtsdo.buildcloud.dao.s3.S3Client;
import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.Execution;
import org.ihtsdo.buildcloud.entity.Package;
import org.ihtsdo.buildcloud.entity.Product;
import org.ihtsdo.buildcloud.service.file.ArchiveEntry;
import org.ihtsdo.buildcloud.service.file.Rf2FileNameTransformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.util.FileCopyUtils;

import java.io.*;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ExecutionDAOImpl implements ExecutionDAO {

	private S3Client s3Client;
	private final ExecutorService executorService;

	@Autowired
	private ExecutionS3PathHelper pathHelper;

	@Autowired
	private String executionBucketName;

	@Autowired
	private String publishedBucketName;

	private final FileHelper executionFileHelper;

	private final FileHelper mavenFileHelper;

	@Autowired
	private InputFileDAO inputFileDAO;

	private final ObjectMapper objectMapper;

	private final File tempDir;

	private static final String BLANK = "";
	private static final Logger LOGGER = LoggerFactory.getLogger(ExecutionDAOImpl.class);
	private static final TypeReference<HashMap<String, Object>> MAP_TYPE_REF = new TypeReference<HashMap<String, Object>>() {};
	private final S3ClientHelper s3ClientHelper;

	@Autowired
	public ExecutionDAOImpl(String mavenBucketName, String executionBucketName, S3Client s3Client, S3ClientHelper s3ClientHelper) {
		objectMapper = new ObjectMapper();
		executorService = Executors.newCachedThreadPool();
		executionFileHelper = new FileHelper(executionBucketName, s3Client, s3ClientHelper);
		mavenFileHelper = new FileHelper(mavenBucketName, s3Client, s3ClientHelper);
		this.s3Client = s3Client;
		this.s3ClientHelper = s3ClientHelper;
		this.tempDir = Files.createTempDir();
	}

	@Override
	public void save(Execution execution, String jsonConfig) {
		// Save config file
		String configPath = pathHelper.getConfigFilePath(execution);
		putFile(configPath, jsonConfig);

		// Save status file
		updateStatus(execution, Execution.Status.BEFORE_TRIGGER);
	}

	@Override
	public ArrayList<Execution> findAll(Build build) {
		String buildDirectoryPath = pathHelper.getBuildPath(build).toString();
		return findExecutions(buildDirectoryPath, build);
	}

	@Override
	public Execution find(Build build, String executionId) {
		String executionDirectoryPath = pathHelper.getExecutionPath(build, executionId).toString();
		ArrayList<Execution> executions = findExecutions(executionDirectoryPath, build);
		if (!executions.isEmpty()) {
			return executions.get(0);
		} else {
			return null;
		}
	}

	@Override
	public String loadConfiguration(Execution execution) throws IOException {
		String configFilePath = pathHelper.getConfigFilePath(execution);
		S3Object s3Object = s3Client.getObject(executionBucketName, configFilePath);
		if (s3Object != null) {
			S3ObjectInputStream objectContent = s3Object.getObjectContent();
			return FileCopyUtils.copyToString(new InputStreamReader(objectContent)); // Closes stream
		} else {
			return null;
		}
	}

	@Override
	public Map<String, Object> loadConfigurationMap(Execution execution) throws IOException {
		String jsonConfigString = loadConfiguration(execution);
		if (jsonConfigString != null) {
			return objectMapper.readValue(jsonConfigString, MAP_TYPE_REF);
		} else {
			return null;
		}
	}

	@Override
	public void updateStatus(Execution execution, Execution.Status newStatus) {
		Execution.Status origStatus = execution.getStatus();
		execution.setStatus(newStatus);
		String newStatusFilePath = pathHelper.getStatusFilePath(execution, execution.getStatus());
		// Put new status before deleting old to avoid there being none.
		putFile(newStatusFilePath, BLANK);
		if (origStatus != null) {
			String origStatusFilePath = pathHelper.getStatusFilePath(execution, origStatus);
			s3Client.deleteObject(executionBucketName, origStatusFilePath);
		}
	}
	
	@Override
	public void assertStatus(Execution execution, Execution.Status ensureStatus) throws Exception {
		if (execution.getStatus() != ensureStatus) {
			throw new Exception ("Execution " + execution.getCreationTime() + " is at status: " + execution.getStatus().name() 
						+ " and is expected to be at status:" + ensureStatus.name());
		}
	}

	@Override
	public InputStream getOutputFileStream(Execution execution, String packageId, String filePath) {
		String outputFilePath = pathHelper.getOutputFilesPath(execution, packageId) + filePath;
		return executionFileHelper.getFileStream(outputFilePath);
	}

	@Override
	public InputStream getManifestStream(Execution execution, Package pkg) {
		StringBuffer manifestDirectoryPathSB = pathHelper.getExecutionManifestDirectoryPath(execution, pkg);

		String directoryPath = manifestDirectoryPathSB.toString();
		List<String> files = executionFileHelper.listFiles(directoryPath);
		//The first file in the manifest directory we'll call our manifest
		if (!files.isEmpty()) {
			String manifestFilePath = directoryPath + files.iterator().next();
			return executionFileHelper.getFileStream(manifestFilePath);
		} else {
			return null;
		}
	}	

	private ArrayList<Execution> findExecutions(String buildDirectoryPath, Build build) {
		ArrayList<Execution> executions = new ArrayList<>();
		LOGGER.info("List s3 objects {}, {}", executionBucketName, buildDirectoryPath);
		ListObjectsRequest listObjectsRequest = new ListObjectsRequest(executionBucketName, buildDirectoryPath, null, null, 1000);
		ObjectListing objectListing = s3Client.listObjects(listObjectsRequest);
		List<S3ObjectSummary> objectSummaries = objectListing.getObjectSummaries();

		for (S3ObjectSummary objectSummary : objectSummaries) {
			String key = objectSummary.getKey();
			if (key.contains("/status:")) {
				LOGGER.debug("Found status key {}", key);
				String[] keyParts = key.split("/");
				String dateString = keyParts[2];
				String status = keyParts[3].split(":")[1];
				Execution execution = new Execution(dateString, status, build);
				executions.add(execution);
			}
		}
		LOGGER.debug("Found {} Executions", executions.size());
		return executions;
	}

	@Override
	public
	String putOutputFile(Execution execution, Package aPackage, File file, String targetRelativePath, boolean calcMD5) throws NoSuchAlgorithmException, IOException, DecoderException {
		String outputFilePath = pathHelper.getExecutionOutputFilePath(execution, aPackage.getBusinessKey(), targetRelativePath + file.getName());
		return executionFileHelper.putFile(file, outputFilePath, calcMD5);
	}
	
	@Override
	public void copyAll(Build buildSource, Execution execution) {
		for (Package buildPackage : buildSource.getPackages()) {
			// Copy input files
			String buildPackageInputFilesPath = pathHelper.getPackageInputFilesPath(buildPackage);
			String executionPackageInputFilesPath = pathHelper.getExecutionInputFilesPath(execution, buildPackage).toString();
			List<String> filePaths = inputFileDAO.listInputFilePaths(buildPackage);
			for (String filePath : filePaths) {
				executionFileHelper.copyFile(buildPackageInputFilesPath + filePath, executionPackageInputFilesPath + filePath);
			}

			// Copy manifest file
			String manifestPath = inputFileDAO.getManifestPath(buildPackage);
			if (manifestPath != null) { // Let the packages with manifests build
				String executionPackageManifestDirectoryPath = pathHelper.getExecutionManifestDirectoryPath(execution, buildPackage).toString();
				executionFileHelper.copyFile(manifestPath, executionPackageManifestDirectoryPath + "manifest.xml");
			}
		}
	}
	
	@Override
	public List<String> listInputFilePaths(Execution execution, String packageId) {
		String executionInputFilesPath = pathHelper.getExecutionInputFilesPath(execution, packageId).toString();
		return executionFileHelper.listFiles(executionInputFilesPath);
	}

	@Override
	public InputStream getInputFileStream(Execution execution, String packageBusinessKey, String inputFile) {
		String path = pathHelper.getExecutionInputFilePath(execution, packageBusinessKey, inputFile);
		return executionFileHelper.getFileStream(path);
	}

	@Override
	public InputStream getLocalInputFileStream(Execution execution, String packageBusinessKey, String relativeFilePath) throws FileNotFoundException {
		String transformedFilePath = pathHelper.getTransformedFilePath(execution, packageBusinessKey, relativeFilePath);
		File localFile = getLocalFile(transformedFilePath);
		return new FileInputStream(localFile);
	}

	@Override
	public AsyncPipedStreamBean getOutputFileOutputStream(Execution execution, String packageBusinessKey, String relativeFilePath) throws IOException {
		String executionOutputFilePath = pathHelper.getExecutionOutputFilePath(execution, packageBusinessKey, relativeFilePath);
		return getFileAsOutputStream(executionOutputFilePath);
	}

	@Override
	public void copyInputFileToOutputFile(Execution execution, String packageBusinessKey, String relativeFilePath) {
		String executionInputFilePath = pathHelper.getExecutionInputFilePath(execution, packageBusinessKey, relativeFilePath);
		String executionOutputFilePath = pathHelper.getExecutionOutputFilePath(execution, packageBusinessKey, relativeFilePath);
		executionFileHelper.copyFile(executionInputFilePath, executionOutputFilePath);
	}
	
	@Override
	public InputStream getOutputFileInputStream(Execution execution, Package pkg, String name) {
		String path = pathHelper.getExecutionOutputFilePath(execution, pkg.getBusinessKey(), name);
		return executionFileHelper.getFileStream(path);
	}

	private AsyncPipedStreamBean getFileAsOutputStream(final String executionOutputFilePath) throws IOException {
		// Stream file to executionFileHelper as it's written to the OutputStream
		final PipedInputStream pipedInputStream = new PipedInputStream();
		PipedOutputStream outputStream = new PipedOutputStream(pipedInputStream);

		Future<String> future = executorService.submit(new Callable<String>() {
			@Override
			public String call() throws Exception {
				executionFileHelper.putFile(pipedInputStream, executionOutputFilePath);
				LOGGER.debug("Execution outputfile stream ended: {}", executionOutputFilePath);
				return executionOutputFilePath;
			}
		});

		return new AsyncPipedStreamBean(outputStream, future);
	}

	private PutObjectResult putFile(String filePath, String contents) {
		return s3Client.putObject(executionBucketName, filePath,
				new ByteArrayInputStream(contents.getBytes()), new ObjectMetadata());
	}
	
	@Override
	public void copyTransformedFileToOutput(Execution execution,
			String packageBusinessKey, String relativeFilePath) {
		copyTransformedFileToOutput(execution, packageBusinessKey, relativeFilePath, relativeFilePath);
	}


	@Override
	public void copyTransformedFileToOutput(Execution execution,
			String packageBusinessKey, String sourceFileName,
			String targetFileName) {
		String transformedFilePath = pathHelper.getTransformedFilePath(execution, packageBusinessKey, sourceFileName);
		String executionOutputFilePath = pathHelper.getExecutionOutputFilePath(execution, packageBusinessKey, targetFileName);
		executionFileHelper.copyFile(transformedFilePath, executionOutputFilePath);
		
	}

	@Override
	public InputStream getTransformedFileAsInputStream(Execution execution,
			String businessKey, String relativeFilePath) {
		String transformedFilePath = pathHelper.getTransformedFilePath(execution, businessKey, relativeFilePath);
		return executionFileHelper.getFileStream(transformedFilePath);
	}

	@Override
	public AsyncPipedStreamBean getTransformedFileOutputStream(Execution execution, String packageBusinessKey, String relativeFilePath) throws IOException {
		String transformedFilePath = pathHelper.getTransformedFilePath(execution, packageBusinessKey, relativeFilePath);
		return getFileAsOutputStream(transformedFilePath);
	}

	@Override
	public OutputStream getLocalTransformedFileOutputStream(Execution execution, String packageBusinessKey, String relativeFilePath) throws FileNotFoundException {
		String transformedFilePath = pathHelper.getTransformedFilePath(execution, packageBusinessKey, relativeFilePath);
		File localFile = getLocalFile(transformedFilePath);
		return new FileOutputStream(localFile);
	}

	@Override
	public List<String> listTransformedFilePaths(Execution execution,
			String packageId) {

		String transformedFilesPath = pathHelper.getExecutionTransformedFilesPath(execution, packageId).toString();
		return executionFileHelper.listFiles(transformedFilesPath);
	}

	@Override
	public List<String> listOutputFilePaths(Execution execution,
			String packageId) {
		String outputFilesPath = pathHelper.getOutputFilesPath(execution, packageId);
		return executionFileHelper.listFiles(outputFilesPath);
	}

	@Required
	public void setExecutionBucketName(String executionBucketName) {
		this.executionBucketName = executionBucketName;
	}

	@Required
	public void setPublishedBucketName(String publishedBucketName) {
		this.publishedBucketName = publishedBucketName;
	}

	@Override
	public ArchiveEntry getPublishedFileArchiveEntry(Product product, String targetFileName, String previousPublishedPackage) throws IOException {
		FileHelper publisheFileHelper = new FileHelper(publishedBucketName, s3Client, s3ClientHelper);
		String previousPublishedPackagePath = pathHelper.getPublishedFilePath(product, previousPublishedPackage);
		return publisheFileHelper.getArchiveEntry(targetFileName, previousPublishedPackagePath, new Rf2FileNameTransformation());
	}

	private File getLocalFile(String transformedFilePath) throws FileNotFoundException {
		return new File(tempDir, transformedFilePath);
	}

	// Just for testing
	public void setS3Client(S3Client s3Client) {
		this.s3Client = s3Client;
		this.executionFileHelper.setS3Client(s3Client);
		this.mavenFileHelper.setS3Client(s3Client);
	}

}

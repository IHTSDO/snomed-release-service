package org.ihtsdo.buildcloud.dao;

import com.amazonaws.services.s3.model.*;
import org.ihtsdo.buildcloud.dao.helper.ExecutionS3PathHelper;
import org.ihtsdo.buildcloud.dao.s3.S3Client;
import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.Execution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StreamUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ExecutionDAOImpl implements ExecutionDAO {

	@Autowired
	private S3Client s3Client;

	@Autowired
	private ExecutionS3PathHelper pathHelper;

	@Autowired
	private String executionS3BucketName;

	@Autowired
	private JmsTemplate jmsTemplate;

	private static final String BLANK = "";

	private static final Logger LOGGER = LoggerFactory.getLogger(ExecutionDAOImpl.class);

	@Override
	public void save(Execution execution, String jsonConfig) {
		saveToBucket(execution, jsonConfig);
	}

	@Override
	public ArrayList<Execution> findAll(Build build) {
		String buildDirectoryPath = pathHelper.getBuildPath(build).toString();
		ArrayList<Execution> executions = findExecutions(buildDirectoryPath, build);
		return executions;
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
		S3Object s3Object = s3Client.getObject(executionS3BucketName, configFilePath);
		if (s3Object != null) {
			S3ObjectInputStream objectContent = s3Object.getObjectContent();
//			objectContent.getHttpRequest().
			return FileCopyUtils.copyToString(new InputStreamReader(objectContent));
		} else {
			return null;
		}
	}

	@Override
	public void saveBuildScripts(File sourceDirectory, Execution execution) {
		saveFiles(sourceDirectory, pathHelper.getBuildScriptsPath(execution));
	}

	@Override
	public void streamBuildScriptsZip(Execution execution, OutputStream outputStream) throws IOException {
		StringBuffer buildScriptsPath = pathHelper.getBuildScriptsPath(execution);
		streamS3FilesAsZip(buildScriptsPath.toString(), outputStream);
	}

	@Override
	public void queueForBuilding(Execution execution) {
		updateStatus(execution, Execution.Status.QUEUED);
		// Note: this url will only work while the Builder is on the same server as the API.
		String executionApiUrl = String.format("http://localhost/api/v1/builds/%s/executions/%s/", execution.getBuild().getId(), execution.getId());
		LOGGER.info("Queuing Execution for building {}", executionApiUrl);
		jmsTemplate.convertAndSend(executionApiUrl);
	}

	@Override
	public void saveOutputFile(Execution execution, String filePath, InputStream inputStream, Long size) {
		LOGGER.debug("Saving execution output file path:{}, size:{}", filePath, size);
		String outputFilePath = pathHelper.getOutputFilePath(execution, filePath);
		putFile(outputFilePath, inputStream, size);
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
			s3Client.deleteObject(executionS3BucketName, origStatusFilePath);
		}
	}

	@Override
	public InputStream getOutputFile(Execution execution, String filePath) {
		String outputFilePath = pathHelper.getOutputFilePath(execution, filePath);
		try {
			S3Object object = s3Client.getObject(executionS3BucketName, outputFilePath);
			return object.getObjectContent();
		} catch (AmazonS3Exception e) {
			return returnNullOrThrow(e);
		}
	}

	private void saveFiles(File sourceDirectory, StringBuffer targetDirectoryPath) {
		File[] files = sourceDirectory.listFiles();
		for (File file : files) {
			StringBuffer filePath = new StringBuffer(targetDirectoryPath).append(file.getName());
			if (file.isFile()) {
				s3Client.putObject(executionS3BucketName, filePath.toString(), file);
			} else if (file.isDirectory()) {
				filePath.append(pathHelper.SEPARATOR);
				saveFiles(file, filePath);
			}
		}
	}

	private ArrayList<Execution> findExecutions(String buildDirectoryPath, Build build) {
		ArrayList<Execution> executions = new ArrayList<>();
		LOGGER.info("List s3 objects {}, {}", executionS3BucketName, buildDirectoryPath);
		ListObjectsRequest listObjectsRequest = new ListObjectsRequest(executionS3BucketName, buildDirectoryPath, null, null, 1000);
		ObjectListing objectListing = s3Client.listObjects(listObjectsRequest);
		List<S3ObjectSummary> objectSummaries = objectListing.getObjectSummaries();

		for (S3ObjectSummary objectSummary : objectSummaries) {
			String key = objectSummary.getKey();
			LOGGER.debug("Found key {}", key);
			if (key.contains("/status:")) {
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

	private void saveToBucket(Execution execution, String jsonConfig) {
		// Save config file
		String configPath = pathHelper.getConfigFilePath(execution);
		putFile(configPath, jsonConfig);

		// Save status file
		updateStatus(execution, Execution.Status.BEFORE_TRIGGER);
	}

	private PutObjectResult putFile(String filePath, InputStream stream, Long size) {
		ObjectMetadata objectMetadata = new ObjectMetadata();
		objectMetadata.setContentLength(size);
		return s3Client.putObject(executionS3BucketName, filePath, stream, objectMetadata);
	}

	private PutObjectResult putFile(String filePath, String contents) {
		return s3Client.putObject(executionS3BucketName, filePath,
				new ByteArrayInputStream(contents.getBytes()), new ObjectMetadata());
	}

	private void streamS3FilesAsZip(String buildScriptsPath, OutputStream outputStream) throws IOException {
		LOGGER.debug("Serving zip of files in {}", buildScriptsPath);
		ObjectListing objectListing = s3Client.listObjects(executionS3BucketName, buildScriptsPath);
		int buildScriptsPathLength = buildScriptsPath.length();

		ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream);
		for (S3ObjectSummary summary : objectListing.getObjectSummaries()) {
			String key = summary.getKey();
			String relativePath = key.substring(buildScriptsPathLength);
			LOGGER.debug("Zip entry. S3Key {}, Entry path {}", key, relativePath);
			zipOutputStream.putNextEntry(new ZipEntry(relativePath));
			S3Object object = s3Client.getObject(executionS3BucketName, key);
			try (InputStream objectContent = object.getObjectContent()) {
				StreamUtils.copy(objectContent, zipOutputStream);
			}
		}
		zipOutputStream.close();
	}

	private InputStream returnNullOrThrow(AmazonS3Exception e) {
		if (e.getStatusCode() == 404) {
			return null;
		} else {
			throw e;
		}
	}

	// Just for testing
	public void setS3Client(S3Client s3Client) {
		this.s3Client = s3Client;
	}

	// Just for testing
	public void setJmsTemplate(JmsTemplate jmsTemplate) {
		this.jmsTemplate = jmsTemplate;
	}
}

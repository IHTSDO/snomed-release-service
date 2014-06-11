package org.ihtsdo.buildcloud.dao;

import com.amazonaws.services.s3.model.*;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.ihtsdo.buildcloud.dao.helper.ExecutionS3PathHelper;
import org.ihtsdo.buildcloud.dao.helper.FileHelper;
import org.ihtsdo.buildcloud.dao.helper.S3ClientHelper;
import org.ihtsdo.buildcloud.dao.s3.S3Client;
import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.Execution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StreamUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ExecutionDAOImpl implements ExecutionDAO {

	private S3Client s3Client;

	@Autowired
	private ExecutionS3PathHelper pathHelper;

	@Autowired
	private String executionBucketName;
	
	@Autowired
	private String mavenBucketName;

	private FileHelper executionFileHelper;

	@Autowired
	private JmsTemplate jmsTemplate;

	private ObjectMapper objectMapper;

	private static final String BLANK = "";
	private static final Logger LOGGER = LoggerFactory.getLogger(ExecutionDAOImpl.class);
	private static final TypeReference<HashMap<String, Object>> MAP_TYPE_REF = new TypeReference<HashMap<String, Object>>() {};

	@Autowired
	public ExecutionDAOImpl(String mavenBucketName, S3Client s3Client, S3ClientHelper s3ClientHelper) {
		objectMapper = new ObjectMapper();
		executionFileHelper = new FileHelper(executionBucketName, s3Client, s3ClientHelper);
		this.s3Client = s3Client;
	}

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
	public void saveBuildScripts(File sourceDirectory, Execution execution) {
		executionFileHelper.putFiles(sourceDirectory, pathHelper.getBuildScriptsPath(execution));
	}

	@Override
	public void streamBuildScriptsZip(Execution execution, OutputStream outputStream) throws IOException {
		StringBuffer buildScriptsPath = pathHelper.getBuildScriptsPath(execution);
		executionFileHelper.streamS3FilesAsZip(buildScriptsPath.toString(), outputStream);
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
		executionFileHelper.putFile(inputStream, size, outputFilePath);
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
	public InputStream getOutputFile(Execution execution, String filePath) {
		String outputFilePath = pathHelper.getOutputFilePath(execution, filePath);
		return executionFileHelper.getFileStream(outputFilePath);
	}



	private ArrayList<Execution> findExecutions(String buildDirectoryPath, Build build) {
		ArrayList<Execution> executions = new ArrayList<>();
		LOGGER.info("List s3 objects {}, {}", executionBucketName, buildDirectoryPath);
		ListObjectsRequest listObjectsRequest = new ListObjectsRequest(executionBucketName, buildDirectoryPath, null, null, 1000);
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

	/*private PutObjectResult putFile(String filePath, InputStream stream, Long size) {
		ObjectMetadata objectMetadata = new ObjectMetadata();
		objectMetadata.setContentLength(size);
		return s3Client.putObject(executionBucketName, filePath, stream, objectMetadata);
	}*/

	private PutObjectResult putFile(String filePath, String contents) {
		return s3Client.putObject(executionBucketName, filePath,
				new ByteArrayInputStream(contents.getBytes()), new ObjectMetadata());
	}

	@Required
	public void setExecutionBucketName(String executionBucketName) {
		this.executionBucketName = executionBucketName;
	}

	@Required
	public void setMavenBucketName(String mavenBucketName) {
		this.mavenBucketName = mavenBucketName;
	}

	// Just for testing
	void setS3Client(S3Client s3Client) {
		this.s3Client = s3Client;
	}

	// Just for testing
	void setJmsTemplate(JmsTemplate jmsTemplate) {
		this.jmsTemplate = jmsTemplate;
	}
}

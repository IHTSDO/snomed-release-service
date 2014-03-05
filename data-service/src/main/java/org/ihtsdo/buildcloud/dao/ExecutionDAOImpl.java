package org.ihtsdo.buildcloud.dao;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.*;
import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.Execution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.util.FileCopyUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class ExecutionDAOImpl implements ExecutionDAO {

	@Autowired
	private AmazonS3Client s3Client;
	private String executionS3BucketName;

	private static final String STATUS_PREFIX = "status:";
	private static final String CONFIG_JSON = "configuration.json";
	private static final String SEPARATOR = "/";
	private static final String BLANK = "";

	private static final Logger LOGGER = LoggerFactory.getLogger(ExecutionDAOImpl.class);

	@Override
	public void save(Execution execution, String jsonConfig) {
		saveToBucket(execution, jsonConfig);
	}

	@Override
	public ArrayList<Execution> findAll(Build build) {
		String buildDirectoryPath = getDirectoryPath(build).toString();
		ArrayList<Execution> executions = findExecutions(buildDirectoryPath, build);
		return executions;
	}

	@Override
	public Execution find(Build build, String executionId) {
		StringBuffer path = getDirectoryPath(build);
		path.append(executionId).append(SEPARATOR);
		String executionDirectoryPath = path.toString();
		ArrayList<Execution> executions = findExecutions(executionDirectoryPath, build);
		if (!executions.isEmpty()) {
			return executions.get(0);
		} else {
			return null;
		}
	}

	@Override
	public String loadConfiguration(Build build, String executionId) throws IOException {
		StringBuffer path = getDirectoryPath(build);
		path.append(executionId).append(SEPARATOR).append(CONFIG_JSON);
		String configFilePath = path.toString();
		S3Object s3Object = s3Client.getObject(executionS3BucketName, configFilePath);
		if (s3Object != null) {
			S3ObjectInputStream objectContent = s3Object.getObjectContent();
			return FileCopyUtils.copyToString(new InputStreamReader(objectContent));
		} else {
			return null;
		}
	}

	private ArrayList<Execution> findExecutions(String buildDirectoryPath, Build build) {
		ArrayList<Execution> executions = new ArrayList<>();
		LOGGER.info("List s3 objects {}, {}", executionS3BucketName, buildDirectoryPath);
		ListObjectsRequest listObjectsRequest = new ListObjectsRequest(executionS3BucketName, buildDirectoryPath, null, null, 100);
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
		StringBuffer directoryPath = getDirectoryPath(execution);

		// Save config file
		String configPath = new StringBuffer(directoryPath).append(CONFIG_JSON).toString();
		putFile(configPath, jsonConfig);

		// Save status file
		String status = execution.getStatus().toString();
		String statusFilePath = new StringBuffer(directoryPath).append(STATUS_PREFIX).append(status).toString();
		putFile(statusFilePath, BLANK);
	}

	private PutObjectResult putFile(String filePath, String contents) {
		return s3Client.putObject(executionS3BucketName, filePath,
				new ByteArrayInputStream(contents.getBytes()), new ObjectMetadata());
	}

	private StringBuffer getDirectoryPath(Execution execution) {
		StringBuffer path = getDirectoryPath(execution.getBuild());
		path.append(execution.getCreationTime());
		path.append(SEPARATOR);
		return path;
	}

	private StringBuffer getDirectoryPath(Build build) {
		String releaseCentreBusinessKey = build.getProduct().getExtension().getReleaseCentre().getBusinessKey();
		StringBuffer path = new StringBuffer();
		path.append(releaseCentreBusinessKey);
		path.append(SEPARATOR);
		path.append(build.getCompositeKey());
		path.append(SEPARATOR);
		return path;
	}

	public void setS3Client(AmazonS3Client s3Client) {
		this.s3Client = s3Client;
	}

	@Required
	public void setExecutionS3BucketName(String executionS3BucketName) {
		this.executionS3BucketName = executionS3BucketName;
	}

}

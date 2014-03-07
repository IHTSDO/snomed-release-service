package org.ihtsdo.buildcloud.dao;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.*;
import org.ihtsdo.buildcloud.dao.helper.ExecutionS3PathHelper;
import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.Execution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.util.FileCopyUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class ExecutionDAOImpl implements ExecutionDAO {

	@Autowired
	private AmazonS3Client s3Client;

	@Autowired
	private ExecutionS3PathHelper pathHelper;

	private String executionS3BucketName;

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
			return FileCopyUtils.copyToString(new InputStreamReader(objectContent));
		} else {
			return null;
		}
	}

	@Override
	public void saveBuildScripts(File sourceDirectory, Execution execution) {
		saveFiles(sourceDirectory, pathHelper.getBuildScriptsPath(execution));
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
		// Save config file
		String configPath = pathHelper.getConfigFilePath(execution);
		putFile(configPath, jsonConfig);

		// Save status file
		String status = execution.getStatus().toString();
		String statusFilePath = pathHelper.getStatusFilePath(execution, status);
		putFile(statusFilePath, BLANK);
	}

	private PutObjectResult putFile(String filePath, String contents) {
		return s3Client.putObject(executionS3BucketName, filePath,
				new ByteArrayInputStream(contents.getBytes()), new ObjectMetadata());
	}

	public void setS3Client(AmazonS3Client s3Client) {
		this.s3Client = s3Client;
	}

	@Required
	public void setExecutionS3BucketName(String executionS3BucketName) {
		this.executionS3BucketName = executionS3BucketName;
	}

}

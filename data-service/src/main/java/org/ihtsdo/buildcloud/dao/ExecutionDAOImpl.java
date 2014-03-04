package org.ihtsdo.buildcloud.dao;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectResult;
import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.Execution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;

import java.io.ByteArrayInputStream;

public class ExecutionDAOImpl implements ExecutionDAO {

	@Autowired
	private AmazonS3Client s3Client;

	private String executionS3BucketName;

	private static final String STATUS = "status";
	private static final String CONFIG_JSON = "config.json";

	@Override
	public void save(Execution execution) {
		putInBucket(execution);
	}

	private void putInBucket(Execution execution) {
		StringBuffer directoryPath = getDirectoryPath(execution).append("/");

		// Save config file
		String configPath = new StringBuffer(directoryPath).append(CONFIG_JSON).toString();
		putFile(configPath, execution.getJsonConfiguration());

		// Save status file
		String statusFilePath = new StringBuffer(directoryPath).append(STATUS).toString();
		putFile(statusFilePath, execution.getStatus().toString());
	}

	private PutObjectResult putFile(String filePath, String contents) {
		return s3Client.putObject(executionS3BucketName, filePath,
				new ByteArrayInputStream(contents.getBytes()), new ObjectMetadata());
	}

	private StringBuffer getDirectoryPath(Execution execution) {
		Build build = execution.getBuild();
		String releaseCentreBusinessKey = build.getProduct().getExtension().getReleaseCentre().getBusinessKey();
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append(releaseCentreBusinessKey).append("/");
		stringBuffer.append(build.getCompositeKey()).append("/");
		stringBuffer.append(execution.getCreationTimeString());
		return stringBuffer;
	}

	public void setS3Client(AmazonS3Client s3Client) {
		this.s3Client = s3Client;
	}

	@Required
	public void setExecutionS3BucketName(String executionS3BucketName) {
		this.executionS3BucketName = executionS3BucketName;
	}

}

package org.ihtsdo.buildcloud.dao;

import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import org.ihtsdo.buildcloud.dao.helper.S3ClientHelper;
import org.ihtsdo.buildcloud.dao.helper.S3PutRequestBuilder;
import org.ihtsdo.buildcloud.dao.s3.S3Client;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.stereotype.Repository;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Repository
public class InputFileDAOImpl implements InputFileDAO {

	@Autowired
	private S3Client s3Client;

	@Autowired
	private S3ClientHelper s3ClientHelper;

	private String executionS3BucketName;

	@Override
	public void putFile(InputStream fileStream, long fileSize, String filePath) {
		S3PutRequestBuilder putRequest = s3ClientHelper.newPutRequest(executionS3BucketName, filePath, fileStream).length(fileSize).useBucketAcl();
		s3Client.putObject(putRequest);
	}

	@Override
	public InputStream getFileStream(String filePath) {
		try {
			S3Object s3Object = s3Client.getObject(executionS3BucketName, filePath);
			if (s3Object != null) {
				return s3Object.getObjectContent();
			}
		} catch (AmazonS3Exception e) {
			if (404 != e.getStatusCode()) {
				throw e;
			}
		}
		return null;
	}

	@Override
	public List<String> listFiles(String directoryPath) {
		ObjectListing objectListing = s3Client.listObjects(executionS3BucketName, directoryPath);
		ArrayList<String> files = new ArrayList<>();
		for (S3ObjectSummary summary : objectListing.getObjectSummaries()) {
			files.add(summary.getKey().substring(directoryPath.length()));
		}
		return files;
	}

	@Override
	// TODO: User logging against file actions?
	public void deleteFile(String filePath) {
		s3Client.deleteObject(executionS3BucketName, filePath);
	}

	@Required
	public void setExecutionS3BucketName(String executionS3BucketName) {
		this.executionS3BucketName = executionS3BucketName;
	}

}

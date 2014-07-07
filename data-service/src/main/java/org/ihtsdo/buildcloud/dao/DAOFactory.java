package org.ihtsdo.buildcloud.dao;

import org.ihtsdo.buildcloud.dao.helper.ExecutionS3PathHelper;
import org.ihtsdo.buildcloud.dao.s3.S3Client;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class DAOFactory {
	
	@Autowired
	InputFileDAO inputFileDAO;
	
	@Autowired
	ExecutionDAO executionDAO;
	
	@Autowired
	BuildDAO buildDAO;
	
	@Autowired
	private ExecutionS3PathHelper executionS3PathHelper;
	
	@Autowired
	private S3Client s3Client;
	
	@Autowired
	private String executionBucketName;
	
	@Autowired
	private String publishedBucketName;
	
	static DAOFactory singleton;
	
	DAOFactory() {
		DAOFactory.singleton = this;
	}
	
	public static InputFileDAO getInputFileDAO () { return singleton.inputFileDAO; }
	public static ExecutionDAO getExecutionDAO () { return singleton.executionDAO; }
	public static BuildDAO getBuildDAO() { return singleton.buildDAO; }
	public static ExecutionS3PathHelper getExecutionPathHelper() { return singleton.executionS3PathHelper; }
	public static S3Client getS3Client() { return singleton.s3Client; }
	public static String getExecutionBucketName() { return singleton.executionBucketName; }


}

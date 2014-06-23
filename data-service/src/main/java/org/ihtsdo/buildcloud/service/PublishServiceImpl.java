package org.ihtsdo.buildcloud.service;

import java.io.InputStream;
import java.util.List;

import org.ihtsdo.buildcloud.dao.helper.ExecutionS3PathHelper;
import org.ihtsdo.buildcloud.dao.helper.FileHelper;
import org.ihtsdo.buildcloud.dao.helper.S3ClientHelper;
import org.ihtsdo.buildcloud.dao.s3.S3Client;
import org.ihtsdo.buildcloud.entity.Execution;
import org.ihtsdo.buildcloud.entity.Extension;
import org.ihtsdo.buildcloud.entity.Product;
import org.ihtsdo.buildcloud.entity.ReleaseCenter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.ihtsdo.buildcloud.entity.Package;
import org.ihtsdo.buildcloud.service.file.FileUtils;

@Service
@Transactional
public class PublishServiceImpl implements PublishService {
	
	private FileHelper executionFileHelper;
	private FileHelper publishedFileHelper;
	@Autowired
	private ExecutionS3PathHelper executionS3PathHelper;
	private static final String SEPARATOR = "/";
	
	
	/**
	 * @param executionBucketName
	 * @param publishedBucketName
	 */
	@Autowired
	public PublishServiceImpl(String executionBucketName, String publishedBucketName,
			S3Client s3Client, S3ClientHelper s3ClientHelper){
		executionFileHelper = new FileHelper(executionBucketName, s3Client, s3ClientHelper);
		publishedFileHelper = new FileHelper(publishedBucketName, s3Client, s3ClientHelper);
		
	}

	@Override
	public void publishExecutionPackage(Execution execution, Package pk) {
		
		String pkgOutPutDir = executionS3PathHelper.getExecutionOutputFilesPath(execution, pk.getBusinessKey() ).toString();
		List<String> filesFound = executionFileHelper.listFiles(pkgOutPutDir);
		String releaseFileName = null;
		for( String fileName : filesFound ){
			if ( FileUtils.isZip(fileName))
			{
				releaseFileName = fileName;
				//only one zip file per package
				break;
			}
		}
		if( releaseFileName == null ){
			throw new IllegalStateException("No zip file found in package;"+ pk.getBusinessKey() );
		}
		String fullPath = executionS3PathHelper.getExecutionOutputFilePath(execution, pk.getBusinessKey(), releaseFileName );
		
		InputStream releaseFileInput = executionFileHelper.getFileStream( fullPath);
		String publishedFilePath = getPublishFilePath(execution, releaseFileName);
		publishedFileHelper.putFile(releaseFileInput, publishedFilePath);
		
	}
	
	/**
	 * @param execution 
	 * @param releaseFileName
	 * @return a file structure like
	 * releaseCenter/extension/product/
	 */
	private String getPublishDirPath(Execution execution) {
		Product product = execution.getBuild().getProduct();
		return getPublishDirPath(product);
	}
	
	/**
	 * @param execution 
	 * @param releaseFileName
	 * @return a file structure like
	 * releaseCenter/extension/product/
	 */
	private String getPublishDirPath(Product product) {
		
		Extension extension = product.getExtension();
		ReleaseCenter releaseCenter =  extension.getReleaseCenter();
		StringBuffer path = new StringBuffer();
		path.append(releaseCenter.getBusinessKey());
		path.append(SEPARATOR);
		path.append(extension.getBusinessKey());
		path.append(SEPARATOR);
		path.append(product.getBusinessKey());
		path.append(SEPARATOR);
		return path.toString();
	}
	
	/**
	 * @param execution 
	 * @param releaseFileName
	 * @return a file structure like
	 * releaseCenter/extension/product/releaseFileName.zip
	 */
	private String getPublishFilePath(Execution execution, String releaseFileName) {
		return getPublishDirPath(execution) + releaseFileName;
	}

	public List<String> getPublishedPackages(Product product) {
		return publishedFileHelper.listFiles(getPublishDirPath(product));
	}
}

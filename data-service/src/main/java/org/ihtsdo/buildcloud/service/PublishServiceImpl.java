package org.ihtsdo.buildcloud.service;

import org.ihtsdo.buildcloud.dao.ProductDAO;
import org.ihtsdo.buildcloud.dao.helper.ExecutionS3PathHelper;
import org.ihtsdo.buildcloud.dao.helper.FileHelper;
import org.ihtsdo.buildcloud.dao.helper.S3ClientHelper;
import org.ihtsdo.buildcloud.dao.s3.S3Client;
import org.ihtsdo.buildcloud.entity.*;
import org.ihtsdo.buildcloud.entity.Package;
import org.ihtsdo.buildcloud.service.exception.BadRequestException;
import org.ihtsdo.buildcloud.service.exception.ResourceNotFoundException;
import org.ihtsdo.buildcloud.service.file.FileUtils;
import org.ihtsdo.buildcloud.service.helper.CompositeKeyHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.List;

@Service
@Transactional
public class PublishServiceImpl implements PublishService {
	
	private final FileHelper executionFileHelper;
	private final FileHelper publishedFileHelper;
	@Autowired
	private ExecutionS3PathHelper executionS3PathHelper;
	
	@Autowired
	ProductDAO productDAO;
	
	private static final String SEPARATOR = "/";
	private final String publishedBucketName;
	
	/**
	 * @param executionBucketName
	 * @param publishedBucketName
	 */
	@Autowired
	public PublishServiceImpl(String executionBucketName, String publishedBucketName,
			S3Client s3Client, S3ClientHelper s3ClientHelper){
		executionFileHelper = new FileHelper(executionBucketName, s3Client, s3ClientHelper);
		this.publishedBucketName = publishedBucketName;
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
			throw new IllegalStateException("No zip file found for package: "+ pk.getBusinessKey() );
		}
		String outputFileFullPath = executionS3PathHelper.getExecutionOutputFilePath(execution, pk.getBusinessKey(), releaseFileName );
		String publishedFilePath = getPublishFilePath(execution, releaseFileName);
		executionFileHelper.copyFile(outputFileFullPath, publishedBucketName, publishedFilePath);
		
	}
	

	
	/**
	 * @param execution 
	 * @return a file structure like
	 * releaseCenter/extension/product/
	 */
	private String getPublishDirPath(Execution execution) {
		Product product = execution.getBuild().getProduct();
		return getPublishDirPath(product);
	}
	
	/**
	 * @param product
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

	@Override
	public List<String> getPublishedPackages(Product product) {
		return publishedFileHelper.listFiles(getPublishDirPath(product));
	}
	
	@Override
	public boolean exists(Product product, String targetFileName) {
		String path = getPublishDirPath(product) + targetFileName;
		return publishedFileHelper.exists(path);
	}

	@Override
	public void publishPackage(String releaseCenterBusinessKey, String extensionBusinessKey, String productBusinessKey, 
			InputStream inputStream,	String originalFilename, long size, User subject) throws ResourceNotFoundException, BadRequestException {
		Product product = productDAO.find(releaseCenterBusinessKey, extensionBusinessKey, productBusinessKey, subject);
		
		if (product == null) {
			throw new ResourceNotFoundException ("Failed to recover product " + releaseCenterBusinessKey 
													+ "/" + extensionBusinessKey
													+ "/" + productBusinessKey);
		}
		
		//We're expecting a zip file only
		if (!FileUtils.isZip(originalFilename)) {
			throw new BadRequestException ("File " + originalFilename + " is not named as a zip archive");
		}
		
		String path = getPublishDirPath(product) + originalFilename;
		publishedFileHelper.putFile(inputStream, size, path);
		
	}
}

package org.ihtsdo.buildcloud.service;

import java.util.List;

import mockit.Deencapsulation;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Mocked;
import mockit.integration.junit4.JMockit;

import org.ihtsdo.buildcloud.dao.helper.ExecutionS3PathHelper;
import org.ihtsdo.buildcloud.dao.helper.S3ClientHelper;
import org.ihtsdo.buildcloud.dao.s3.S3Client;
import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.Execution;
import org.ihtsdo.buildcloud.entity.Extension;
import org.ihtsdo.buildcloud.entity.Package;
import org.ihtsdo.buildcloud.entity.Product;
import org.ihtsdo.buildcloud.entity.ReleaseCenter;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
@RunWith(JMockit.class)
public class PublishServiceImplTest {
    
    private static final String PATH_SEPARATOR = "/";
    private static final String TEST_ZIP = "test.zip";
    private static final String EXECUTION_OUTPUT = "/execution/output/";
    private static final String PUBLISHED_BUCKET_NAME = "publishedBucketName";
    private static final String EXECUTION_BUCKET_NAME = "executionBucketName";
    private PublishServiceImpl publishServiceImpl;
    @Mocked private S3Client s3Client;
    @Mocked private S3ClientHelper s3ClientHelper;
    @Mocked  ExecutionS3PathHelper s3PathHelper;
    
    @Before
    public void setUp(){
	publishServiceImpl = new PublishServiceImpl(EXECUTION_BUCKET_NAME, PUBLISHED_BUCKET_NAME, s3Client, s3ClientHelper);
	Deencapsulation.setField(publishServiceImpl, s3PathHelper);
	//use Deencapsulation in JMockit or use 
	//ReflectionTestUtils.setField(publishServiceImpl, "executionS3PathHelper", s3PathHelper);
    }
    
    @Test
    public void testPublishExecutionPackage( @Injectable final Execution execution, @Injectable final Package pk){
	final Build build = createDummyBuild();
	new Expectations() { {
	    s3PathHelper.getExecutionOutputFilesPath(execution, anyString);
	    returns( new StringBuffer(EXECUTION_OUTPUT));
	    
	    s3Client.listObjects(EXECUTION_BUCKET_NAME, anyString);
	    ObjectListing ol = createDummyObjectListing(EXECUTION_OUTPUT + TEST_ZIP);
	    returns(ol);
	    
	    s3PathHelper.getExecutionOutputFilePath(execution, anyString, anyString);
	    String fileToBePublished = EXECUTION_OUTPUT + "text.zip";
	    returns( fileToBePublished);
	    
	    execution.getBuild();
	    returns(build);
	  
	    s3Client.copyObject(EXECUTION_BUCKET_NAME, fileToBePublished, PUBLISHED_BUCKET_NAME, 
		    getExpectedPublishFileDir(build.getProduct()).toLowerCase() + TEST_ZIP);
	    
	    
	    
	}};
	publishServiceImpl.publishExecutionPackage(execution, pk);
	
    }
    
    private Build createDummyBuild() {
	Build build = new Build("test");
	Product product = new Product("testProduct");
	Extension extension = new Extension("testExtension");
	ReleaseCenter releaseCenter = new ReleaseCenter("International Release Center", "International");
	extension.setReleaseCenter(releaseCenter);
	product.setExtension(extension);
	build.setProduct(product);
	build.getProduct().getExtension().getReleaseCenter().getBusinessKey();
	return build;
    }
    
    private String getExpectedPublishFileDir(Product product){
	 //"international/testextension/testproduct/test.zip"
	    String productName = product.getName();
	    Extension extension = product.getExtension();
	    String extensionName =  extension.getName();
	    String centerName =  extension.getReleaseCenter().getShortName();
	    StringBuilder publishedFilePath = new StringBuilder();
	    publishedFilePath.append(centerName);
	    publishedFilePath.append( PATH_SEPARATOR + extensionName);
	    publishedFilePath.append(PATH_SEPARATOR + productName);
	    publishedFilePath.append(PATH_SEPARATOR);
	    return publishedFilePath.toString();
	   
    }

    @Test
    public void testGetPublishedPackages(){
	final Product product = createDummyBuild().getProduct();
	new Expectations(){{
	    s3Client.listObjects(PUBLISHED_BUCKET_NAME, getExpectedPublishFileDir(product).toLowerCase() );
	    ObjectListing ol = createDummyObjectListing( getExpectedPublishFileDir(product) + TEST_ZIP);
	    returns(ol);
	}};
	List<String> result = publishServiceImpl.getPublishedPackages(product);
	Assert.assertEquals("Published file should be there", TEST_ZIP, result.get(0));
    }

    private ObjectListing createDummyObjectListing( String keyName) {
	ObjectListing ol = new ObjectListing();
	S3ObjectSummary summary = new S3ObjectSummary();
	summary.setKey(keyName);
	ol.getObjectSummaries().add(summary);
	return ol;
    }

}

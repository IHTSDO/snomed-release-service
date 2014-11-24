package org.ihtsdo.buildcloud.service;

import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import mockit.Deencapsulation;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Mocked;
import mockit.integration.junit4.JMockit;
import org.ihtsdo.buildcloud.dao.helper.BuildS3PathHelper;
import org.ihtsdo.buildcloud.dao.helper.S3ClientHelper;
import org.ihtsdo.buildcloud.dao.helper.S3PutRequestBuilder;
import org.ihtsdo.buildcloud.dao.s3.S3Client;
import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.Product;
import org.ihtsdo.buildcloud.entity.ReleaseCenter;
import org.ihtsdo.buildcloud.service.exception.BusinessServiceException;
import org.ihtsdo.buildcloud.test.TestUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.InputStream;
import java.util.List;

@RunWith(JMockit.class)
public class PublishServiceImplTest {

	private static final String TEST_MD5 = "test.md5";
	private static final String PATH_SEPARATOR = "/";
	private static final String TEST_ZIP = "test.zip";
	private static final String BUILD_OUTPUT = "/build/output/";
	private static final String PUBLISHED_BUCKET_NAME = "publishedBucketName";
	private static final String BUILD_BUCKET_NAME = "buildBucketName";
	private PublishServiceImpl publishServiceImpl;
	@Mocked
	private S3Client s3Client;
	@Mocked
	private S3ClientHelper s3ClientHelper;
	@Mocked
	BuildS3PathHelper s3PathHelper;

	@Before
	public void setUp() {
		TestUtils.setTestUser();
		publishServiceImpl = new PublishServiceImpl(BUILD_BUCKET_NAME, PUBLISHED_BUCKET_NAME, s3Client, s3ClientHelper);
		Deencapsulation.setField(publishServiceImpl, s3PathHelper);
		//use Deencapsulation in JMockit or use
		//ReflectionTestUtils.setField(publishServiceImpl, "buildS3PathHelper", s3PathHelper);
	}

	@Test
	public void testPublishBuildPackage(@Injectable final Build build) throws BusinessServiceException {
		final Product product = createDummyProduct();
		new Expectations() {
			{
				build.getUniqueId();
				returns("123");

				s3PathHelper.getBuildOutputFilesPath(build);
				returns(new StringBuilder(BUILD_OUTPUT));

				s3Client.listObjects(BUILD_BUCKET_NAME, anyString);
				ObjectListing ol = createDummyObjectListing(BUILD_OUTPUT);
				returns(ol);

				s3PathHelper.getBuildOutputFilePath(build, anyString);
				String fileToBePublished = BUILD_OUTPUT + "text.zip";
				returns(fileToBePublished);

				build.getProduct();
				returns(product);

				String zipKey = getExpectedPublishFileDir(product).toLowerCase() + TEST_ZIP;
				s3Client.copyObject(BUILD_BUCKET_NAME, fileToBePublished, PUBLISHED_BUCKET_NAME,
						zipKey);

				s3Client.getObject(PUBLISHED_BUCKET_NAME, zipKey);
				S3Object s3Object = new S3Object();
				s3Object.setObjectContent(getClass().getResourceAsStream("/test.zip"));
				returns(s3Object);

				s3ClientHelper.newPutRequest(anyString, anyString, withInstanceOf(InputStream.class));
				returns(new S3PutRequestBuilder(null, null, null, s3ClientHelper));

				s3ClientHelper.useBucketAcl(withInstanceOf(PutObjectRequest.class));

				s3Client.putObject(withInstanceOf(PutObjectRequest.class));
				returns(null);

				s3PathHelper.getBuildOutputFilePath(build, anyString);
				String md5fileToBePublished = BUILD_OUTPUT + TEST_MD5;
				returns(md5fileToBePublished);

				build.getProduct();
				returns(product);

				String md5 = getExpectedPublishFileDir(product).toLowerCase() + TEST_MD5;
				s3Client.copyObject(BUILD_BUCKET_NAME, md5fileToBePublished, PUBLISHED_BUCKET_NAME,
						md5);
			}
		};
		publishServiceImpl.publishBuild(build);

	}

	private Product createDummyProduct() {
		Product product = new Product("test");
		ReleaseCenter releaseCenter = new ReleaseCenter("International Release Center", "International");
		product.setReleaseCenter(releaseCenter);
		return product;
	}

	private String getExpectedPublishFileDir(final Product product) {
		//"international/test.zip"
		return product.getReleaseCenter().getBusinessKey() + PATH_SEPARATOR;

	}

	@Test
	public void testGetPublishedPackages() {
		final Product product = createDummyProduct();
		new Expectations() {{
			s3Client.listObjects(PUBLISHED_BUCKET_NAME, getExpectedPublishFileDir(product).toLowerCase());
			ObjectListing ol = createDummyObjectListing(getExpectedPublishFileDir(product));
			returns(ol);
		}};
		List<String> result = publishServiceImpl.getPublishedPackages(product.getReleaseCenter());
		Assert.assertEquals("Text file should be filtered out.", 1, result.size());
		Assert.assertEquals("Published file should be there", TEST_ZIP, result.get(0));
	}

	private ObjectListing createDummyObjectListing(final String dirName) {
		ObjectListing ol = new ObjectListing();
		ol.getObjectSummaries().add(getS3ObjectSummary(dirName + TEST_ZIP));
		ol.getObjectSummaries().add(getS3ObjectSummary(dirName + "test/file.txt"));
		ol.getObjectSummaries().add( getS3ObjectSummary(dirName + TEST_MD5));
		return ol;
	}

	private S3ObjectSummary getS3ObjectSummary(final String key) {
		S3ObjectSummary summary = new S3ObjectSummary();
		summary.setKey(key);
		return summary;
	}

}
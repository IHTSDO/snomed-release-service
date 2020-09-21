package org.ihtsdo.buildcloud.dao;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.MockType;
import org.easymock.internal.MocksControl;
import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.Product;
import org.ihtsdo.buildcloud.test.TestUtils;
import org.ihtsdo.otf.dao.s3.S3Client;
import org.ihtsdo.otf.dao.s3.S3ClientFactory;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3ObjectSummary;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"/test/testDataServiceContext.xml"})
@Transactional
public class BuildDAOImplTest {
	
	public static final String TEST_FILE_NAME = "der2_Refset_SimpleDelta_INT_20140831.txt";

	@Autowired
	protected BuildDAOImpl buildDAO;

	@Autowired
	private S3ClientFactory factory;

	@Autowired
	protected ProductDAO productDAO;

	@Autowired
	private S3Client s3Client;

	private Product product;
	protected Build build;
	private MocksControl mocksControl;
	private S3Client mockS3Client;

	@Before
	public void setup() {
		mocksControl = new MocksControl(MockType.DEFAULT);
		this.mockS3Client = mocksControl.createMock(S3Client.class);
		buildDAO.setS3Client(mockS3Client);

		product = productDAO.find(1L);
		final Date creationTime = new GregorianCalendar(2014, 1, 4, 10, 30, 1).getTime();
		build = new Build(creationTime, product);
	}

	@After
	public void tearDown() {
		//Need to return the buildDAO to it's original state for other unitTests which expect to use the Offline client
		buildDAO.setS3Client(s3Client);
	}

	@Test
	public void testSave() throws IOException {
		final Capture<String> configPathCapture = new Capture<>();
		final Capture<InputStream> configJsonStreamCapture = new Capture<>();
		EasyMock.expect(mockS3Client.putObject(EasyMock.isA(String.class), EasyMock.capture(configPathCapture), EasyMock.capture(configJsonStreamCapture), EasyMock.isA(ObjectMetadata.class))).andReturn(null);
		final Capture<String> qaConfigPathCapture = new Capture<>();
		final Capture<InputStream> qaConfigJsonStreamCapture = new Capture<>();
		EasyMock.expect(mockS3Client.putObject(EasyMock.isA(String.class), EasyMock.capture(qaConfigPathCapture), EasyMock.capture(qaConfigJsonStreamCapture), EasyMock.isA(ObjectMetadata.class))).andReturn(null);

		final Capture<String> statusPathCapture = new Capture<>();
		EasyMock.expect(mockS3Client.putObject(EasyMock.isA(String.class), EasyMock.capture(statusPathCapture), EasyMock.isA(InputStream.class), EasyMock.isA(ObjectMetadata.class))).andReturn(null);

		mocksControl.replay();
		buildDAO.save(build);
		mocksControl.verify();

		Assert.assertEquals("international/" + product.getBusinessKey() + "/2014-02-04T10:30:01/configuration.json", configPathCapture.getValue());
		String configJson = TestUtils.readStream(new FileInputStream(buildDAO.toJson(build.getConfiguration())));
		Assert.assertEquals("{" +
				"\"readmeHeader\":null," +
				"\"readmeEndDate\":null," +
				"\"firstTimeRelease\":false," +
				"\"betaRelease\":false," +
				"\"previousPublishedPackage\":null," +
				"\"newRF2InputFiles\":null," +
				"\"justPackage\":false," +
				"\"workbenchDataFixesRequired\":false," +
				"\"inputFilesFixesRequired\":false," +
				"\"createLegacyIds\":false," +
				"\"extensionConfig\":null," +
				"\"includePrevReleaseFiles\":null," +
				"\"dailyBuild\":false," +
				"\"classifyOutputFiles\":false," +
				"\"licenceStatement\":null," +
				"\"releaseInformationFields\":null," +
				"\"useClassifierPreConditionChecks\":false," +
				"\"conceptPreferredTerms\":null," +
				"\"defaultBranchPath\":null," +
				"\"branchPath\":null," +
				"\"buildName\":null," +
				"\"exportType\":null," +
				"\"customRefsetCompositeKeys\":{}," +
				"\"effectiveTime\":null" +
				"}", configJson);

		Assert.assertEquals("international/" + product.getBusinessKey() + "/2014-02-04T10:30:01/qa-test-config.json", qaConfigPathCapture.getValue());
		Assert.assertEquals("international/" + product.getBusinessKey() + "/2014-02-04T10:30:01/status:BEFORE_TRIGGER", statusPathCapture.getValue());
	}

	@Test
	public void testFindAllDesc() {
		final ObjectListing objectListing = new ObjectListing();
		addObjectSummary(objectListing, "international/" + product.getBusinessKey() + "/2014-02-04T10:30:01/configuration.json");
		addObjectSummary(objectListing, "international/" + product.getBusinessKey() + "/2014-02-04T10:30:01/status:BEFORE_TRIGGER");
		addObjectSummary(objectListing, "international/" + product.getBusinessKey() + "/2014-03-04T10:30:01/configuration.json");
		addObjectSummary(objectListing, "international/" + product.getBusinessKey() + "/2014-03-04T10:30:01/status:BEFORE_TRIGGER");
		EasyMock.expect(mockS3Client.listObjects(EasyMock.isA(ListObjectsRequest.class))).andReturn(objectListing);

		mocksControl.replay();
		final List<Build> all = buildDAO.findAllDesc(product, null, null);
		mocksControl.verify();

		Assert.assertEquals(2, all.size());

		Assert.assertEquals("2014-03-04T10:30:01", all.get(0).getCreationTime());
		Assert.assertEquals(Build.Status.BEFORE_TRIGGER, all.get(0).getStatus());

		Assert.assertEquals("2014-02-04T10:30:01", all.get(1).getCreationTime());
		Assert.assertEquals(Build.Status.BEFORE_TRIGGER, all.get(1).getStatus());
	}

	@Test
	public void testFindNone() {
		final String buildId = "2014-02-04T10:30:01";
		final ObjectListing objectListing = new ObjectListing();
		final Capture<ListObjectsRequest> listObjectsRequestCapture = new Capture<>();
		EasyMock.expect(mockS3Client.listObjects(EasyMock.capture(listObjectsRequestCapture))).andReturn(objectListing);

		mocksControl.replay();
		final Build foundBuild = buildDAO.find(product, buildId);
		mocksControl.verify();

		Assert.assertNull(foundBuild);
	}

	@Test
	public void testFindOne() {
		final String buildId = "2014-02-04T10:30:01";
		final ObjectListing objectListing = new ObjectListing();
		addObjectSummary(objectListing, "international/" + product.getBusinessKey() + "/" + buildId + "/configuration.json");
		addObjectSummary(objectListing, "international/" + product.getBusinessKey() + "/" + buildId + "/status:BEFORE_TRIGGER");
		final Capture<ListObjectsRequest> listObjectsRequestCapture = new Capture<>();
		EasyMock.expect(mockS3Client.listObjects(EasyMock.capture(listObjectsRequestCapture))).andReturn(objectListing);

		mocksControl.replay();
		final Build foundBuild = buildDAO.find(product, buildId);
		mocksControl.verify();

		Assert.assertNotNull(foundBuild);

		Assert.assertEquals("2014-02-04T10:30:01", foundBuild.getCreationTime());
		Assert.assertEquals(Build.Status.BEFORE_TRIGGER, foundBuild.getStatus());
	}
	
	
	@Test
	public void testPutOutputFile() throws Exception {

		//Leaving this as offline to remove external dependency, but set to true to check Amazon is happy with our MD5 Encoding.
		final boolean offlineMode = true;
		final S3Client s3Client1 = factory.getClient(offlineMode);
		buildDAO.setS3Client(s3Client1);
		final String testFile = getClass().getResource("/org/ihtsdo/buildcloud/service/build/"+ TEST_FILE_NAME).getFile();

		final boolean calcMD5 = true;
		final String md5Received = buildDAO.putOutputFile(build, new File(testFile), calcMD5);

		System.out.println(md5Received);

		//Amazon are expecting the md5 to be xWJD6+IqEtukiwI9rz4pNw==
		//Offline test is just going to return the MD5 input, so this test only makes sense in online mode.
		final byte[] md5BytesExpected = Base64.decodeBase64("xWJD6+IqEtukiwI9rz4pNw==");
		final byte[] md5BytesReceived =  Base64.decodeBase64(md5Received);
		Assert.assertArrayEquals(md5BytesExpected, md5BytesReceived);
		
		//Now lets see if we can get that file back out again
		final InputStream is = buildDAO.getOutputFileInputStream(build, TEST_FILE_NAME);
		Assert.assertNotNull(is);
	}

	private void addObjectSummary(final ObjectListing objectListing, final String path) {
		final S3ObjectSummary summary = new S3ObjectSummary();
		summary.setKey(path);
		objectListing.getObjectSummaries().add(summary);
	}
	
}

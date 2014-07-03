package org.ihtsdo.buildcloud.dao;

import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import org.apache.commons.codec.binary.Base64;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.MockType;
import org.easymock.internal.MocksControl;
import org.ihtsdo.buildcloud.dao.s3.S3Client;
import org.ihtsdo.buildcloud.dao.s3.S3ClientFactory;
import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.Execution;
import org.ihtsdo.buildcloud.entity.Package;
import org.ihtsdo.buildcloud.entity.helper.TestEntityGenerator;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"/test/testDataServiceContext.xml"})
@Transactional
public class ExecutionDAOImplTest {
	
	public static final String TEST_FILE_NAME = "der2_Refset_SimpleDelta_INT_20140831.txt";

	@Autowired
	protected ExecutionDAOImpl executionDAO;

	@Autowired
	private S3ClientFactory factory;

	@Autowired
	protected BuildDAO buildDAO;

	private Build build;
	protected Execution execution;
	private MocksControl mocksControl;
	private S3Client mockS3Client;

	@Before
	public void setup() {
		mocksControl = new MocksControl(MockType.DEFAULT);
		this.mockS3Client = mocksControl.createMock(S3Client.class);
		executionDAO.setS3Client(mockS3Client);

		build = buildDAO.find(1L, TestEntityGenerator.TEST_USER);
		Date creationTime = new GregorianCalendar(2014, 1, 4, 10, 30, 01).getTime();
		execution = new Execution(creationTime, build);
	}

	@Test
	public void testSave() {

		Capture<String> configPathCapture = new Capture<>();
		EasyMock.expect(mockS3Client.putObject(EasyMock.isA(String.class), EasyMock.capture(configPathCapture), EasyMock.isA(InputStream.class), EasyMock.isA(ObjectMetadata.class))).andReturn(null);

		Capture<String> statusPathCapture = new Capture<>();
		EasyMock.expect(mockS3Client.putObject(EasyMock.isA(String.class), EasyMock.capture(statusPathCapture), EasyMock.isA(InputStream.class), EasyMock.isA(ObjectMetadata.class))).andReturn(null);

		mocksControl.replay();
		executionDAO.save(execution, "");
		mocksControl.verify();

		Assert.assertEquals("international/" + build.getCompositeKey() + "/2014-02-04T10:30:01/configuration.json", configPathCapture.getValue());
		Assert.assertEquals("international/" + build.getCompositeKey() + "/2014-02-04T10:30:01/status:BEFORE_TRIGGER", statusPathCapture.getValue());
	}

	@Test
	public void testFindAll() {
		ObjectListing objectListing = new ObjectListing();
		addObjectSummary(objectListing, "international/" + build.getCompositeKey() + "/2014-02-04T10:30:01/configuration.json");
		addObjectSummary(objectListing, "international/" + build.getCompositeKey() + "/2014-02-04T10:30:01/status:BEFORE_TRIGGER");
		addObjectSummary(objectListing, "international/" + build.getCompositeKey() + "/2014-03-04T10:30:01/configuration.json");
		addObjectSummary(objectListing, "international/" + build.getCompositeKey() + "/2014-03-04T10:30:01/status:BEFORE_TRIGGER");
		EasyMock.expect(mockS3Client.listObjects(EasyMock.isA(ListObjectsRequest.class))).andReturn(objectListing);

		mocksControl.replay();
		ArrayList<Execution> all = executionDAO.findAll(build);
		mocksControl.verify();

		Assert.assertEquals(2, all.size());

		Assert.assertEquals("2014-02-04T10:30:01", all.get(0).getCreationTime());
		Assert.assertEquals(Execution.Status.BEFORE_TRIGGER, all.get(0).getStatus());

		Assert.assertEquals("2014-03-04T10:30:01", all.get(1).getCreationTime());
		Assert.assertEquals(Execution.Status.BEFORE_TRIGGER, all.get(1).getStatus());
	}

	@Test
	public void testFindNone() {
		String executionId = "2014-02-04T10:30:01";
		ObjectListing objectListing = new ObjectListing();
		Capture<ListObjectsRequest> listObjectsRequestCapture = new Capture<>();
		EasyMock.expect(mockS3Client.listObjects(EasyMock.capture(listObjectsRequestCapture))).andReturn(objectListing);

		mocksControl.replay();
		Execution foundExecution = executionDAO.find(build, executionId);
		mocksControl.verify();

		Assert.assertNull(foundExecution);
	}

	@Test
	public void testFindOne() {
		String executionId = "2014-02-04T10:30:01";
		ObjectListing objectListing = new ObjectListing();
		addObjectSummary(objectListing, "international/" + build.getCompositeKey() + "/" + executionId + "/configuration.json");
		addObjectSummary(objectListing, "international/" + build.getCompositeKey() + "/" + executionId + "/status:BEFORE_TRIGGER");
		Capture<ListObjectsRequest> listObjectsRequestCapture = new Capture<>();
		EasyMock.expect(mockS3Client.listObjects(EasyMock.capture(listObjectsRequestCapture))).andReturn(objectListing);

		mocksControl.replay();
		Execution foundExecution = executionDAO.find(build, executionId);
		mocksControl.verify();

		Assert.assertNotNull(foundExecution);

		Assert.assertEquals("2014-02-04T10:30:01", foundExecution.getCreationTime());
		Assert.assertEquals(Execution.Status.BEFORE_TRIGGER, foundExecution.getStatus());
	}
	
	
	@Test
	public void testPutOutputFile() throws Exception {

		//Leaving this as offline to remove external dependency, but set to true to check Amazon is happy with our MD5 Encoding.
		boolean offlineMode = true; 
		S3Client onlineS3Client = factory.getClient(offlineMode);
		executionDAO.setS3Client(onlineS3Client);
		
		Package pkg = execution.getBuild().getPackages().iterator().next();
		String testFile = getClass().getResource("/org/ihtsdo/buildcloud/service/execution/"+ TEST_FILE_NAME).getFile();
		boolean calcMD5 = true;
		String md5Received = executionDAO.putOutputFile(execution, pkg, new File(testFile), "", calcMD5);
		
		//Amazon are expecting the md5 to be g8tgi4y8+ABULBMAbgodiA==
		//Offline test is just going to return the MD5 input, so this test only makes sense in online mode.
		byte[] md5BytesExpected = Base64.decodeBase64("g8tgi4y8+ABULBMAbgodiA==");
		byte[] md5BytesReceived =  Base64.decodeBase64(md5Received);
		Assert.assertArrayEquals(md5BytesExpected, md5BytesReceived);
		
		//Now lets see if we can get that file back out again
		InputStream is = executionDAO.getOutputFileInputStream(execution, pkg, TEST_FILE_NAME);
		Assert.assertNotNull(is);
	}

	private void addObjectSummary(ObjectListing objectListing, String path) {
		S3ObjectSummary summary = new S3ObjectSummary();
		summary.setKey(path);
		objectListing.getObjectSummaries().add(summary);
	}
	
}

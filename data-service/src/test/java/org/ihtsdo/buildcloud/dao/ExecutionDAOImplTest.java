package org.ihtsdo.buildcloud.dao;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.MockType;
import org.easymock.internal.MocksControl;
import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.Execution;
import org.ihtsdo.test.DummyHazelcastQueue;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"/applicationContext.xml"})
@Transactional
public class ExecutionDAOImplTest {

	@Autowired
	private ExecutionDAOImpl executionDAO;

	@Autowired
	private BuildDAO buildDAO;

	@Autowired
	private DummyHazelcastQueue<String> dummyBuildQueue;

	private Build build;
	private Execution execution;
	private MocksControl mocksControl;
	private AmazonS3Client mockS3Client;

	@Before
	public void setup() {
		mocksControl = new MocksControl(MockType.DEFAULT);
		this.mockS3Client = mocksControl.createMock(AmazonS3Client.class);
		executionDAO.setS3Client(mockS3Client);

		build = buildDAO.find(1L, "test");
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
	public void testQueueForBuilding() {
		execution.setStatus(Execution.Status.BEFORE_TRIGGER);

		Capture<String> oldStatusPathCapture = new Capture<>();
		Capture<String> newStatusPathCapture = new Capture<>();
		mockS3Client.deleteObject(EasyMock.isA(String.class), EasyMock.capture(oldStatusPathCapture));
		EasyMock.expect(mockS3Client.putObject(EasyMock.isA(String.class), EasyMock.capture(newStatusPathCapture), EasyMock.isA(InputStream.class), EasyMock.isA(ObjectMetadata.class))).andReturn(null);

		mocksControl.replay();
		executionDAO.queueForBuilding(execution);
		mocksControl.verify();

		Assert.assertEquals("international/" + build.getCompositeKey() + "/2014-02-04T10:30:01/status:BEFORE_TRIGGER", oldStatusPathCapture.getValue());
		Assert.assertEquals("international/" + build.getCompositeKey() + "/2014-02-04T10:30:01/status:QUEUED", newStatusPathCapture.getValue());
		List<String> capturedQueueItems = dummyBuildQueue.getCapturedQueueItems();
		Assert.assertEquals(1, capturedQueueItems.size());
		Assert.assertEquals("http://localhost/api/v1/builds/1/executions/2014-02-04T10:30:01/", capturedQueueItems.get(0));
	}

	private void addObjectSummary(ObjectListing objectListing, String path) {
		S3ObjectSummary summary = new S3ObjectSummary();
		summary.setKey(path);
		objectListing.getObjectSummaries().add(summary);
	}

}

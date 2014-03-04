package org.ihtsdo.buildcloud.dao;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.MockType;
import org.easymock.internal.MocksControl;
import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.Execution;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.Date;
import java.util.GregorianCalendar;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"/applicationContext.xml"})
@Transactional
public class ExecutionDAOImplTest {

	@Autowired
	private ExecutionDAOImpl executionDAO;

	@Autowired
	private BuildDAO buildDAO;
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
		execution = new Execution(creationTime, "", build);
	}

	@Test
	public void testSave() {

		Capture<String> configPathCapture = new Capture<String>();
		EasyMock.expect(mockS3Client.putObject(EasyMock.isA(String.class), EasyMock.capture(configPathCapture), EasyMock.isA(InputStream.class), EasyMock.isA(ObjectMetadata.class))).andReturn(null);

		Capture<String> statusPathCapture = new Capture<String>();
		EasyMock.expect(mockS3Client.putObject(EasyMock.isA(String.class), EasyMock.capture(statusPathCapture), EasyMock.isA(InputStream.class), EasyMock.isA(ObjectMetadata.class))).andReturn(null);

		mocksControl.replay();
		executionDAO.save(execution);
		mocksControl.verify();

		Assert.assertEquals("international/1_20130731_international_release/2014-02-04T10:30:01/config.json", configPathCapture.getValue());
		Assert.assertEquals("international/1_20130731_international_release/2014-02-04T10:30:01/status", statusPathCapture.getValue());
	}

}

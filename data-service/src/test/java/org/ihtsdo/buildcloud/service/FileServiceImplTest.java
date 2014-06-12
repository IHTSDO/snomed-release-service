package org.ihtsdo.buildcloud.service;

import org.easymock.MockType;
import org.easymock.internal.MocksControl;
import org.ihtsdo.buildcloud.dao.BuildDAO;
import org.ihtsdo.buildcloud.dao.s3.S3Client;
import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.Execution;
import org.ihtsdo.buildcloud.entity.helper.TestEntityGenerator;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.GregorianCalendar;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"/test/testDataServiceContext.xml"})
@Transactional
public class FileServiceImplTest {
	
	public static final String TEST_FILE_NAME = "der2_Refset_SimpleDelta_INT_20140831.txt";

	@Autowired
	protected FileService fileService;
	
	@Autowired
	protected BuildDAO buildDAO;
	
	protected Build build;
	protected Execution execution;
	
	private MocksControl mocksControl;
	private S3Client mockS3Client;
	
	@Before
	public void setup() {
		mocksControl = new MocksControl(MockType.DEFAULT);
		mockS3Client = mocksControl.createMock(S3Client.class);
		//executionDAO.setS3Client(mockS3Client);

		build = buildDAO.find(1L, TestEntityGenerator.TEST_USER);
		Date creationTime = new GregorianCalendar(2014, 1, 4, 10, 30, 01).getTime();
		execution = new Execution(creationTime, build);
	}

	@Test
	public void testExecutionOutputFileOutputStream() throws IOException {
		OutputStream outputStream = fileService.getExecutionFileOutputStream("out.txt");
		Assert.assertNotNull(outputStream);
		outputStream.close();
	}
	
}

package org.ihtsdo.buildcloud.service;

import org.easymock.MockType;
import org.easymock.internal.MocksControl;
import org.ihtsdo.buildcloud.dao.BuildDAO;
import org.ihtsdo.buildcloud.dao.s3.S3Client;
import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.Execution;
import org.ihtsdo.buildcloud.entity.Package;
import org.ihtsdo.buildcloud.entity.helper.EntityHelper;
import org.ihtsdo.buildcloud.entity.helper.TestEntityGenerator;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.GregorianCalendar;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"/applicationContext.xml"})
@Transactional
public class FileServiceImplTest {
	
	public static final String TEST_FILE_NAME = "der2_Refset_SimpleDelta_INT_20140831.txt";

	@Autowired
	private FileService fileService;
	
	@Autowired
	private BuildDAO buildDAO;
	
	private Build build;
	private Execution execution;
	
	private MocksControl mocksControl;
	private S3Client mockS3Client;
	
	@Before
	public void setup() {
		mocksControl = new MocksControl(MockType.DEFAULT);
		this.mockS3Client = mocksControl.createMock(S3Client.class);
		//executionDAO.setS3Client(mockS3Client);

		build = buildDAO.find(1L, TestEntityGenerator.TEST_USER);
		Date creationTime = new GregorianCalendar(2014, 1, 4, 10, 30, 01).getTime();
		execution = new Execution(creationTime, build);
	}

	@Test
	public void testExecutionOutputFileOutputStream() throws IOException {
		OutputStream outputStream = fileService.getExecutionOutputFileOutputStream("out.txt");
		Assert.assertNotNull(outputStream);
		outputStream.close();
	}
	
	@Test
	public void testPutOutputFile() throws FileNotFoundException, IOException {
		
		Package pkg = execution.getBuild().getPackages().get(0);
		String testFile = getClass().getResource("/org/ihtsdo/buildcloud/service/execution/"+ TEST_FILE_NAME).getFile();
		boolean calcMD5 = false;
		fileService.putOutputFile(execution, pkg, new File(testFile), calcMD5);
		
		//Now lets see if we can get that file back out again
		OutputStream os = fileService.getExecutionOutputFileOutputStream(execution, pkg.getBusinessKey(), TEST_FILE_NAME);
		Assert.assertNotNull(os);
	}

}

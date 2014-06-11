package org.ihtsdo.buildcloud.service;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Base64;
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
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Base64;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * This test will call the actual AWS S3 Service, so we make it manual to ensure
 * our build system will not have a dependency on an external system.  However we
 * can still manually check that Amazon is happy with our MD5 Encoding!
 * @author Peter
 *
 */
public class FileServiceImplTestManual extends FileServiceImplTest {
	
	@Test
	public void testPutOutputFile() throws FileNotFoundException, IOException, NoSuchAlgorithmException, DecoderException {
		Assert.fail(); // Enable this line to ensure automatic checks aren't picking up this Manual test class
		Package pkg = execution.getBuild().getPackages().get(0);
		String testFile = getClass().getResource("/org/ihtsdo/buildcloud/service/execution/"+ TEST_FILE_NAME).getFile();
		boolean calcMD5 = true;
		String md5Received = fileService.putOutputFile(execution, pkg, new File(testFile), calcMD5);
		
		//Amazon are expecting the md5 to be g8tgi4y8+ABULBMAbgodiA==
		byte[] md5BytesExpected = Base64.decodeBase64("g8tgi4y8+ABULBMAbgodiA==");
		byte[] md5BytesReceived =  Base64.decodeBase64(md5Received);
		Assert.assertArrayEquals(md5BytesExpected, md5BytesReceived);
		
		//Now lets see if we can get that file back out again
		OutputStream os = fileService.getExecutionOutputFileOutputStream(execution, pkg.getBusinessKey(), TEST_FILE_NAME);
		Assert.assertNotNull(os);
	}

}

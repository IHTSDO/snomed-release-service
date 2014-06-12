package org.ihtsdo.buildcloud.dao;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Base64;
import org.ihtsdo.buildcloud.dao.s3.S3Client;
import org.ihtsdo.buildcloud.dao.s3.S3ClientFactory;
import org.ihtsdo.buildcloud.entity.Package;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;

/**
 * This test will call the actual AWS S3 Service, so we make it manual to ensure
 * our build system will not have a dependency on an external system.  However we
 * can still manually check that Amazon is happy with our MD5 Encoding!
 * @author Peter
 *
 */
public class ExecutionDAOImplTestManual extends ExecutionDAOImplTest {
	
	public static final String TEST_FILE_NAME = "der2_Refset_SimpleDelta_INT_20140831.txt";
	
	@Autowired
	private S3ClientFactory factory;
	
	@Test
	public void testPutOutputFile() throws Exception, FileNotFoundException, IOException, NoSuchAlgorithmException, DecoderException {
		

		boolean offlineMode = false; //We're going to use the online AmazonS3 for this test to ensure it's happy with our MD5 Encoding
		S3Client onlineS3Client = factory.getClient(offlineMode);
		executionDAO.setS3Client(onlineS3Client);
		
		Package pkg = execution.getBuild().getPackages().get(0);
		String testFile = getClass().getResource("/org/ihtsdo/buildcloud/service/execution/"+ TEST_FILE_NAME).getFile();
		boolean calcMD5 = true;
		String md5Received = executionDAO.putOutputFile(execution, pkg, new File(testFile), "", calcMD5);
		
		//Amazon are expecting the md5 to be g8tgi4y8+ABULBMAbgodiA==
		byte[] md5BytesExpected = Base64.decodeBase64("g8tgi4y8+ABULBMAbgodiA==");
		byte[] md5BytesReceived =  Base64.decodeBase64(md5Received);
		Assert.assertArrayEquals(md5BytesExpected, md5BytesReceived);
		
		//Now lets see if we can get that file back out again
		InputStream is = executionDAO.getOutputFileInputStream(execution, pkg, TEST_FILE_NAME);
		Assert.assertNotNull(is);
	}

}

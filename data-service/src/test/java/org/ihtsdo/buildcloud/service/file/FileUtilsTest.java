package org.ihtsdo.buildcloud.service.file;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;

public class FileUtilsTest {

	@Test
	public void test() throws URISyntaxException, NoSuchAlgorithmException, IOException {
		String testFilePath = "/org/ihtsdo/buildcloud/service/execution/der2_Refset_SimpleDelta_INT_20140831.txt";
		URL testFileURL = getClass().getResource(testFilePath);
		File testFile = new File(testFileURL.toURI());
		
		//Normal command line utils use a hex format - base 16
		String md5 = FileUtils.calculateMD5(testFile);
		Assert.assertNotNull(md5);
		
		//If we're running on Unix, we can compare that to the stock method of generating an MD5 
		//This would need Apache Commons Exec and it's such a bad idea that I'm not going to do it.
		//Have copied MD5 generated from the Mac command line instead.
		Assert.assertEquals ("c56243ebe22a12dba48b023daf3e2937", md5);
		/*
		if (SystemUtils.IS_OS_UNIX) {
			String line = "md5 " + testFile.getAbsolutePath();
			CommandLine cmdLine = CommandLine.parse(line);
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			executor.setStreamHandler(new PumpStreamHandler(output));
			DefaultExecutor executor = new DefaultExecutor();
			int exitValue = executor.execute(cmdLine);
		}*/
	}

}

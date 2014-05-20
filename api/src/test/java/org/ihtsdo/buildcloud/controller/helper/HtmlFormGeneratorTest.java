package org.ihtsdo.buildcloud.controller.helper;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;
import java.text.ParseException;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HtmlFormGeneratorTest {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(HtmlFormGeneratorTest.class);
	
	public static final String TEST_XSD_FILE = "/file-test-defn.xsd";
	public static final String INVALID_XSD_FILE = "/invalid-file-test-defn.xsd";

	@Test
	public void testValidFile() throws Exception{

		HtmlFormGenerator generator = new HtmlFormGenerator(getTestFile(TEST_XSD_FILE));
		String output = generator.generateHTML();
		LOGGER.debug (output);
		Assert.assertNotNull(output);
	}
	
	@Test
	public void testInvalidFile() {
		Exception actualExecptionCaught = null;
		
		try{
			HtmlFormGenerator generator = new HtmlFormGenerator(getTestFile(INVALID_XSD_FILE));
			String output = generator.generateHTML();
			LOGGER.debug (output);
			Assert.assertNotNull(output);			
		} catch (Exception e) {
			actualExecptionCaught = e;
		}
		
		Assert.assertNotNull(actualExecptionCaught);
		Assert.assertTrue(actualExecptionCaught instanceof ParseException);
	}
	
	private File getTestFile(String fileLocation) throws FileNotFoundException{

		File testFile = null;
		
		URL fileURL = getClass().getResource(fileLocation);
		
		if (fileURL != null) {
			testFile = new File(fileURL.getFile());
		}

		if (testFile == null || !testFile.exists()) {
			LOGGER.warn("Failed to recover test resource " + fileLocation + " from: " + getClass().getResource("/.").getPath());
			throw new FileNotFoundException();
		}

		return testFile;
	}

}

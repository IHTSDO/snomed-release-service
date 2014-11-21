package org.ihtsdo.buildcloud.service.execution;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.io.FilenameUtils;
import org.ihtsdo.buildcloud.dao.BuildDAO;
import org.ihtsdo.buildcloud.dao.ExecutionDAOImpl;
import org.ihtsdo.buildcloud.dao.helper.ExecutionS3PathHelper;
import org.ihtsdo.buildcloud.dao.helper.FileHelper;
import org.ihtsdo.buildcloud.dao.helper.S3ClientHelper;
import org.ihtsdo.buildcloud.dao.s3.S3Client;
import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.Execution;
import org.ihtsdo.buildcloud.service.exception.ResourceNotFoundException;
import org.ihtsdo.buildcloud.service.file.FileUtils;
import org.ihtsdo.buildcloud.test.TestUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Map;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"/test/testDataServiceContext.xml"})
@Transactional
public class ZipperTest {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ZipperTest.class);

	@Autowired
	private ExecutionS3PathHelper pathHelper;
	
	@Autowired
	protected BuildDAO buildDAO;

	@Autowired
	private ExecutionDAOImpl executionDAO;

	@Autowired
	private String executionBucketName;
	
	@Autowired 
	S3Client s3client;
	
	protected Execution execution;
	
	@Before
	public void setup() throws NoSuchAlgorithmException, IOException, DecoderException, InterruptedException {
		Build build = buildDAO.find(1L, TestUtils.TEST_USER);
		Thread.sleep(1000);// Make execution unique
		execution = new Execution(new Date(), build);

		//We're going to locally copy a manifest file for the test
		FileHelper fileHelper = new FileHelper(executionBucketName, s3client, new S3ClientHelper(s3client));
		String executionManifestDirectoryPath = pathHelper.getExecutionManifestDirectoryPath(execution);
		Assert.assertFalse(new File(executionManifestDirectoryPath).exists());
		fileHelper.putFile(getClass().getResourceAsStream("zip_simple_refset_manifest.xml"), executionManifestDirectoryPath + "zip_simple_refset_manifest.xml");

		//And also a refset file to copy down from the output (NOT INPUT!) directory
		URL fileURL = getClass().getResource("der2_Refset_SimpleDelta_INT_20140831.txt");
		File testRefset = new File(fileURL.getFile());
		Assert.assertTrue(testRefset.exists());
		executionDAO.putOutputFile(execution, testRefset, false);
	}
	
	@Test
	public void testZipper() throws JAXBException, IOException, NoSuchAlgorithmException, DecoderException, ResourceNotFoundException {

		Zipper zipper = new Zipper(execution, executionDAO);
		File zipFile = zipper.createZipFile();

		Assert.assertNotNull(zipFile);
		Assert.assertTrue(zipFile.exists());

		Map<String, String> zipContents = FileUtils.examineZipContents(zipFile.getName(), new FileInputStream(zipFile));
		for (String key : zipContents.keySet()) {
			System.out.println(key + "," + zipContents.get(key));
		}
		Assert.assertEquals("Expecting 11 directories + 1 file = 12 items in zipped file", 12, zipContents.size());

		//And lets make sure our test file is in there.
		Assert.assertTrue(zipContents.containsValue(FilenameUtils
				.separatorsToSystem("SnomedCT_Release_INT_20140831/RF2Release/Delta/Refset/Content/der2_Refset_SimpleDelta_INT_20140831.txt")));

		LOGGER.info("Created Test Zip Archive: {}", zipFile.getAbsolutePath());
	}

}

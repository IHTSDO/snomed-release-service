package org.ihtsdo.buildcloud.core.service.build;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.io.FilenameUtils;
import org.ihtsdo.buildcloud.config.TestConfig;
import org.ihtsdo.buildcloud.core.dao.BuildDAOImpl;
import org.ihtsdo.buildcloud.core.dao.ProductDAO;
import org.ihtsdo.buildcloud.core.dao.helper.BuildS3PathHelper;
import org.ihtsdo.buildcloud.core.entity.Build;
import org.ihtsdo.buildcloud.core.entity.Product;
import org.ihtsdo.otf.dao.s3.S3Client;
import org.ihtsdo.otf.dao.s3.helper.FileHelper;
import org.ihtsdo.otf.dao.s3.helper.S3ClientHelper;
import org.ihtsdo.otf.rest.exception.ResourceNotFoundException;
import org.ihtsdo.otf.utils.FileUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Map;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = TestConfig.class)
@Transactional
public class ZipperTest {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ZipperTest.class);

	@Autowired
	private BuildS3PathHelper pathHelper;
	
	@Autowired
	protected ProductDAO productDAO;

	@Autowired
	private BuildDAOImpl buildDAO;

	@Value("${srs.build.bucketName}")
	private String buildBucketName;
	
	@Autowired 
	S3Client s3client;
	
	protected Build build;
	
	@Before
	public void setup() throws NoSuchAlgorithmException, IOException, DecoderException, InterruptedException {
		Product product = productDAO.find(1L);
		Thread.sleep(1000);// Make build unique
		build = new Build(new Date(), product);

		//We're going to locally copy a manifest file for the test
		FileHelper fileHelper = new FileHelper(buildBucketName, s3client, new S3ClientHelper(s3client));
		String buildManifestDirectoryPath = pathHelper.getBuildManifestDirectoryPath(build);
		Assert.assertFalse(new File(buildManifestDirectoryPath).exists());
		fileHelper.putFile(getClass().getResourceAsStream("zip_simple_refset_manifest.xml"), buildManifestDirectoryPath + "zip_simple_refset_manifest.xml");

		//And also a refset file to copy down from the output (NOT INPUT!) directory
		URL fileURL = getClass().getResource("der2_Refset_SimpleDelta_INT_20140831.txt");
		File testRefset = new File(fileURL.getFile());
		Assert.assertTrue(testRefset.exists());
		buildDAO.putOutputFile(build, testRefset, false);
	}
	
	@Test
	public void testZipper() throws JAXBException, IOException, NoSuchAlgorithmException, DecoderException, ResourceNotFoundException {

		Zipper zipper = new Zipper(build, buildDAO);
		File zipFile = zipper.createZipFile(false, false);

		Assert.assertNotNull(zipFile);
		Assert.assertTrue(zipFile.exists());

		Map<String, String> zipContents = FileUtils.examineZipContents(zipFile.getName(), new FileInputStream(zipFile));
		for (String key : zipContents.keySet()) {
			System.out.println(key + "," + zipContents.get(key));
		}
		assertEquals("Expecting 11 directories + 1 file = 12 items in zipped file", 12, zipContents.size());

		//And lets make sure our test file is in there.
		assertTrue(zipContents.containsValue(FilenameUtils
				.separatorsToSystem("SnomedCT_Release_INT_20140831/RF2Release/Delta/Refset/Content/der2_Refset_SimpleDelta_INT_20140831.txt")));
		
		assertTrue(zipContents.containsValue(FilenameUtils
				.separatorsToSystem("SnomedCT_Release_INT_20140831/RF2Release/Snapshot/Refset/")));

		assertTrue(zipContents.containsValue(FilenameUtils
				.separatorsToSystem("SnomedCT_Release_INT_20140831/RF2Release/Full/Refset/")));
		
		LOGGER.info("Created Test Zip Archive: {}", zipFile.getAbsolutePath());
	}

	
	@Test
	public void testZipperDeltaOnly() throws JAXBException, IOException, NoSuchAlgorithmException, DecoderException, ResourceNotFoundException {

		Zipper zipper = new Zipper(build, buildDAO);
		File zipFile = zipper.createZipFile(true, false);

		assertNotNull(zipFile);
		assertTrue(zipFile.exists());

		Map<String, String> zipContents = FileUtils.examineZipContents(zipFile.getName(), new FileInputStream(zipFile));
		for (String key : zipContents.keySet()) {
			System.out.println(key + "," + zipContents.get(key));
		}
		assertEquals("Expecting 5 directories + 1 file = 6 items in zipped file", 6, zipContents.size());

		//And lets make sure our test file is in there.
		assertTrue(zipContents.containsValue(FilenameUtils
				.separatorsToSystem("SnomedCT_Release_INT_20140831/RF2Release/Delta/Refset/Content/der2_Refset_SimpleDelta_INT_20140831.txt")));

		assertFalse(zipContents.containsValue(FilenameUtils
				.separatorsToSystem("SnomedCT_Release_INT_20140831/RF2Release/Snapshot/Refset/")));

		assertFalse(zipContents.containsValue(FilenameUtils
				.separatorsToSystem("SnomedCT_Release_INT_20140831/RF2Release/Snapshot/Refset/")));
		
		LOGGER.info("Created Test Zip Archive: {}", zipFile.getAbsolutePath());
	}
}

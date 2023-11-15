package org.ihtsdo.buildcloud.core.service.build;

import org.apache.commons.io.FilenameUtils;
import org.ihtsdo.buildcloud.core.dao.BuildDAOImpl;
import org.ihtsdo.buildcloud.core.dao.ProductDAO;
import org.ihtsdo.buildcloud.core.dao.helper.S3PathHelper;
import org.ihtsdo.buildcloud.core.entity.Build;
import org.ihtsdo.buildcloud.core.entity.Product;
import org.ihtsdo.buildcloud.test.AbstractTest;
import org.ihtsdo.otf.dao.s3.S3Client;
import org.ihtsdo.otf.dao.s3.helper.FileHelper;
import org.ihtsdo.otf.rest.exception.ResourceNotFoundException;
import org.ihtsdo.otf.utils.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@Transactional
public class ZipperTest  extends AbstractTest {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ZipperTest.class);

	@Autowired
	private S3PathHelper pathHelper;
	
	@Autowired
	protected ProductDAO productDAO;

	@Autowired
	private BuildDAOImpl buildDAO;

	@Value("${srs.storage.bucketName}")
	private String buildBucketName;
	
	@Autowired 
	S3Client s3client;
	
	protected Build build;
	
	@BeforeEach
	public void setup() throws Exception {
		super.setup();
		Product product = productDAO.find(1L);
		Thread.sleep(1000);// Make build unique
		build = new Build(new Date(), product.getReleaseCenter().getBusinessKey(), product.getBusinessKey(), product.getBuildConfiguration(), product.getQaTestConfig());

		//We're going to locally copy a manifest file for the test
		FileHelper fileHelper = new FileHelper(buildBucketName, s3client);
		String buildManifestDirectoryPath = pathHelper.getBuildManifestDirectoryPath(build);
		assertFalse(new File(buildManifestDirectoryPath).exists());
		fileHelper.putFile(getClass().getResourceAsStream("zip_simple_refset_manifest.xml"), buildManifestDirectoryPath + "zip_simple_refset_manifest.xml");

		//And also a refset file to copy down from the output (NOT INPUT!) directory
		URL deltaFileURL = getClass().getResource("der2_Refset_SimpleDelta_INT_20140831.txt");
		File deltaTestRefset = new File(deltaFileURL.getFile());
		assertTrue(deltaTestRefset.exists());
		buildDAO.putOutputFile(build, deltaTestRefset, false);

		URL snaphostFileURL = getClass().getResource("der2_Refset_SimpleSnapshot_INT_20140831.txt");
		File snapshotTestRefset = new File(snaphostFileURL.getFile());
		assertTrue(snapshotTestRefset.exists());
		buildDAO.putOutputFile(build, snapshotTestRefset, false);
	}
	
	@Test
	public void testZipper() throws JAXBException, IOException, ResourceNotFoundException {

		Zipper zipper = new Zipper(build, buildDAO);
		File zipFile = zipper.createZipFile(Zipper.FileTypeOption.NONE);

		assertNotNull(zipFile);
		assertTrue(zipFile.exists());

		Map<String, String> zipContents = FileUtils.examineZipContents(zipFile.getName(), new FileInputStream(zipFile));
		for (String key : zipContents.keySet()) {
			System.out.println(key + "," + zipContents.get(key));
		}
		assertEquals(13, zipContents.size(), "Expecting 11 directories + 2 file = 13 items in zipped file");

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
	public void testZipperDeltaOnly() throws JAXBException, IOException, ResourceNotFoundException {

		Zipper zipper = new Zipper(build, buildDAO);
		File zipFile = zipper.createZipFile(Zipper.FileTypeOption.DELTA_ONLY);

		assertNotNull(zipFile);
		assertTrue(zipFile.exists());

		Map<String, String> zipContents = FileUtils.examineZipContents(zipFile.getName(), new FileInputStream(zipFile));
		for (String key : zipContents.keySet()) {
			System.out.println(key + "," + zipContents.get(key));
		}
		assertEquals(6, zipContents.size(), "Expecting 5 directories + 1 file = 6 items in zipped file");

		//And lets make sure our test file is in there.
		assertTrue(zipContents.containsValue(FilenameUtils
				.separatorsToSystem("SnomedCT_Release_INT_20140831/RF2Release/Delta/Refset/Content/der2_Refset_SimpleDelta_INT_20140831.txt")));

		assertFalse(zipContents.containsValue(FilenameUtils
				.separatorsToSystem("SnomedCT_Release_INT_20140831/RF2Release/Snapshot/Refset/")));

		assertFalse(zipContents.containsValue(FilenameUtils
				.separatorsToSystem("SnomedCT_Release_INT_20140831/RF2Release/Full/Refset/")));
		
		LOGGER.info("Created Test Zip Archive: {}", zipFile.getAbsolutePath());
	}

	@Test
	public void testZipperSnapshotOnly() throws JAXBException, IOException, ResourceNotFoundException {

		Zipper zipper = new Zipper(build, buildDAO);
		File zipFile = zipper.createZipFile(Zipper.FileTypeOption.SNAPSHOT_ONLY);

		assertNotNull(zipFile);
		assertTrue(zipFile.exists());

		Map<String, String> zipContents = FileUtils.examineZipContents(zipFile.getName(), new FileInputStream(zipFile));
		for (String key : zipContents.keySet()) {
			System.out.println(key + "," + zipContents.get(key));
		}
		assertEquals(6, zipContents.size(), "Expecting 5 directories + 1 file = 6 items in zipped file");

		//And lets make sure our test file is in there.
		assertTrue(zipContents.containsValue(FilenameUtils
				.separatorsToSystem("SnomedCT_Release_INT_20140831/RF2Release/Snapshot/Refset/Content/der2_Refset_SimpleSnapshot_INT_20140831.txt")));

		assertFalse(zipContents.containsValue(FilenameUtils
				.separatorsToSystem("SnomedCT_Release_INT_20140831/RF2Release/Delta/Refset/")));

		assertFalse(zipContents.containsValue(FilenameUtils
				.separatorsToSystem("SnomedCT_Release_INT_20140831/RF2Release/Full/Refset/")));

		LOGGER.info("Created Test Zip Archive: {}", zipFile.getAbsolutePath());
	}
}

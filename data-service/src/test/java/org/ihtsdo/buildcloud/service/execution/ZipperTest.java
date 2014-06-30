package org.ihtsdo.buildcloud.service.execution;

import org.apache.commons.codec.DecoderException;
import org.ihtsdo.buildcloud.dao.BuildDAO;
import org.ihtsdo.buildcloud.dao.ExecutionDAOImpl;
import org.ihtsdo.buildcloud.dao.PackageDAO;
import org.ihtsdo.buildcloud.dao.helper.ExecutionS3PathHelper;
import org.ihtsdo.buildcloud.dao.helper.FileHelper;
import org.ihtsdo.buildcloud.dao.helper.S3ClientHelper;
import org.ihtsdo.buildcloud.dao.s3.OfflineS3ClientImpl;
import org.ihtsdo.buildcloud.dao.s3.S3Client;
import org.ihtsdo.buildcloud.entity.Execution;
import org.ihtsdo.buildcloud.entity.Package;
import org.ihtsdo.buildcloud.entity.helper.EntityHelper;
import org.ihtsdo.buildcloud.entity.helper.TestEntityGenerator;
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
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.GregorianCalendar;

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
	private PackageDAO packageDAO;
	
	@Autowired
	private ExecutionDAOImpl executionDAO;
	
	@Autowired
	private String executionBucketName;
	
	protected Package pkg;
	
	protected Execution execution;
	
	private static final String TEST_BUCKET = "test-bucket";
	
	@Before
	public void setup() throws NoSuchAlgorithmException, IOException, DecoderException {
		
		String testPackageName = TestEntityGenerator.packageNames[0][1][0];
		String testPackageId = EntityHelper.formatAsBusinessKey(testPackageName);
		
		pkg = packageDAO.find(5L, testPackageId, TestEntityGenerator.TEST_USER);

		Date creationTime = new GregorianCalendar(2014, 1, 4, 10, 30, 01).getTime();
		execution = new Execution(creationTime, pkg.getBuild());
		
		Path tempDirectory = Files.createTempDirectory(getClass().getName());
		Assert.assertTrue(new File(tempDirectory.toFile(), TEST_BUCKET).mkdirs());
		S3Client s3OfflineClient = new OfflineS3ClientImpl(tempDirectory.toFile());
		executionDAO.setS3Client(s3OfflineClient);
		
		//We're going to locally copy a manifest file for the test
		URL fileURL = getClass().getResource("simple_refset_manifest.xml");
		File testManifest = new File(fileURL.getFile());
		Assert.assertTrue(testManifest.exists());		
		executionDAO.putOutputFile(execution, pkg, testManifest, "manifest/", false);
		
		//And also a refset file to copy down from the output (NOT INPUT!) directory
		fileURL = getClass().getResource("der2_Refset_SimpleDelta_INT_20140831.txt");
		File testRefset = new File(fileURL.getFile());
		Assert.assertTrue(testManifest.exists());
		String targetPath = pathHelper.getExecutionOutputFilePath(execution, pkg.getBusinessKey(), testRefset.getName());
		FileHelper fileHelper = new FileHelper(executionBucketName, s3OfflineClient, new S3ClientHelper(s3OfflineClient));
		fileHelper.putFile(testManifest, targetPath);
	}
	
	@Test
	// TODO: implement or delete
	public void testZipper() throws JAXBException, IOException, NoSuchAlgorithmException, DecoderException {

//		Zipper zipper = new Zipper (execution, pkg, executionDAO);
//		File zipFile = zipper.createZipFile();
//
//		Assert.assertNotNull(zipFile);
//		Assert.assertTrue(zipFile.exists());
//
//		Map<String, String> zipContents = FileUtils.examineZipContents(zipFile.getName(), new FileInputStream(zipFile));
//		Assert.assertEquals("Expecting 11 directories + 1 file = 12 items in zipped file", 12, zipContents.size());
//
		//And lets make sure our test file is in there.
//		Assert.assertTrue(zipContents.containsValue(FilenameUtils.separatorsToSystem("/SnomedCT_Release_INT_20140831/RF2Release/Delta/Refset/Content/der2_Refset_SimpleDelta_INT_20140831.txt")));
//
//		LOGGER.info("Created Test Zip Archive: {}", zipFile.getAbsolutePath() );
	}
}

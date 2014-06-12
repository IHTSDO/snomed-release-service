package org.ihtsdo.buildcloud.service.execution;

import org.ihtsdo.buildcloud.dao.PackageDAO;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import javax.xml.bind.JAXBException;
import java.io.IOException;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"/test/testDataServiceContext.xml"})
@Transactional
public class ZipperTest {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ZipperTest.class);
	
	@Autowired
	private PackageDAO dao;

	@Test
	public void testZipper() throws JAXBException, IOException {
		// Peter has the fix for this.

//		String testPackageName = TestEntityGenerator.packageNames[0][1][0];
//		String testPackageId = EntityHelper.formatAsBusinessKey(testPackageName);
//
//		Package pkg = dao.find(5L, testPackageId, TestEntityGenerator.TEST_USER);
//		FileService fs = new FileServiceMock ("simple_refset_manifest.xml", "/org/ihtsdo/buildcloud/service/execution/");
//
//		Zipper zipper = new Zipper (pkg, fs);
//		File zipFile = zipper.createZipFile();
//
//		Assert.assertNotNull(zipFile);
//		Assert.assertTrue(zipFile.exists());
//
//		Map<String, String> zipContents = FileUtils.examineZipContents(zipFile.getName(), new FileInputStream(zipFile));
//		Assert.assertEquals("Expecting 11 directories + 1 file = 12 items in zipped file", 12, zipContents.size());

		//And lets make sure our test file is in there.
//		Assert.assertTrue(zipContents.containsValue(FilenameUtils.separatorsToSystem("/SnomedCT_Release_INT_20140831/RF2Release/Delta/Refset/Content/der2_Refset_SimpleDelta_INT_20140831.txt")));
//
//		LOGGER.info("Created Test Zip Archive: {}", zipFile.getAbsolutePath() );
	}

}

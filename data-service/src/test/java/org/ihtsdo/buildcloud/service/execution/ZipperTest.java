package org.ihtsdo.buildcloud.service.execution;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.bind.JAXBException;

import org.ihtsdo.buildcloud.dao.PackageDAO;
import org.ihtsdo.buildcloud.entity.Package;
import org.ihtsdo.buildcloud.entity.helper.EntityHelper;
import org.ihtsdo.buildcloud.entity.helper.TestEntityGenerator;
import org.ihtsdo.buildcloud.service.FileService;
import org.ihtsdo.buildcloud.service.execution.Zipper;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"/applicationContext.xml"})
@Transactional
public class ZipperTest {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ZipperTest.class);
	
	@Autowired
	private PackageDAO dao;
	
	@Autowired
	private FileService fs;

	@Test
	public void testZipper() throws JAXBException, IOException {
		String testPackageName = TestEntityGenerator.packageNames[0][1][0];
		String testPackageId = EntityHelper.formatAsBusinessKey(testPackageName);
		
		Package pkg = dao.find(5L, testPackageId, TestEntityGenerator.TEST_USER);
		Zipper zipper = new Zipper (pkg, "some_test.zip", fs);
		InputStream manifestStream = getClass().getResourceAsStream("simple_refset_manifest.xml");
		File zipFile = zipper.createZipFile(manifestStream);
		Assert.assertNotNull(zipFile);
		Assert.assertTrue(zipFile.exists());
		LOGGER.info("Created Test Zip Archive: {}", zipFile.getAbsolutePath() );
	}

}

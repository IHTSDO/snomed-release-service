package org.ihtsdo.buildcloud.dao;

import org.ihtsdo.buildcloud.entity.Package;
import org.ihtsdo.buildcloud.entity.helper.EntityHelper;
import org.ihtsdo.buildcloud.entity.helper.TestEntityGenerator;
import org.ihtsdo.buildcloud.test.TestUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"/test/testDataServiceContext.xml"})
@Transactional
public class PackageDAOImplTest {

	@Autowired
	private PackageDAO dao;

	@Test
	public void testInitialData() {
		String testPackageName = TestEntityGenerator.packageNames[0][0][0];
		String testPackageId = EntityHelper.formatAsBusinessKey(testPackageName);
		
		Package aPackage = dao.find(1L, testPackageId, TestUtils.TEST_USER);
		Assert.assertNotNull(aPackage);
		Assert.assertEquals(testPackageName, aPackage.getName());
	}

}

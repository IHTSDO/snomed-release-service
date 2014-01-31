package org.ihtsdo.buildcloud.dao;

import org.ihtsdo.buildcloud.entity.Package;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"/applicationContext.xml"})
@Transactional
public class PackageDAOImplTest {

	@Autowired
	private PackageDAO dao;

	@Test
	public void testInitialData() {
		Package aPackage = dao.find("international", "snomed_ct_international_edition", "snomed_ct_spanish_edition",
				"biannual", "release", "test");
		Assert.assertNotNull(aPackage);
		Assert.assertEquals("Release", aPackage.getName());
	}

}

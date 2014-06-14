package org.ihtsdo.buildcloud.service;

import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.Package;
import org.ihtsdo.buildcloud.entity.User;
import org.ihtsdo.buildcloud.entity.helper.EntityHelper;
import org.ihtsdo.buildcloud.entity.helper.TestEntityGenerator;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"/test/testDataServiceContext.xml"})
@Transactional
public class PackageServiceImplTest extends TestEntityGenerator {
	
	@Autowired
	private BuildService bs;
	
	@Autowired
	private PackageService ps;
	private User authenticatedUser;

	@Before
	public void setup() {
		authenticatedUser = TestEntityGenerator.TEST_USER;
	}

	@Test
	public void testCreate() throws Exception{

		Assert.assertNotNull(ps);
		String rc  = EntityHelper.formatAsBusinessKey(releaseCenterShortNames[0]);
		String ext = EntityHelper.formatAsBusinessKey(extensionNames[0]);
		String p   = EntityHelper.formatAsBusinessKey(productNames[0][0]);
		
		//Packages get looked up using a build composite key (ie include the unique ID) 
		//so first lets find the first build for a known product, and use that
		List<Build> builds = bs.findForProduct(rc, ext, p, authenticatedUser);
		String buildCompKey = builds.get(0).getCompositeKey();
		
		List<Package> packages = ps.findAll(buildCompKey, authenticatedUser);
		int before = packages.size();
		//LOGGER.warn("Found " + before + " packages");
		
		Assert.assertTrue(before > 0);  //Check our test data is in there.
		ps.create(buildCompKey, "my test packages name", authenticatedUser);
		int after = ps.findAll(buildCompKey, authenticatedUser).size();
		//LOGGER.warn("After create, found " + after + " packages");
		Assert.assertEquals(before + 1, after);
		
		//TODO Could add further tests to ensure that the new item was created at the correct point in the hierarchy
	}

}

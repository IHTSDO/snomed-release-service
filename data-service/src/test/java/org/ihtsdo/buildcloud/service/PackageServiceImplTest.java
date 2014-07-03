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
	private BuildService buildService;
	
	@Autowired
	private PackageService packageService;

	private User authenticatedUser;
	private String buildCompKey;
	private Build build;

	@Before
	public void setup() {
		authenticatedUser = TestEntityGenerator.TEST_USER;
		Assert.assertNotNull(packageService);
		String releaseCenterName  = EntityHelper.formatAsBusinessKey(releaseCenterShortNames[0]);
		String extensionName = EntityHelper.formatAsBusinessKey(extensionNames[0]);
		String packageName   = EntityHelper.formatAsBusinessKey(productNames[0][0]);

		//Packages get looked up using a build composite key (ie include the unique ID)
		//so first lets find the first build for a known product, and use that
		List<Build> builds = buildService.findForProduct(releaseCenterName, extensionName, packageName, authenticatedUser);
		build = builds.get(0);
		buildCompKey = build.getCompositeKey();
	}

	@Test
	public void testCreate() throws Exception {
		// Assert pre conditions
		Assert.assertEquals("Test data in database.", 3, packageService.findAll(buildCompKey, authenticatedUser).size());

		// Run test method
		Package aPackage = packageService.create(buildCompKey, "my test packages name", authenticatedUser);

		// Assert post conditions
		Assert.assertEquals("New package created.", 4, packageService.findAll(buildCompKey, authenticatedUser).size());
		Assert.assertNotNull(aPackage.getId());
		Assert.assertEquals("my test packages name", aPackage.getName());
		Assert.assertEquals(build, aPackage.getBuild());
	}



}

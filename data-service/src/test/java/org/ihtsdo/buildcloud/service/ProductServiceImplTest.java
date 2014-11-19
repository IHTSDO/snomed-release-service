package org.ihtsdo.buildcloud.service;

import org.ihtsdo.buildcloud.entity.Product;
import org.ihtsdo.buildcloud.entity.helper.EntityHelper;
import org.ihtsdo.buildcloud.entity.helper.TestEntityGenerator;
import org.ihtsdo.buildcloud.test.TestUtils;
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
public class ProductServiceImplTest extends TestEntityGenerator {
	
	@Autowired
	private ProductService ps;

	@Before
	public void setup() {
		TestUtils.setTestUser();
	}

	@Test
	public void testCreate() throws Exception{

		Assert.assertNotNull(ps);
		String rc = EntityHelper.formatAsBusinessKey(releaseCenterShortNames[0]);
		String ext = EntityHelper.formatAsBusinessKey(extensionNames[0]);
		List<Product> products = ps.findAll(rc, ext);
		int before = products.size();
		//LOGGER.warn("Found " + before + " products");
		Assert.assertTrue(before > 0);  //Check our test authenticatedUser is in there.
		ps.create(rc, ext, "my test product name");
		int after = ps.findAll(rc, ext).size();
		Assert.assertEquals(before + 1, after);
		
		//TODO Could add further tests to ensure that the new item was created at the correct point in the hierarchy
	}

}

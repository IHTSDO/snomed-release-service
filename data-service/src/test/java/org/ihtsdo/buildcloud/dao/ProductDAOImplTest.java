package org.ihtsdo.buildcloud.dao;

import org.ihtsdo.buildcloud.entity.Product;
import org.ihtsdo.buildcloud.entity.helper.EntityHelper;
import org.ihtsdo.buildcloud.entity.helper.TestEntityGenerator;
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
public class ProductDAOImplTest extends TestEntityGenerator{

	@Autowired
	private ProductDAO dao;

	@Test
	public void testInitialData() {

		Product product = dao.find(	EntityHelper.formatAsBusinessKey(TestEntityGenerator.releaseCenterShortNames[0]),
									EntityHelper.formatAsBusinessKey(TestEntityGenerator.extensionNames[1].toLowerCase()), 
									EntityHelper.formatAsBusinessKey(TestEntityGenerator.productNames[1][0].toLowerCase()),
									TestEntityGenerator.TEST_USER);
		Assert.assertNotNull(product);
		Assert.assertEquals(TestEntityGenerator.productNames[1][0], product.getName());
	}

}

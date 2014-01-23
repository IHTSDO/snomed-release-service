package org.ihtsdo.buildcloud.dao;

import org.ihtsdo.buildcloud.entity.Extension;
import org.ihtsdo.buildcloud.entity.Product;
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
public class ProductDAOImplTest {

	@Autowired
	private ProductDAO dao;

	@Test
	public void testInitialData() {
		Product product = dao.find("international", "snomed_ct_international_edition", "snomed_ct_spanish_edition", "test");
		Assert.assertNotNull(product);
		Assert.assertEquals("SNOMED CT Spanish Edition", product.getName());
	}

}

package org.ihtsdo.buildcloud.service;

import org.ihtsdo.buildcloud.entity.Product;
import org.ihtsdo.buildcloud.entity.helper.EntityHelper;
import org.ihtsdo.buildcloud.entity.helper.TestEntityGenerator;
import org.ihtsdo.buildcloud.service.helper.FilterOption;
import org.ihtsdo.buildcloud.test.TestUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"/test/testDataServiceContext.xml"})
@Transactional
public class ProductServiceImplTest extends TestEntityGenerator {
	
	@Autowired
	private ProductService bs;

	private String releaseCenterKey;

	@Before
	public void setup() {
		TestUtils.setTestUser();
		releaseCenterKey = EntityHelper.formatAsBusinessKey(releaseCenterShortNames[0]);
	}
	
	@Test
	public void testCreate() throws Exception{

		Assert.assertNotNull(bs);
		EnumSet<FilterOption> filterOptions = EnumSet.of(FilterOption.INCLUDE_REMOVED);
		List<Product> products = bs.findAll(releaseCenterKey, filterOptions);
		int before = products.size();
		//LOGGER.warn("Found " + before + " products");
		Assert.assertTrue(before > 0);  //Check our test data is in there.
		bs.create(releaseCenterKey,
				"my test product name");
		int after = bs.findAll(releaseCenterKey, filterOptions).size();
		Assert.assertEquals(before + 1, after);
		
		//TODO Could add further tests to ensure that the new item was created at the correct point in the hierarchy
	}
	
	@Test
	public void testRemovedFilter() throws Exception{

		EnumSet<FilterOption> filterOff = EnumSet.noneOf(FilterOption.class);
		EnumSet<FilterOption> filterOn = EnumSet.of(FilterOption.INCLUDE_REMOVED);
		List<Product> products = bs.findAll(releaseCenterKey, filterOff);
		int visibleProductCount = products.size();
		Assert.assertTrue(visibleProductCount > 0);
		
		int includeRemovedCount = bs.findAll(releaseCenterKey, filterOn).size();
		Assert.assertTrue(includeRemovedCount > 0);
		
		//TODO When remove functionality is built, use it to remove a product and check
		//that our product count goes up if we inclue removed products.
	}		

}

package org.ihtsdo.buildcloud.core.service;

import org.ihtsdo.buildcloud.TestConfig;
import org.ihtsdo.buildcloud.core.entity.Product;
import org.ihtsdo.buildcloud.core.entity.helper.EntityHelper;
import org.ihtsdo.buildcloud.core.entity.helper.TestEntityGenerator;
import org.ihtsdo.buildcloud.core.service.helper.FilterOption;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfig.class)
@Transactional
public class ProductServiceImplTest extends TestEntityGenerator {
	
	@Autowired
	private ProductService bs;

	private String releaseCenterKey;

	@BeforeEach
	public void setup() {
		releaseCenterKey = EntityHelper.formatAsBusinessKey(releaseCenterShortNames[0]);
	}
	
	@Test
	public void testCreate() throws Exception{

		assertNotNull(bs);
		EnumSet<FilterOption> filterOptions = EnumSet.of(FilterOption.INCLUDE_REMOVED);
		filterOptions.add(FilterOption.INCLUDE_LEGACY);
		Page<Product> page = bs.findAll(releaseCenterKey, filterOptions, PageRequest.of(0,20), false);
		int before = page.getContent().size();
		//LOGGER.warn("Found " + before + " products");
		assertTrue(before > 0);  //Check our test data is in there.
		bs.create(releaseCenterKey, "my test product name", null);
		int after = bs.findAll(releaseCenterKey, filterOptions,PageRequest.of(0,20), false).getContent().size();
		assertEquals(before + 1, after);
		
		//TODO Could add further tests to ensure that the new item was created at the correct point in the hierarchy
	}
	
	@Test
	@Disabled
	// TODO
	public void testRemovedFilter() throws Exception{

		EnumSet<FilterOption> filterOff = EnumSet.noneOf(FilterOption.class);
		EnumSet<FilterOption> filterOn = EnumSet.of(FilterOption.INCLUDE_REMOVED);
		filterOn.add(FilterOption.INCLUDE_LEGACY);
		Page<Product> page = bs.findAll(releaseCenterKey, filterOff, PageRequest.of(0,10), false);
		assertEquals(0, page.getContent().size());

		int includeRemovedCount = bs.findAll(releaseCenterKey, filterOn, PageRequest.of(0,10), false).getContent().size();
		assertTrue(includeRemovedCount > 0);
		
		//TODO When remove functionality is built, use it to remove a product and check
		//that our product count goes up if we inclue removed products.
	}		

}

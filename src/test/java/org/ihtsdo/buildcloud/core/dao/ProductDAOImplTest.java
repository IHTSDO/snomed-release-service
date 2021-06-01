package org.ihtsdo.buildcloud.core.dao;

import org.ihtsdo.buildcloud.config.TestConfig;
import org.ihtsdo.buildcloud.core.entity.Product;
import org.ihtsdo.buildcloud.core.entity.helper.TestEntityGenerator;
import org.ihtsdo.buildcloud.core.service.helper.FilterOption;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = TestConfig.class)
@Transactional
public class ProductDAOImplTest {

	@Autowired
	private ProductDAOImpl dao;

	@Test
	@Ignore
	// TODO
	public void testInitialData() {
		EnumSet<FilterOption> filterOptions = EnumSet.of(FilterOption.INCLUDE_REMOVED);
		filterOptions.add(FilterOption.INCLUDE_LEGACY);
		Page<Product> products = dao.findAll(filterOptions, PageRequest.of(0,10));
		Assert.assertNotNull(products);
		Assert.assertEquals(TestEntityGenerator.productNames.length, products.getTotalElements());

		Assert.assertNotNull(dao.find(1L));
		Assert.assertNotNull(dao.find(2L));
		// Attempt to recover product greater than our current amount of test data - should not be found
		Assert.assertNull(dao.find((long)TestEntityGenerator.productNames.length + 1));
	}
}

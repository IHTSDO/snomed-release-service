package org.ihtsdo.buildcloud.core.dao;

import org.ihtsdo.buildcloud.TestConfig;
import org.ihtsdo.buildcloud.core.entity.Product;
import org.ihtsdo.buildcloud.core.entity.helper.TestEntityGenerator;
import org.ihtsdo.buildcloud.core.service.helper.FilterOption;
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
public class ProductDAOImplTest {

	@Autowired
	private ProductDAOImpl dao;

	@Test
	@Disabled
	// TODO
	public void testInitialData() {
		EnumSet<FilterOption> filterOptions = EnumSet.of(FilterOption.INCLUDE_REMOVED);
		filterOptions.add(FilterOption.INCLUDE_LEGACY);
		Page<Product> products = dao.findAll(filterOptions, PageRequest.of(0,10));
		assertNotNull(products);
		assertEquals(TestEntityGenerator.productNames.length, products.getTotalElements());

		assertNotNull(dao.find(1L));
		assertNotNull(dao.find(2L));
		// Attempt to recover product greater than our current amount of test data - should not be found
		assertNull(dao.find((long)TestEntityGenerator.productNames.length + 1));
	}
}

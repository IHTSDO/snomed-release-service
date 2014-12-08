package org.ihtsdo.buildcloud.dao;

import org.ihtsdo.buildcloud.entity.Product;
import org.ihtsdo.buildcloud.entity.helper.TestEntityGenerator;
import org.ihtsdo.buildcloud.service.helper.FilterOption;
import org.ihtsdo.buildcloud.test.TestUtils;
import org.junit.Assert;
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
public class ProductDAOImplTest {

	@Autowired
	private ProductDAO dao;

	@Test
	public void testInitialData() {
		EnumSet<FilterOption> filterOptions = EnumSet.of(FilterOption.INCLUDE_REMOVED);
		List<Product> products = dao.findAll(filterOptions, TestUtils.TEST_USER);
		Assert.assertNotNull(products);
		Assert.assertEquals(TestEntityGenerator.productNames.length, products.size());

		Assert.assertNotNull(dao.find(1L, TestUtils.TEST_USER));
		Assert.assertNotNull(dao.find(2L, TestUtils.TEST_USER));
		// Attempt to recover product greater than our current amount of test data - should not be found
		Assert.assertNull(dao.find((long)TestEntityGenerator.productNames.length + 1, TestUtils.TEST_USER));
	}

}

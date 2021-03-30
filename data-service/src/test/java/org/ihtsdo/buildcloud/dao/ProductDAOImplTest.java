package org.ihtsdo.buildcloud.dao;

import org.ihtsdo.buildcloud.config.HibernateTransactionManagerConfiguration;
import org.ihtsdo.buildcloud.config.LocalSessionFactoryBeanConfiguration;
import org.ihtsdo.buildcloud.entity.Product;
import org.ihtsdo.buildcloud.entity.helper.TestEntityGenerator;
import org.ihtsdo.buildcloud.service.helper.FilterOption;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;

@EnableConfigurationProperties
@PropertySource(value = "classpath:application.properties", encoding = "UTF-8")
@TestConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {LocalSessionFactoryBeanConfiguration.class,
		HibernateTransactionManagerConfiguration.class, ProductDAOImpl.class})
@Transactional()
public class ProductDAOImplTest {

	@Autowired
	private ProductDAOImpl dao;

	@Test
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

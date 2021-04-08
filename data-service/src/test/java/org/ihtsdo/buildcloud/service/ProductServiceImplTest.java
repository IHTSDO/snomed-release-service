package org.ihtsdo.buildcloud.service;

import com.amazonaws.auth.BasicAWSCredentials;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ihtsdo.buildcloud.config.DataServiceTestConfig;
import org.ihtsdo.buildcloud.dao.*;
import org.ihtsdo.buildcloud.dao.helper.BuildS3PathHelper;
import org.ihtsdo.buildcloud.entity.Product;
import org.ihtsdo.buildcloud.entity.helper.EntityHelper;
import org.ihtsdo.buildcloud.entity.helper.TestEntityGenerator;
import org.ihtsdo.buildcloud.service.build.readme.ReadmeGenerator;
import org.ihtsdo.buildcloud.service.build.transform.LegacyIdTransformationService;
import org.ihtsdo.buildcloud.service.build.transform.PesudoUUIDGenerator;
import org.ihtsdo.buildcloud.service.build.transform.TransformationService;
import org.ihtsdo.buildcloud.service.helper.FilterOption;
import org.ihtsdo.buildcloud.service.identifier.client.IdServiceRestClientImpl;
import org.ihtsdo.buildcloud.service.postcondition.PostconditionManager;
import org.ihtsdo.buildcloud.service.precondition.PreconditionManager;
import org.ihtsdo.buildcloud.service.workbenchdatafix.ModuleResolverService;
import org.ihtsdo.otf.dao.s3.OfflineS3ClientImpl;
import org.ihtsdo.otf.dao.s3.helper.S3ClientHelper;
import org.junit.Assert;
import org.junit.Before;
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

@PropertySource(value = "classpath:application.properties", encoding = "UTF-8")
@TestConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = DataServiceTestConfig.class)
@Transactional
public class ProductServiceImplTest extends TestEntityGenerator {
	
	@Autowired
	private ProductService bs;

	private String releaseCenterKey;

	@Before
	public void setup() {
		releaseCenterKey = EntityHelper.formatAsBusinessKey(releaseCenterShortNames[0]);
	}
	
	@Test
	public void testCreate() throws Exception{

		Assert.assertNotNull(bs);
		EnumSet<FilterOption> filterOptions = EnumSet.of(FilterOption.INCLUDE_REMOVED);
		filterOptions.add(FilterOption.INCLUDE_LEGACY);
		Page<Product> page = bs.findAll(releaseCenterKey, filterOptions, PageRequest.of(0,10));
		int before = page.getContent().size();
		//LOGGER.warn("Found " + before + " products");
		Assert.assertTrue(before > 0);  //Check our test data is in there.
		bs.create(releaseCenterKey,
				"my test product name");
		int after = bs.findAll(releaseCenterKey, filterOptions,PageRequest.of(0,10)).getContent().size();
		Assert.assertEquals(before + 1, after);
		
		//TODO Could add further tests to ensure that the new item was created at the correct point in the hierarchy
	}
	
	@Test
	public void testRemovedFilter() throws Exception{

		EnumSet<FilterOption> filterOff = EnumSet.noneOf(FilterOption.class);
		EnumSet<FilterOption> filterOn = EnumSet.of(FilterOption.INCLUDE_REMOVED);
		filterOn.add(FilterOption.INCLUDE_LEGACY);
		Page<Product> page = bs.findAll(releaseCenterKey, filterOff, PageRequest.of(0,10));
		int visibleProductCount = page.getContent().size();
		Assert.assertTrue(visibleProductCount == 0);
		
		int includeRemovedCount = bs.findAll(releaseCenterKey, filterOn, PageRequest.of(0,10)).getContent().size();
		Assert.assertTrue(includeRemovedCount > 0);
		
		//TODO When remove functionality is built, use it to remove a product and check
		//that our product count goes up if we inclue removed products.
	}		

}

package org.ihtsdo.buildcloud.service;

import java.util.List;

import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.helper.EntityHelper;
import org.ihtsdo.buildcloud.entity.helper.TestEntityGenerator;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"/applicationContext.xml"})
@Transactional
public class BuildServiceImplTest extends TestEntityGenerator {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(BuildServiceImplTest.class);
	
	public static final String AUTHENTICATED_ID = "test";
	
	@Autowired
	private BuildService bs;

	@Test
	public void testCreate() throws Exception{

		Assert.assertNotNull(bs);
		List<Build> builds = bs.findAll(AUTHENTICATED_ID);
		int before = builds.size();
		//LOGGER.warn("Found " + before + " builds");
		Assert.assertTrue(before > 0);  //Check our test data is in there.
		bs.create(EntityHelper.formatAsBusinessKey(releaseCentreShortNames[0]), 
				  EntityHelper.formatAsBusinessKey(extensionNames[0]), 
				  EntityHelper.formatAsBusinessKey(productNames[0][0]), 
				  "my test build name", 
				  AUTHENTICATED_ID);
		int after = bs.findAll(AUTHENTICATED_ID).size();
		Assert.assertEquals(before + 1, after);
		
		//TODO Could add further tests to ensure that the new item was created at the correct point in the hierarchy
	}

}

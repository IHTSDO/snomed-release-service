package org.ihtsdo.buildcloud.service;

import java.util.List;

import org.ihtsdo.buildcloud.entity.ReleaseCentre;
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
public class ReleaseCentreServiceImplTest extends TestEntityGenerator {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ReleaseCentreServiceImplTest.class);
	
	public static final String AUTHENTICATED_ID = "test";
	
	@Autowired
	private ReleaseCentreService rcs;

	@Test
	public void testCreate() throws Exception{

		Assert.assertNotNull(rcs);
		List<ReleaseCentre> releaseCenters = rcs.findAll(AUTHENTICATED_ID);
		int before = releaseCenters.size();
		//LOGGER.warn("Found " + before + " release centers");
		Assert.assertTrue(before > 0);  //Check our test data is in there.
		rcs.create("my test releaseCentre name", "some short name", AUTHENTICATED_ID);
		int after = rcs.findAll(AUTHENTICATED_ID).size();
		//LOGGER.warn("After create, found " + after + " release centers");
		Assert.assertEquals(before + 1, after);
		
		//TODO Could add further test to ensure that the new item was created at the correct point in the hierarchy
	}

}

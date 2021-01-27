package org.ihtsdo.buildcloud.service;

import org.ihtsdo.buildcloud.entity.ReleaseCenter;
import org.ihtsdo.buildcloud.entity.helper.TestEntityGenerator;
import org.ihtsdo.buildcloud.test.TestUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"/test/testDataServiceContext.xml"})
@Transactional
public class ReleaseCenterServiceImplTest extends TestEntityGenerator {
	
	@Autowired
	private ReleaseCenterService rcs;

	@Before
	public void setup() {
//		TestUtils.setTestUser();
	}

	@Test
	public void testCreate() throws Exception{

		Assert.assertNotNull(rcs);
		List<ReleaseCenter> releaseCenters = rcs.findAll();
		int before = releaseCenters.size();
		//LOGGER.warn("Found " + before + " release centers");
		Assert.assertTrue(before > 0);  //Check our test data is in there.
		rcs.create("my test releaseCenter name", "some short name", "code system");
		int after = rcs.findAll().size();
		//LOGGER.warn("After create, found " + after + " release centers");
		Assert.assertEquals(before + 1, after);
		
		//TODO Could add further test to ensure that the new item was created at the correct point in the hierarchy
	}

}

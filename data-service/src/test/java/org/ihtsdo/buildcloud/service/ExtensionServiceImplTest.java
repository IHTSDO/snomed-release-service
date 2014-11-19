package org.ihtsdo.buildcloud.service;

import org.ihtsdo.buildcloud.entity.Extension;
import org.ihtsdo.buildcloud.entity.helper.EntityHelper;
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
public class ExtensionServiceImplTest extends TestEntityGenerator {
	
	@Autowired
	private ExtensionService es;

	@Before
	public void setup() {
		TestUtils.setTestUser();
	}

	@Test
	public void testCreate() throws Exception{

		Assert.assertNotNull(es);
		String rc = EntityHelper.formatAsBusinessKey(releaseCenterShortNames[0]);
		List<Extension> extensions = es.findAll(rc);
		int before = extensions.size();
		//LOGGER.warn("Found " + before + " extensions");
		Assert.assertTrue(before > 0);  //Check our test data is in there.
		es.create(rc, "my test extension name");
		int after = es.findAll(rc).size();
		Assert.assertEquals(before + 1, after);
		
		//TODO Could add further tests to ensure that the new item was created at the correct point in the hierarchy
	}

}

package org.ihtsdo.buildcloud.dao;

import org.ihtsdo.buildcloud.entity.Build;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"/applicationContext.xml"})
@Transactional
public class BuildDAOImplTest {
	
	public static final String AUTHENTICATED_ID = "test";

	@Autowired
	private BuildDAO dao;

	@Test
	public void testInitialData() {
		List<Build> builds = dao.findAll(AUTHENTICATED_ID);
		Assert.assertNotNull(builds);
		Assert.assertEquals(8, builds.size());

		Assert.assertNotNull(dao.find(1L, AUTHENTICATED_ID));
		Assert.assertNotNull(dao.find(2L, AUTHENTICATED_ID));
		Assert.assertNull(dao.find(9L, AUTHENTICATED_ID));
	}

}

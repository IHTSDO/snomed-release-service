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

	@Autowired
	private BuildDAO dao;

	@Test
	public void testInitialData() {
		List<Build> builds = dao.findAll("test");
		Assert.assertNotNull(builds);
		Assert.assertEquals(8, builds.size());

		Assert.assertNotNull(dao.find(1L, "test"));
		Assert.assertNotNull(dao.find(2L, "test"));
		Assert.assertNull(dao.find(9L, "test"));
	}

}

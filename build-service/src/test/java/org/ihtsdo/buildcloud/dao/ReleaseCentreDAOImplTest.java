package org.ihtsdo.buildcloud.dao;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"/applicationContext.xml"})
@Transactional
public class ReleaseCentreDAOImplTest {

	@Autowired
	private ReleaseCentreDAO dao;

	@Test
	public void test() {
		Assert.assertNotNull(dao);
		Assert.assertEquals(2, dao.findAll().size());
	}

}

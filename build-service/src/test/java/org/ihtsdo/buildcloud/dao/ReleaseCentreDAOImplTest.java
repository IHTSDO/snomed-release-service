package org.ihtsdo.buildcloud.dao;

import org.ihtsdo.buildcloud.entity.ReleaseCentre;
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
public class ReleaseCentreDAOImplTest {

	@Autowired
	private ReleaseCentreDAO dao;

	@Test
	public void testInitialData() {
		Assert.assertNotNull(dao);
		List<ReleaseCentre> centres = dao.findAll("test");
		Assert.assertEquals(1, centres.size());
		ReleaseCentre internationalReleaseCentre = centres.get(0);
		Assert.assertEquals("International", internationalReleaseCentre.getName());
		Assert.assertEquals("international", internationalReleaseCentre.getBusinessKey());
	}

	@Test
	public void test() {
		ReleaseCentre releaseCentre = dao.find("international", "test");
		Assert.assertNotNull(releaseCentre);
		Assert.assertEquals("International", releaseCentre.getName());
	}

}

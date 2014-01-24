package org.ihtsdo.buildcloud.dao;

import org.ihtsdo.buildcloud.entity.Extension;
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
public class ExtensionDAOImplTest {

	@Autowired
	private ExtensionDAO dao;

	@Test
	public void testInitialData() {
		Extension extension = dao.find("international", "snomed_ct_international_edition", "test");
		Assert.assertNotNull(extension);
		Assert.assertEquals("SNOMED CT International Edition", extension.getName());
	}

}

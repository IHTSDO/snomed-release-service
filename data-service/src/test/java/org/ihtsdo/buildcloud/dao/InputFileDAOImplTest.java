package org.ihtsdo.buildcloud.dao;

import org.ihtsdo.buildcloud.entity.InputFile;
import org.ihtsdo.buildcloud.entity.helper.TestEntityGenerator;
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
public class InputFileDAOImplTest {

	@Autowired
	private InputFileDAO dao;

	@Test
	public void testInitialData() {
		InputFile inputFile = dao.find(1L, "rf2_release", "concepts_rf2", TestEntityGenerator.TEST_USER);
		Assert.assertNotNull(inputFile);
		Assert.assertEquals("concepts rf2", inputFile.getName());
		Assert.assertEquals("concepts_rf2", inputFile.getBusinessKey());
	}

}

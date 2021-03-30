package org.ihtsdo.buildcloud.dao;

import java.util.List;

import org.ihtsdo.buildcloud.config.HibernateTransactionManagerConfiguration;
import org.ihtsdo.buildcloud.config.LocalSessionFactoryBeanConfiguration;
import org.ihtsdo.buildcloud.entity.ReleaseCenter;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

@EnableConfigurationProperties
@PropertySource(value = "classpath:application.properties", encoding = "UTF-8")
@TestConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {LocalSessionFactoryBeanConfiguration.class,
		HibernateTransactionManagerConfiguration.class, ReleaseCenterDAOImpl.class})
@Transactional
public class ReleaseCenterDAOImplTest {

	@Autowired
	private ReleaseCenterDAO dao;

	@Test
	public void testFindAll() {
		Assert.assertNotNull(dao);
		List<ReleaseCenter> centers = dao.findAll();
		Assert.assertEquals(5, centers.size());
		ReleaseCenter internationalReleaseCenter = centers.get(0);
		Assert.assertEquals("International Release Center", internationalReleaseCenter.getName());
		Assert.assertEquals("International", internationalReleaseCenter.getShortName());
		Assert.assertEquals("international", internationalReleaseCenter.getBusinessKey());
	}

	@Test
	public void testFind() {
		ReleaseCenter releaseCenter = dao.find("international");
		Assert.assertNotNull(releaseCenter);
		Assert.assertEquals("International Release Center", releaseCenter.getName());
		Assert.assertEquals("International", releaseCenter.getShortName());
	}

}

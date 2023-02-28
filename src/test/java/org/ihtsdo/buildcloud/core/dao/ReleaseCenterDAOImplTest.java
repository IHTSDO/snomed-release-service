package org.ihtsdo.buildcloud.core.dao;

import java.util.List;

import org.ihtsdo.buildcloud.TestConfig;
import org.ihtsdo.buildcloud.core.entity.ReleaseCenter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfig.class)
@Transactional
public class ReleaseCenterDAOImplTest {

	@Autowired
	private ReleaseCenterDAO dao;

	@Test
	public void testFindAll() {
		assertNotNull(dao);
		List<ReleaseCenter> centers = dao.findAll();
		assertEquals(5, centers.size());
		ReleaseCenter internationalReleaseCenter = centers.get(0);
		assertEquals("International Release Center", internationalReleaseCenter.getName());
		assertEquals("International", internationalReleaseCenter.getShortName());
		assertEquals("international", internationalReleaseCenter.getBusinessKey());
	}

	@Test
	public void testFind() {
		ReleaseCenter releaseCenter = dao.find("international");
		assertNotNull(releaseCenter);
		assertEquals("International Release Center", releaseCenter.getName());
		assertEquals("International", releaseCenter.getShortName());
	}

}

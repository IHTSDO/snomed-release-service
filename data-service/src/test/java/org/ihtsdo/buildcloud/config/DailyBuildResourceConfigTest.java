package org.ihtsdo.buildcloud.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"/test/testDataServiceContext.xml"})
public class DailyBuildResourceConfigTest {
	@Autowired
	private DailyBuildResourceConfig dailyBuildResourceConfig;
	
	@Test
	public void testUseCloud() {
		assertNotNull(dailyBuildResourceConfig);
		assertEquals(false, dailyBuildResourceConfig.isUseCloud());
	}
	
	@Test
	public void testGetCloud() {
		assertNotNull(dailyBuildResourceConfig);
		assertTrue(dailyBuildResourceConfig.getCloud().getBucketName().isEmpty());
	}
	@Test
	public void testGetLocalPath() {
		assertNotNull(dailyBuildResourceConfig);
		assertEquals("store/local/", dailyBuildResourceConfig.getLocal().getPath());
	}
}
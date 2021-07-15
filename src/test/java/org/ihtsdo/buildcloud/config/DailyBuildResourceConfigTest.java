package org.ihtsdo.buildcloud.config;

import org.ihtsdo.buildcloud.TestConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = TestConfig.class)
public class DailyBuildResourceConfigTest {

	@Autowired
	private DailyBuildResourceConfig dailyBuildResourceConfig;
	
	@Test
	public void testUseCloud() {
		assertNotNull(dailyBuildResourceConfig);
		assertFalse(dailyBuildResourceConfig.isUseCloud());
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
package org.ihtsdo.buildcloud.config;

import org.ihtsdo.buildcloud.TestConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
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
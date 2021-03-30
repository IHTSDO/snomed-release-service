package org.ihtsdo.buildcloud.config;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.*;

@EnableConfigurationProperties
@PropertySource(value = "classpath:application.properties", encoding = "UTF-8")
@TestConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = DailyBuildResourceConfig.class)
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
package org.ihtsdo.buildcloud.core.dao;

import org.ihtsdo.buildcloud.TestConfig;
import org.ihtsdo.buildcloud.core.entity.Build;
import org.ihtsdo.buildcloud.core.entity.BuildStatusTracker;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = TestConfig.class)
@Transactional()
public class BuildStatusTrackerDaoImplTest {

	@Autowired
	private BuildStatusTrackerDao buildStatusTrackerDao;

	private String productKey;

	private String buildId;


	@Test
	public void testCRUD() {
		BuildStatusTracker tracker = new BuildStatusTracker();
		productKey = "international_edition_releases";
		tracker.setProductKey(productKey);
		buildId = "2021-06-07T12:18:00";
		tracker.setBuildId(buildId);
		tracker.setStatus(Build.Status.QUEUED.name());
		tracker.setReleaseCenterKey("international");
		buildStatusTrackerDao.save(tracker);
		Timestamp startTime = tracker.getStartTime();
		Timestamp lastUpdated = tracker.getLastUpdatedTime();

		BuildStatusTracker result = buildStatusTrackerDao.findByProductKeyAndBuildId(productKey, buildId);
		assertNotNull(result);
		assertEquals(buildId, result.getBuildId());
		assertEquals(productKey, result.getProductKey());
		assertEquals("international", result.getReleaseCenterKey());
		assertEquals(startTime, result.getStartTime());
		assertEquals(lastUpdated, result.getLastUpdatedTime());

		result.setStatus(Build.Status.BUILDING.name());
		buildStatusTrackerDao.update(result);

		result = buildStatusTrackerDao.findByProductKeyAndBuildId(productKey, buildId);
		assertNotNull(result);
		assertEquals("BUILDING", result.getStatus());
		assertEquals(startTime, result.getStartTime());
		assertNotEquals(lastUpdated, tracker.getLastUpdatedTime());
		assertTrue(tracker.getLastUpdatedTime().after(lastUpdated));

		result.setRvfRunId("12345");
		result.setStatus(Build.Status.RVF_RUNNING.name());
		buildStatusTrackerDao.update(result);
		result = buildStatusTrackerDao.findByRvfRunId("12345");

		assertNotNull(result);
		assertEquals("12345", result.getRvfRunId());
		assertEquals(Build.Status.RVF_RUNNING.name(), result.getStatus());
		assertTrue(tracker.getLastUpdatedTime().after(lastUpdated));

	}

}
package org.ihtsdo.buildcloud.core.service;

import org.ihtsdo.buildcloud.TestConfig;
import org.ihtsdo.buildcloud.core.entity.Build;
import org.ihtsdo.buildcloud.core.entity.BuildStatusTracker;
import org.ihtsdo.buildcloud.core.service.manager.BuildStatusTrackerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfig.class)
@Transactional
class BuildStatusTrackerServiceTest {

	@Autowired
	private BuildStatusTrackerService buildStatusTrackerService;

	@Test
    void testCRUD() {
        BuildStatusTracker tracker = new BuildStatusTracker();
		String productKey = "international_edition_releases";
		tracker.setProductKey(productKey);
		String buildId = "2021-06-07T12:18:00";
		tracker.setBuildId(buildId);
		tracker.setStatus(Build.Status.QUEUED.name());
		tracker.setReleaseCenterKey("international");
		buildStatusTrackerService.save(tracker);
		Timestamp startTime = tracker.getStartTime();
		Timestamp lastUpdated = tracker.getLastUpdatedTime();

		BuildStatusTracker result = buildStatusTrackerService.findByProductKeyAndBuildId(productKey, buildId);
		assertNotNull(result);
		assertEquals(buildId, result.getBuildId());
		assertEquals(productKey, result.getProductKey());
		assertEquals("international", result.getReleaseCenterKey());
		assertEquals(startTime, result.getStartTime());
		assertEquals(lastUpdated, result.getLastUpdatedTime());

		result.setStatus(Build.Status.BUILDING.name());
		buildStatusTrackerService.update(result);

		result = buildStatusTrackerService.findByProductKeyAndBuildId(productKey, buildId);
		assertNotNull(result);
		assertEquals("BUILDING", result.getStatus());
		assertEquals(startTime, result.getStartTime());
		assertNotEquals(lastUpdated, tracker.getLastUpdatedTime());
		assertTrue(tracker.getLastUpdatedTime().after(lastUpdated));

		result.setRvfRunId("12345");
		result.setStatus(Build.Status.RVF_RUNNING.name());
		buildStatusTrackerService.update(result);
		result = buildStatusTrackerService.findByRvfRunIdAndBuildId("12345", buildId);

		assertNotNull(result);
		assertEquals("12345", result.getRvfRunId());
		assertEquals(Build.Status.RVF_RUNNING.name(), result.getStatus());
		assertTrue(tracker.getLastUpdatedTime().after(lastUpdated));

	}

}

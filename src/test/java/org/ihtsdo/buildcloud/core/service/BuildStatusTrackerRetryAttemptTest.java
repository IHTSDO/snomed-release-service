package org.ihtsdo.buildcloud.core.service;

import org.ihtsdo.buildcloud.TestConfig;
import org.ihtsdo.buildcloud.core.dao.BuildStatusTrackerDao;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfig.class)
@Transactional
class BuildStatusTrackerRetryAttemptTest {

	@Autowired
	private BuildStatusTrackerService trackerService;

	@Autowired
	private BuildStatusTrackerDao trackerDao;

	@Test
	void retry_createsNewTrackerRow_incrementsRetryCount_andResetsStartTime() throws Exception {
		final String productKey = "retry_product";
		final String buildId = "2026-01-07T16:47:37";

		BuildStatusTracker attempt0 = new BuildStatusTracker();
		attempt0.setProductKey(productKey);
		attempt0.setReleaseCenterKey("international");
		attempt0.setBuildId(buildId);
		attempt0.setRetryCount(0);
		attempt0.setStatus(Build.Status.BUILDING.name());
		trackerService.save(attempt0);

		final long id0 = attempt0.getId();
		final Timestamp start0 = attempt0.getStartTime();

		// Ensure the next attempt has a later start time.
		Thread.sleep(2);

		BuildStatusTracker attempt1 = trackerService.createRetryAttempt(attempt0, 1, Build.Status.QUEUED.name());
		assertNotNull(attempt1);
		assertNotEquals(id0, attempt1.getId());

		// Attempt count increments (stored on the attempt row)
		assertEquals(1, attempt1.getRetryCount());

		// Elapsed time resets by virtue of a new attempt row (new start time)
		assertTrue(attempt1.getStartTime().after(start0), "Expected retry attempt to have a later startTime.");

		// Should keep history: two rows exist for the same productKey+buildId
		final List<BuildStatusTracker> rows = trackerDao.findAllByProductKeyAndBuildId(productKey, buildId);
		assertEquals(2, rows.size());

		// Latest attempt should be returned by findByProductKeyAndBuildId (ordered by startTime desc)
		final BuildStatusTracker latest = trackerService.findByProductKeyAndBuildId(productKey, buildId);
		assertNotNull(latest);
		assertEquals(attempt1.getId(), latest.getId());
		assertEquals(1, latest.getRetryCount());
	}
}



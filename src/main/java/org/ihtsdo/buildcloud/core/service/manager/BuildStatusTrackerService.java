package org.ihtsdo.buildcloud.core.service.manager;

import org.ihtsdo.buildcloud.core.dao.BuildStatusTrackerDao;
import org.ihtsdo.buildcloud.core.entity.BuildStatusTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.List;

@Service
@Transactional
public class BuildStatusTrackerService {

	private static final Logger LOGGER = LoggerFactory.getLogger(BuildStatusTrackerService.class);

	private final BuildStatusTrackerDao trackerDao;

	public BuildStatusTrackerService(BuildStatusTrackerDao trackerDao) {
		this.trackerDao = trackerDao;
	}

	public List<BuildStatusTracker> findByStatus(String... status) {
		return trackerDao.findByStatus(status);
	}

	public List<BuildStatusTracker> findByProductAndStatus(String productKey, String... status) {
		return trackerDao.findByProductAndStatus(productKey, status);
	}

	public BuildStatusTracker findByProductKeyAndBuildId(String productKey, String buildId) {
		return trackerDao.findByProductKeyAndBuildId(productKey, buildId);
	}

	public BuildStatusTracker findLatestByReleaseCenterAndProduct(String releaseCenterKey, String productKey) {
		return trackerDao.findLatestByReleaseCenterAndProduct(releaseCenterKey, productKey);
	}

	public BuildStatusTracker findByRvfRunIdAndBuildId(String rvfRunId, String buildId) {
		return trackerDao.findByRvfRunIdAndBuildId(rvfRunId, buildId);
	}

	public void save(BuildStatusTracker tracker) {
		trackerDao.save(tracker);
	}

	public void update(BuildStatusTracker tracker) {
		trackerDao.update(tracker);
	}

	public void updateStatus(BuildStatusTracker tracker, String newStatus) {
		String previousStatus = tracker.getStatus();
		Timestamp previousUpdatedTime = tracker.getLastUpdatedTime();

		tracker.setStatus(newStatus);
		trackerDao.update(tracker);

		if (previousUpdatedTime != null && tracker.getLastUpdatedTime() != null) {
			long timeTakenInMinutes =
					(tracker.getLastUpdatedTime().getTime() - previousUpdatedTime.getTime()) / (1000 * 60);
			if (previousStatus != null && !previousStatus.equals(newStatus)) {
				LOGGER.info("Status tracking stats for build id {}: It took {} minutes from {} to {}",
						tracker.getBuildId(), timeTakenInMinutes, previousStatus, newStatus);
			}
		}
	}

	/**
	 * Create a new tracker row representing a retry attempt of the same buildId (preserves history).
	 * The latest attempt should be resolved by ordering by startTime desc.
	 */
	public BuildStatusTracker createRetryAttempt(BuildStatusTracker previousAttempt, int retryCount, String status) {
		BuildStatusTracker retryAttempt = new BuildStatusTracker();
		retryAttempt.setProductKey(previousAttempt.getProductKey());
		retryAttempt.setReleaseCenterKey(previousAttempt.getReleaseCenterKey());
		retryAttempt.setBuildId(previousAttempt.getBuildId());
		retryAttempt.setRetryCount(retryCount);
		retryAttempt.setStatus(status);
		trackerDao.save(retryAttempt);
		return retryAttempt;
	}
}





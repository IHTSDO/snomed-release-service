package org.ihtsdo.buildcloud.core.service.manager;

import org.ihtsdo.buildcloud.core.dao.BuildDAO;
import org.ihtsdo.buildcloud.core.entity.Build;
import org.ihtsdo.buildcloud.core.entity.BuildStatusTracker;
import org.ihtsdo.buildcloud.core.service.BuildService;
import org.ihtsdo.buildcloud.core.service.CreateReleasePackageBuildRequest;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.otf.rest.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

import static org.ihtsdo.buildcloud.core.entity.Build.Status.FAILED;
import static org.ihtsdo.buildcloud.core.entity.Build.Status.QUEUED;

@Service
public class InterruptedBuildRetryProcessor {

	private static final Logger LOGGER = LoggerFactory.getLogger(InterruptedBuildRetryProcessor.class);

	private final BuildStatusTrackerService trackerService;
	private final BuildService buildService;
	private final ReleaseBuildManager releaseBuildManager;
	private final BuildDAO buildDAO;

	@Value("${srs.build.queue.timeout.millis:3600000}")
	private long queuedTimeoutMillis;

	@Value("${srs.build.interrupted.max-retries:3}")
	private int interruptedMaxRetries;

	public InterruptedBuildRetryProcessor(BuildStatusTrackerService trackerService,
										  BuildService buildService,
										  ReleaseBuildManager releaseBuildManager,
										  BuildDAO buildDAO) {
		this.trackerService = trackerService;
		this.buildService = buildService;
		this.releaseBuildManager = releaseBuildManager;
		this.buildDAO = buildDAO;
	}

	// Package-private setters used by tests to override configuration without reflection.
	void setInterruptedMaxRetries(int interruptedMaxRetries) {
		this.interruptedMaxRetries = interruptedMaxRetries;
	}

	void setQueuedTimeoutMillis(long queuedTimeoutMillis) {
		this.queuedTimeoutMillis = queuedTimeoutMillis;
	}

	public void retryIfLatestInterrupted(BuildStatusTracker tracker) throws IOException, BusinessServiceException {
		String releaseCenterKey = tracker.getReleaseCenterKey();
		String productKey = tracker.getProductKey();
		String buildId = tracker.getBuildId();

		Build interruptedBuild;
		try {
			interruptedBuild = buildService.find(releaseCenterKey, productKey, buildId,
					true, true, null, null);
		} catch (ResourceNotFoundException e) {
			LOGGER.warn("Interrupted build not found for [{}:{}:{}]. Skipping.", releaseCenterKey, productKey, buildId);
			return;
		}

		// If we've already reached the maximum number of retries for this build chain, mark as FAILED
		if (tracker.getRetryCount() >= interruptedMaxRetries) {
			LOGGER.warn("Max retries ({}) reached for interrupted build [{}:{}:{}]. Marking as FAILED.",
					interruptedMaxRetries, releaseCenterKey, productKey, buildId);
			buildDAO.updateStatus(interruptedBuild, FAILED);
			trackerService.updateStatus(tracker, FAILED.name());
			return;
		}

		// If we've already scheduled a retry for this tracker, do not do it again
		if (tracker.getRetryBuildId() != null) {
			LOGGER.info("Skipping retry of interrupted build [{}:{}:{}] because a retry build {} is already recorded.",
					releaseCenterKey, productKey, buildId, tracker.getRetryBuildId());
			return;
		}

		// Check that this interrupted build is still the latest for the product
		BuildStatusTracker latestTracker = trackerService.findLatestByReleaseCenterAndProduct(releaseCenterKey, productKey);
		if (latestTracker == null) {
			LOGGER.warn("No BuildStatusTracker found for [{}:{}] when retrying interrupted build {}.", releaseCenterKey, productKey, buildId);
			return;
		}

		if (!buildId.equals(latestTracker.getBuildId())) {
			// A newer build already exists; do not auto-retry this one
			LOGGER.info("Skipping retry of build [{}:{}:{}] because a newer build {} exists.",
					releaseCenterKey, productKey, buildId, latestTracker.getBuildId());
			return;
		}

		// Clone and queue a new build attempt
		String username = interruptedBuild.getBuildUser();
		Build retryBuild = buildService.cloneBuild(releaseCenterKey, productKey, buildId, username);

		LOGGER.info("Retrying interrupted build [{}:{}:{}] with new build id {}.",
				releaseCenterKey, productKey, buildId, retryBuild.getId());

		CreateReleasePackageBuildRequest request =
				new CreateReleasePackageBuildRequest(retryBuild, retryBuild.getBuildUser(), null);

		releaseBuildManager.queueBuild(request);

		// Mark this tracker so we don't attempt to auto-retry it again:
		// keep status as INTERRUPTED but record which build is the retry.
		tracker.setRetryBuildId(retryBuild.getId());
		trackerService.update(tracker);

		// Increment retry count on the new build's tracker so we can enforce max retries
		BuildStatusTracker retryTracker = trackerService.findByProductKeyAndBuildId(productKey, retryBuild.getId());
		if (retryTracker != null) {
			trackerService.updateRetryCount(retryTracker, tracker.getRetryCount() + 1);
		}
	}

	public void failIfStillQueued(BuildStatusTracker tracker) throws IOException {
		long cutoffMillis = System.currentTimeMillis() - queuedTimeoutMillis;

		// Skip if not yet old enough
		if (tracker.getLastUpdatedTime() == null ||
				tracker.getLastUpdatedTime().getTime() > cutoffMillis) {
			return;
		}

		String releaseCenterKey = tracker.getReleaseCenterKey();
		String productKey = tracker.getProductKey();
		String buildId = tracker.getBuildId();

		Build build;
		try {
			build = buildService.find(releaseCenterKey, productKey, buildId,
					true, true, null, null);
		} catch (ResourceNotFoundException e) {
			LOGGER.warn("Stuck queued build not found for [{}:{}:{}]. Skipping.",
					releaseCenterKey, productKey, buildId);
			return;
		}

		// Re-check tracker from DB to avoid acting on stale in-memory state
		BuildStatusTracker latestTracker = trackerService.findByProductKeyAndBuildId(productKey, buildId);
		if (latestTracker == null || !QUEUED.name().equals(latestTracker.getStatus())) {
			LOGGER.info("Skipping failing queued build [{}:{}:{}] because tracker status is now {}.",
					releaseCenterKey, productKey, buildId,
					latestTracker != null ? latestTracker.getStatus() : "null");
			return;
		}

		// Also ensure the build itself is still QUEUED (worker might have started it)
		if (build.getStatus() != QUEUED) {
			LOGGER.info("Skipping failing queued build [{}:{}:{}] because build status is now {}.",
					releaseCenterKey, productKey, buildId, build.getStatus());
			return;
		}

		// Update build status in S3 and send status JMS update
		buildDAO.updateStatus(build, FAILED);

		// Update tracker
		trackerService.updateStatus(tracker, FAILED.name());

		LOGGER.warn("Marked stuck queued build [{}:{}:{}] as FAILED after timeout (>{} ms).",
				releaseCenterKey, productKey, buildId, queuedTimeoutMillis);
	}
}



package org.ihtsdo.buildcloud.core.service.manager;

import org.ihtsdo.buildcloud.core.entity.BuildStatusTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.ihtsdo.buildcloud.core.entity.Build.Status.INTERRUPTED;
import static org.ihtsdo.buildcloud.core.entity.Build.Status.QUEUED;

@Service
@ConditionalOnProperty(name = "srs.manager", havingValue = "true")
public class InterruptedBuildRetryScheduler {

	private static final Logger LOGGER = LoggerFactory.getLogger(InterruptedBuildRetryScheduler.class);

	private final BuildStatusTrackerService trackerService;
	private final InterruptedBuildRetryProcessor processor;

	@Value("${srs.build.retry.enabled:true}")
	private boolean retryEnabled;

	@Value("${srs.build.interrupted.max-age-days:1}")
	private int interruptedMaxAgeDays;

	public InterruptedBuildRetryScheduler(BuildStatusTrackerService trackerService,
										  InterruptedBuildRetryProcessor processor) {
		this.trackerService = trackerService;
		this.processor = processor;
	}

	@Scheduled(fixedDelayString = "${srs.build.retry.fixed-delay-millis:300000}")
	public void retryInterruptedBuilds() {
		if (!retryEnabled) {
			return;
		}

		List<BuildStatusTracker> interruptedTrackers = trackerService.findByStatus(INTERRUPTED.name());
		if (interruptedTrackers != null && !interruptedTrackers.isEmpty()) {
			for (BuildStatusTracker tracker : interruptedTrackers) {
				try {
					if (isOlderThanConfiguredAge(tracker)) {
						LOGGER.info("Skipping interrupted build [{}:{}:{}] because it is older than {} day(s).",
								tracker.getReleaseCenterKey(), tracker.getProductKey(), tracker.getBuildId(),
								interruptedMaxAgeDays);
						continue;
					}
					processor.retryIfLatestInterrupted(tracker);
				} catch (Exception e) {
					LOGGER.error("Failed to retry interrupted build [{}:{}:{}].",
							tracker.getReleaseCenterKey(), tracker.getProductKey(), tracker.getBuildId(), e);
				}
			}
		}

		// Also check for builds stuck in QUEUED for too long
		failStuckQueuedBuilds();
	}

	private void failStuckQueuedBuilds() {
		List<BuildStatusTracker> queuedTrackers = trackerService.findByStatus(QUEUED.name());
		if (queuedTrackers == null || queuedTrackers.isEmpty()) {
			return;
		}

		for (BuildStatusTracker tracker : queuedTrackers) {
			try {
				processor.failIfStillQueued(tracker);
			} catch (Exception e) {
				LOGGER.error("Failed to mark stuck queued build [{}:{}:{}] as FAILED.",
						tracker.getReleaseCenterKey(), tracker.getProductKey(), tracker.getBuildId(), e);
			}
		}
	}

	private boolean isOlderThanConfiguredAge(BuildStatusTracker tracker) {
		if (tracker.getStartTime() == null) {
			return true;
		}
		// If max age is zero or negative, treat as "no age limit"
		if (interruptedMaxAgeDays <= 0) {
			return false;
		}
		long maxAgeMillis = TimeUnit.DAYS.toMillis(interruptedMaxAgeDays);
		long ageMillis = System.currentTimeMillis() - tracker.getStartTime().getTime();
		return ageMillis > maxAgeMillis;
	}
}

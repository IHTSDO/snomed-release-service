package org.ihtsdo.buildcloud.core.service.manager;

import org.ihtsdo.buildcloud.core.dao.BuildDAO;
import org.ihtsdo.buildcloud.core.dao.BuildStatusTrackerDao;
import org.ihtsdo.buildcloud.core.entity.Build;
import org.ihtsdo.buildcloud.core.entity.BuildStatusTracker;
import org.ihtsdo.buildcloud.core.entity.BuildConfiguration;
import org.ihtsdo.buildcloud.core.entity.Product;
import org.ihtsdo.buildcloud.core.entity.helper.EntityHelper;
import org.ihtsdo.buildcloud.core.entity.helper.TestEntityGenerator;
import org.ihtsdo.buildcloud.core.service.BuildService;
import org.ihtsdo.buildcloud.core.service.ProductService;
import org.ihtsdo.buildcloud.core.service.helper.FilterOption;
import org.ihtsdo.buildcloud.test.AbstractTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.EnumSet;
import java.util.concurrent.TimeUnit;

import static org.ihtsdo.buildcloud.core.entity.Build.Status.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Scheduler tests using the real Spring/H2/Liquibase context (via {@link AbstractTest})
 * and a real {@link BuildStatusTrackerDao}, without using mocks.
 */
@ExtendWith(SpringExtension.class)
@TestPropertySource(properties = "srs.worker=false")
@Transactional
class InterruptedBuildRetrySchedulerTest extends AbstractTest {

	@Autowired
	private InterruptedBuildRetryScheduler scheduler;

	@Autowired
	private BuildStatusTrackerDao trackerDao; // real JPA/H2 implementation

	@Autowired
	private BuildService buildService;

	@Autowired
	private BuildDAO buildDAO;

	@Autowired
	private ProductService productService;

	@Autowired
	private InterruptedBuildRetryProcessor processor;

	/**
	 * When the latest build for a product is INTERRUPTED and under the retry limit,
	 * the scheduler should clone it, queue a retry, leave the original tracker as
	 * INTERRUPTED but record the retry build id, and initialise the retry tracker
	 * with retryCount = original+1.
	 */
	@Test
	void retryInterruptedBuilds_whenLatestAndUnderRetryLimit_clonesAndQueues() throws IOException {
		// Arrange
		configureScheduler(3, 60_000L); // plenty of time before queue timeout matters

		String releaseCenterKey = EntityHelper.formatAsBusinessKey(TestEntityGenerator.releaseCenterShortNames[0]);
		Product product = loadFirstProduct(releaseCenterKey);
		String productKey = product.getBusinessKey();

		Build interruptedBuild = createBuild(releaseCenterKey, productKey, "base-build");

		// Mark tracker as INTERRUPTED and under retry limit
		BuildStatusTracker tracker = trackerDao.findByProductKeyAndBuildId(productKey, interruptedBuild.getId());
		assertNotNull(tracker);
		tracker.setStatus(INTERRUPTED.name());
		tracker.setRetryCount(0);
		trackerDao.update(tracker);

		// Act
		scheduler.retryInterruptedBuilds();

		// Original tracker still INTERRUPTED but linked to the retry build
		BuildStatusTracker originalAfter = trackerDao.findByProductKeyAndBuildId(productKey, interruptedBuild.getId());
		assertNotNull(originalAfter);
		assertEquals(INTERRUPTED.name(), originalAfter.getStatus());

		// New tracker (latest for this product) has retryCount = 1
		BuildStatusTracker latestTracker = trackerDao.findLatestByReleaseCenterAndProduct(releaseCenterKey, productKey);
		assertNotNull(latestTracker);
		assertNotEquals(interruptedBuild.getId(), latestTracker.getBuildId());
		assertEquals(1, latestTracker.getRetryCount());
		// And original tracker points at the new retry build
		assertEquals(latestTracker.getBuildId(), originalAfter.getNextRetryBuildId());
	}

	/**
	 * If there is a newer build for the same product, the scheduler must not retry
	 * the older interrupted build.
	 */
	@Test
	void retryInterruptedBuilds_whenNewerBuildExists_skipsOld() throws IOException {
		configureScheduler(3, 60_000L);

		String releaseCenterKey = EntityHelper.formatAsBusinessKey(TestEntityGenerator.releaseCenterShortNames[0]);
		Product product = loadFirstProduct(releaseCenterKey);
		String productKey = product.getBusinessKey();

		// Older interrupted build
		Build oldBuild = createBuild(releaseCenterKey, productKey, "old-build");
		BuildStatusTracker oldTracker = trackerDao.findByProductKeyAndBuildId(productKey, oldBuild.getId());
		assertNotNull(oldTracker);
		oldTracker.setStatus(INTERRUPTED.name());
		trackerDao.update(oldTracker);

		// Newer build for the same product
		Build newBuild = createBuild(releaseCenterKey, productKey, "newer-build");
		BuildStatusTracker newTracker = trackerDao.findByProductKeyAndBuildId(productKey, newBuild.getId());
		assertNotNull(newTracker);

		scheduler.retryInterruptedBuilds();

		// Old tracker remains INTERRUPTED
		BuildStatusTracker reloadedOld = trackerDao.findByProductKeyAndBuildId(productKey, oldBuild.getId());
		assertEquals(INTERRUPTED.name(), reloadedOld.getStatus());

		// And the old tracker has not been linked to any retry build
		assertNull(reloadedOld.getNextRetryBuildId());
	}

	/**
	 * When retryCount has reached the configured interruptedMaxRetries, the scheduler
	 * should mark the interrupted build as FAILED and not schedule another retry.
	 */
	@Test
	void retryInterruptedBuilds_whenAtMaxRetries_marksFailedAndSkipsRetry() throws IOException {
		configureScheduler(2, 3600000L); // max retries = 2

		String releaseCenterKey = EntityHelper.formatAsBusinessKey(TestEntityGenerator.releaseCenterShortNames[0]);
		Product product = loadFirstProduct(releaseCenterKey);
		String productKey = product.getBusinessKey();

		Build interruptedBuild = createBuild(releaseCenterKey, productKey, "interrupted-max");

		BuildStatusTracker tracker = trackerDao.findByProductKeyAndBuildId(productKey, interruptedBuild.getId());
		assertNotNull(tracker);
		tracker.setStatus(INTERRUPTED.name());
		tracker.setRetryCount(2); // already at max
		trackerDao.update(tracker);

		scheduler.retryInterruptedBuilds();

		// Build marked FAILED and tracker updated
		BuildStatusTracker updated = trackerDao.findByProductKeyAndBuildId(productKey, interruptedBuild.getId());
		assertEquals(FAILED.name(), updated.getStatus());

		Build reloadedBuild = buildService.find(releaseCenterKey, productKey, interruptedBuild.getId(),
				true, true, null, null);
		assertEquals(FAILED, reloadedBuild.getStatus());
	}

	/**
	 * Interrupted builds older than one day should not be retried.
	 */
	@Test
	void retryInterruptedBuilds_whenOlderThanOneDay_doesNotRetry() throws Exception {
		configureScheduler(3, 60_000L);

		String releaseCenterKey = EntityHelper.formatAsBusinessKey(TestEntityGenerator.releaseCenterShortNames[0]);
		Product product = loadFirstProduct(releaseCenterKey);
		String productKey = product.getBusinessKey();

		// Create an interrupted build with a tracker startTime more than one day old
		Build interruptedBuild = createBuild(releaseCenterKey, productKey, "old-interrupted-build");
		BuildStatusTracker tracker = trackerDao.findByProductKeyAndBuildId(productKey, interruptedBuild.getId());
		assertNotNull(tracker);
		tracker.setStatus(INTERRUPTED.name());

		// Set startTime to 2 days ago so it should be considered too old to retry
		long twoDaysAgoMillis = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(2);
		Timestamp twoDaysAgo = new Timestamp(twoDaysAgoMillis);
		var startTimeField = BuildStatusTracker.class.getDeclaredField("startTime");
		startTimeField.setAccessible(true);
		startTimeField.set(tracker, twoDaysAgo);
		trackerDao.update(tracker);

		// Act
		scheduler.retryInterruptedBuilds();

		// Assert: tracker is still INTERRUPTED and has not been linked to a retry build
		BuildStatusTracker reloaded = trackerDao.findByProductKeyAndBuildId(productKey, interruptedBuild.getId());
		assertNotNull(reloaded);
		assertEquals(INTERRUPTED.name(), reloaded.getStatus());
		assertNull(reloaded.getNextRetryBuildId());
	}

	private void configureScheduler(InterruptedBuildRetryScheduler scheduler, int maxRetries, long queueTimeoutMillis) {
		try {
			// Override processor configuration for tests using its package-private setters
			processor.setInterruptedMaxRetries(maxRetries);
			processor.setQueuedTimeoutMillis(queueTimeoutMillis);

			// Ensure the scheduler is enabled
			var enabledField = InterruptedBuildRetryScheduler.class.getDeclaredField("retryEnabled");
			enabledField.setAccessible(true);
			enabledField.setBoolean(scheduler, true);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	private void configureScheduler(int maxRetries, long queueTimeoutMillis) {
		configureScheduler(scheduler, maxRetries, queueTimeoutMillis);
	}

	private Product loadFirstProduct(String releaseCenterKey) {
		EnumSet<FilterOption> filterOptions = EnumSet.of(FilterOption.INCLUDE_LEGACY);
		Page<Product> page = productService.findAll(releaseCenterKey, filterOptions, PageRequest.of(0, 10), false);
		assertFalse(page.getContent().isEmpty(), "Expected at least one product for tests");
		return page.getContent().get(0);
	}

	private Build createBuild(String releaseCenterKey, String productKey, String buildId) throws IOException {
		// Persist a minimal build directly via BuildDAO so BuildService.find(...) in the scheduler can load it.
		Build build = new Build(buildId, releaseCenterKey, productKey, PENDING.name());
		build.setBuildUser("test-user");
		// Provide a basic configuration so cloneBuild() can safely manipulate it.
		BuildConfiguration configuration = new BuildConfiguration();
		configuration.setBuildName(buildId + "-name");
		configuration.setDefaultBranchPath("MAIN");
		build.setConfiguration(configuration);
		buildDAO.save(build);
		// Create a matching tracker row
		BuildStatusTracker tracker = new BuildStatusTracker();
		tracker.setReleaseCenterKey(releaseCenterKey);
		tracker.setProductKey(productKey);
		tracker.setBuildId(buildId);
		tracker.setStatus(PENDING.name());
		trackerDao.save(tracker);

		return build;
	}
}



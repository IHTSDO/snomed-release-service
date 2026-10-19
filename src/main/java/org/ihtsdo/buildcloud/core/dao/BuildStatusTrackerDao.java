package org.ihtsdo.buildcloud.core.dao;

import org.ihtsdo.buildcloud.core.entity.BuildStatusTracker;

import java.util.Collection;
import java.util.List;

public interface BuildStatusTrackerDao extends EntityDAO<BuildStatusTracker> {

	List<BuildStatusTracker> findByProductAndStatus(String productKey, String... status);

	BuildStatusTracker findByRvfRunIdAndBuildId(String runId, String buildId);

	BuildStatusTracker findByProductKeyAndBuildId(String productKey, String buildId);

	List<BuildStatusTracker> findAllByProductKeyAndBuildId(String productKey, String buildId);

	/**
	 * Find all trackers for the given builds of a product, ordered by start time descending,
	 * so the first tracker found for each build ID is its latest attempt.
	 */
	List<BuildStatusTracker> findAllByProductKeyAndBuildIds(String productKey, Collection<String> buildIds);

	/**
	 * Find the most recently started build for a given release center and product.
	 */
	BuildStatusTracker findLatestByReleaseCenterAndProduct(String releaseCenterKey, String productKey);

	List<BuildStatusTracker> findByStatus(String... status);
}

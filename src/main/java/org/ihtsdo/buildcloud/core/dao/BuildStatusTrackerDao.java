package org.ihtsdo.buildcloud.core.dao;

import org.ihtsdo.buildcloud.core.entity.BuildStatusTracker;

import java.util.List;

public interface BuildStatusTrackerDao extends EntityDAO<BuildStatusTracker> {

	List<BuildStatusTracker> findByProductAndStatus(String productKey, String... status);

	BuildStatusTracker findByRvfRunIdAndBuildId(String runId, String buildId);

	BuildStatusTracker findByProductKeyAndBuildId(String productKey, String buildId);
}

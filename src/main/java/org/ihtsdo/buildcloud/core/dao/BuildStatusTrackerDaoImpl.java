package org.ihtsdo.buildcloud.core.dao;

import org.hibernate.query.Query;
import org.ihtsdo.buildcloud.core.entity.BuildStatusTracker;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class BuildStatusTrackerDaoImpl extends EntityDAOImpl<BuildStatusTracker> implements BuildStatusTrackerDao {

	protected BuildStatusTrackerDaoImpl() {
		super(BuildStatusTracker.class);
	}

	@Override
	public List<BuildStatusTracker> findByProductAndStatus(String productKey, String... status) {
		Query query = getCurrentSession().createQuery(
				"select statusTracker " +
						"from BuildStatusTracker statusTracker " +
						"where statusTracker.productKey = :productKey " +
						" and statusTracker.status in (:status)");
		query.setParameter("productKey", productKey);
		query.setParameterList("status", status);
		return query.list();
	}

	@Override
	public BuildStatusTracker findByRvfRunId(String rvfRunId) {
		Query query = getCurrentSession().createQuery(
				"select statusTracker " +
						"from BuildStatusTracker statusTracker " +
						"where statusTracker.rvfRunId = :rvfRunId");
		query.setParameter("rvfRunId", rvfRunId);
		return (BuildStatusTracker) query.uniqueResult();
	}

	@Override
	public BuildStatusTracker findByProductKeyAndBuildId(String productKey, String buildId) {
		Query query = getCurrentSession().createQuery(
				"select statusTracker " +
						"from BuildStatusTracker statusTracker " +
						"where statusTracker.productKey = :productKey " +
						" and statusTracker.buildId = :buildId");
		query.setParameter("productKey", productKey);
		query.setParameter("buildId", buildId);
		return (BuildStatusTracker) query.uniqueResult();
	}
}

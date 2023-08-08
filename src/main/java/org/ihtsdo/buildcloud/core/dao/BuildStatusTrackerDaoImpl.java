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
		Query<BuildStatusTracker> query = getCurrentSession().createQuery(
				"select statusTracker " +
						"from BuildStatusTracker statusTracker " +
						"where statusTracker.productKey = :productKey " +
						" and statusTracker.status in (:status)", BuildStatusTracker.class);
		query.setParameter("productKey", productKey);
		query.setParameterList("status", status);
		return query.list();
	}

	@Override
	public BuildStatusTracker findByRvfRunId(String rvfRunId) {
		Query<BuildStatusTracker> query = getCurrentSession().createQuery(
				"select statusTracker " +
						"from BuildStatusTracker statusTracker " +
						"where statusTracker.rvfRunId = :rvfRunId", BuildStatusTracker.class);
		query.setParameter("rvfRunId", rvfRunId);
		return query.uniqueResult();
	}

	@Override
	public BuildStatusTracker findByProductKeyAndBuildId(String productKey, String buildId) {
		Query<BuildStatusTracker> query = getCurrentSession().createQuery(
				"select statusTracker " +
						"from BuildStatusTracker statusTracker " +
						"where statusTracker.productKey = :productKey " +
						" and statusTracker.buildId = :buildId", BuildStatusTracker.class);
		query.setParameter("productKey", productKey);
		query.setParameter("buildId", buildId);
		return query.uniqueResult();
	}
}

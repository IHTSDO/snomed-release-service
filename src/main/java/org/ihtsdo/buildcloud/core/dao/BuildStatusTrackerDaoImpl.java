package org.ihtsdo.buildcloud.core.dao;

import org.hibernate.query.Query;
import org.ihtsdo.buildcloud.core.entity.BuildStatusTracker;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class BuildStatusTrackerDaoImpl extends EntityDAOImpl<BuildStatusTracker> implements BuildStatusTrackerDao {

    public static final String PRODUCT_KEY = "productKey";

    protected BuildStatusTrackerDaoImpl() {
		super(BuildStatusTracker.class);
	}

	@Override
	public List<BuildStatusTracker> findByProductAndStatus(String productKey, String... status) {
		Query<BuildStatusTracker> query = getCurrentSession().createQuery(
				"select statusTracker " +
						"from BuildStatusTracker statusTracker " +
						"where statusTracker.productKey = :productKey " +
						" and statusTracker.status in (:status)",
				BuildStatusTracker.class);
		query.setParameter(PRODUCT_KEY, productKey);
		query.setParameterList("status", status);
		return query.list();
	}

	@Override
	public BuildStatusTracker findByRvfRunIdAndBuildId(String rvfRunId, String buildId) {
		Query<BuildStatusTracker> query = getCurrentSession().createQuery(
				"select statusTracker " +
						"from BuildStatusTracker statusTracker " +
						"where statusTracker.rvfRunId = :rvfRunId and statusTracker.buildId = :buildId",
				BuildStatusTracker.class);
		query.setParameter("rvfRunId", rvfRunId);
		query.setParameter("buildId", buildId);
		return query.uniqueResult();
	}

	@Override
	public BuildStatusTracker findByProductKeyAndBuildId(String productKey, String buildId) {
		Query<BuildStatusTracker> query = getCurrentSession().createQuery(
				"select statusTracker " +
						"from BuildStatusTracker statusTracker " +
						"where statusTracker.productKey = :productKey " +
						" and statusTracker.buildId = :buildId",
				BuildStatusTracker.class);
		query.setParameter(PRODUCT_KEY, productKey);
		query.setParameter("buildId", buildId);
		return query.uniqueResult();
	}

	@Override
	public List<BuildStatusTracker> findByStatus(String... status) {
		Query<BuildStatusTracker> query = getCurrentSession().createQuery(
				"select statusTracker " +
						"from BuildStatusTracker statusTracker " +
						"where statusTracker.status in (:status)",
				BuildStatusTracker.class);
		query.setParameterList("status", status);
		return query.list();
	}

	@Override
	public BuildStatusTracker findLatestByReleaseCenterAndProduct(String releaseCenterKey, String productKey) {
		Query<BuildStatusTracker> query = getCurrentSession().createQuery(
				"select statusTracker " +
						"from BuildStatusTracker statusTracker " +
						"where statusTracker.releaseCenterKey = :releaseCenterKey " +
						" and statusTracker.productKey = :productKey " +
						"order by statusTracker.startTime desc",
				BuildStatusTracker.class);
		query.setParameter("releaseCenterKey", releaseCenterKey);
		query.setParameter(PRODUCT_KEY, productKey);
		query.setMaxResults(1);
		return query.uniqueResult();
	}
}

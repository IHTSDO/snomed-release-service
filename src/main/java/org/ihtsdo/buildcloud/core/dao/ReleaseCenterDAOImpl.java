package org.ihtsdo.buildcloud.core.dao;

import org.hibernate.query.Query;
import org.ihtsdo.buildcloud.core.entity.ReleaseCenter;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ReleaseCenterDAOImpl extends EntityDAOImpl<ReleaseCenter> implements ReleaseCenterDAO {

	protected ReleaseCenterDAOImpl() {
		super(ReleaseCenter.class);
	}

	@Override
	public List<ReleaseCenter> findAll() {
		Query<ReleaseCenter> query = getCurrentSession().createQuery(
				"select releaseCenter " +
						"from ReleaseCenter releaseCenter " +
						"order by releaseCenter.id ", ReleaseCenter.class);
		return query.list();
	}

	@Override
	public ReleaseCenter find(String businessKey) {
		Query<ReleaseCenter> query = getCurrentSession().createQuery(
				"select releaseCenter " +
						"from ReleaseCenter releaseCenter " +
						"where releaseCenter.businessKey = :businessKey " +
						"order by releaseCenter.id ", ReleaseCenter.class);
		query.setParameter("businessKey", businessKey);
		return query.uniqueResult();
	}


}

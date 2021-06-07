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
	@SuppressWarnings("unchecked")
	public List<ReleaseCenter> findAll() {
		Query query = getCurrentSession().createQuery(
				"select releaseCenter " +
						"from ReleaseCenter releaseCenter " +
						"order by releaseCenter.id ");
		return query.list();
	}

	@Override
	public ReleaseCenter find(String businessKey) {
		Query query = getCurrentSession().createQuery(
				"select releaseCenter " +
						"from ReleaseCenter releaseCenter " +
						"where releaseCenter.businessKey = :businessKey " +
						"order by releaseCenter.id ");
		query.setParameter("businessKey", businessKey);
		return (ReleaseCenter) query.uniqueResult();
	}


}

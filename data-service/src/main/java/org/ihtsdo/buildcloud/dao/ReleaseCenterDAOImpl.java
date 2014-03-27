package org.ihtsdo.buildcloud.dao;

import org.hibernate.Query;
import org.ihtsdo.buildcloud.entity.ReleaseCenter;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ReleaseCenterDAOImpl extends EntityDAOImpl<ReleaseCenter> implements ReleaseCenterDAO {

	@Override
	public List<ReleaseCenter> findAll(String authenticatedId) {
		Query query = getCurrentSession().createQuery(
				"select releaseCenter " +
				"from ReleaseCenterMembership m " +
				"where m.user.oauthId = :oauthId " +
				"order by m.releaseCenter.id ");
		query.setString("oauthId", authenticatedId);
		return query.list();
	}

	@Override
	public ReleaseCenter find(String businessKey, String authenticatedId) {
		Query query = getCurrentSession().createQuery(
				"select releaseCenter " +
				"from ReleaseCenterMembership m " +
				"where m.user.oauthId = :oauthId " +
				"and m.releaseCenter.businessKey = :businessKey " +
				"order by m.releaseCenter.id ");
		query.setString("oauthId", authenticatedId);
		query.setString("businessKey", businessKey);
		return (ReleaseCenter) query.uniqueResult();
	}


}

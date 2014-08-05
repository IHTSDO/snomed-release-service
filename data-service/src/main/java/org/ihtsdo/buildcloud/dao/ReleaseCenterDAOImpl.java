package org.ihtsdo.buildcloud.dao;

import org.hibernate.Query;
import org.ihtsdo.buildcloud.entity.ReleaseCenter;
import org.ihtsdo.buildcloud.entity.User;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ReleaseCenterDAOImpl extends EntityDAOImpl<ReleaseCenter> implements ReleaseCenterDAO {

	protected ReleaseCenterDAOImpl() {
		super(ReleaseCenter.class);
	}

	@Override
	public List<ReleaseCenter> findAll(User user) {
		Query query = getCurrentSession().createQuery(
				"select releaseCenter " +
						"from ReleaseCenterMembership m " +
						"where m.user = :user " +
						"order by m.releaseCenter.id ");
		query.setEntity("user", user);
		return query.list();
	}

	@Override
	public ReleaseCenter find(String businessKey, User user) {
		Query query = getCurrentSession().createQuery(
				"select releaseCenter " +
						"from ReleaseCenterMembership m " +
						"where m.user = :user " +
						"and m.releaseCenter.businessKey = :businessKey " +
						"order by m.releaseCenter.id ");
		query.setEntity("user", user);
		query.setString("businessKey", businessKey);
		return (ReleaseCenter) query.uniqueResult();
	}


}

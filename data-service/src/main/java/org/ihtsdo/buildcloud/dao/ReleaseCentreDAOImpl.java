package org.ihtsdo.buildcloud.dao;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.ihtsdo.buildcloud.entity.ReleaseCentre;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ReleaseCentreDAOImpl implements ReleaseCentreDAO {

	@Autowired
	private SessionFactory sessionFactory;

	@Override
	public List<ReleaseCentre> findAll(String authenticatedId) {
		Query query = getCurrentSession().createQuery(
				"select releaseCentre " +
				"from ReleaseCentreMembership m " +
				"where m.user.oauthId = :oauthId");
		query.setString("oauthId", authenticatedId);
		return query.list();
	}

	@Override
	public ReleaseCentre find(String businessKey, String authenticatedId) {
		Query query = getCurrentSession().createQuery(
				"select releaseCentre " +
				"from ReleaseCentreMembership m " +
				"where m.user.oauthId = :oauthId " +
				"and m.releaseCentre.businessKey = :businessKey");
		query.setString("oauthId", authenticatedId);
		query.setString("businessKey", businessKey);
		return (ReleaseCentre) query.uniqueResult();
	}

	@Override
	public void save(ReleaseCentre entity) {
		getCurrentSession().save(entity);
	}

	private Session getCurrentSession() {
		return sessionFactory.getCurrentSession();
	}

}

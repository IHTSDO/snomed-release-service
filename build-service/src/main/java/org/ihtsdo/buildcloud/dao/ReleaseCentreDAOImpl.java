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
	public List<ReleaseCentre> findAll(String oauthId) {
		Query query = getCurrentSession().createQuery(
				"select releaseCentre " +
				"from ReleaseCentreMembership m " +
				"where m.user.oauthId = :oauthId");
		query.setString("oauthId", oauthId);
		return query.list();
	}

	@Override
	public ReleaseCentre find(String businessKey, String oauthId) {
		Query query = getCurrentSession().createQuery(
				"select releaseCentre " +
				"from ReleaseCentreMembership m " +
				"where m.user.oauthId = :oauthId " +
				"and m.releaseCentre.businessKey = :businessKey");
		query.setString("oauthId", oauthId);
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

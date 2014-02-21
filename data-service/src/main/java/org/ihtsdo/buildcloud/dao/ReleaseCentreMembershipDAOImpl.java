package org.ihtsdo.buildcloud.dao;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.ihtsdo.buildcloud.entity.ReleaseCentreMembership;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class ReleaseCentreMembershipDAOImpl implements ReleaseCentreMembershipDAO {

	@Autowired
	private SessionFactory sessionFactory;

	@Override
	public void save(ReleaseCentreMembership entity) {
		getCurrentSession().save(entity);
	}

	private Session getCurrentSession() {
		return sessionFactory.getCurrentSession();
	}

}

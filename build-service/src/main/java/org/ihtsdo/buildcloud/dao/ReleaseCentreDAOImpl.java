package org.ihtsdo.buildcloud.dao;

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
	public List<ReleaseCentre> getReleaseCentres() {
		return getCurrentSession().createQuery("from ReleaseCentre").list();
	}

	@Override
	public void save(ReleaseCentre releaseCentre) {
		getCurrentSession().save(releaseCentre);
	}

	private Session getCurrentSession() {
		return sessionFactory.getCurrentSession();
	}

}

package org.ihtsdo.buildcloud.dao;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public abstract class EntityDAOImpl<T> implements EntityDAO<T> {

	@Autowired
	private SessionFactory sessionFactory;

	@Override
	public void save(T entity) {
		getCurrentSession().save(entity);
	}

	@Override
	public void update(T entity) {
		getCurrentSession().update(entity);
	}

	protected Session getCurrentSession() {
		return sessionFactory.getCurrentSession();
	}

}

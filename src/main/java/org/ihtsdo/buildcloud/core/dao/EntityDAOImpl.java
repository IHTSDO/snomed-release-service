package org.ihtsdo.buildcloud.core.dao;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.io.Serializable;

@Repository
public abstract class EntityDAOImpl<T> implements EntityDAO<T> {

	private final Class<T> type;

	protected EntityDAOImpl(Class<T> type) {
		this.type = type;
	}

	@Autowired
	private SessionFactory sessionFactory;

	@Override
	public void save(T entity) {
		getCurrentSession().persist(entity);
	}

	@Override
	public T load(Serializable id) {
		return getCurrentSession().get(type, id);
	}

	@Override
	public void update(T entity) {
		getCurrentSession().merge(entity);
	}

	@Override
	public void delete(T entity) {
		getCurrentSession().remove(entity);
	}

	protected Session getCurrentSession() {
		return sessionFactory.getCurrentSession();
	}

}

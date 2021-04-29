package org.ihtsdo.buildcloud.dao;

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
		getCurrentSession().save(entity);
	}

	@Override
	public T load(Serializable id) {
		@SuppressWarnings("unchecked")
		T t = getCurrentSession().get(type, id);
		return t;
	}

	@Override
	public void update(T entity) {
		getCurrentSession().update(entity);
	}

	@Override
	public void delete(T entity) {
		getCurrentSession().delete(entity);
	}

	protected Session getCurrentSession() {
		return sessionFactory.getCurrentSession();
	}

}

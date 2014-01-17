package org.ihtsdo.buildcloud.dao;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public abstract class AbstractDAOImpl<T> {

	@Autowired
	private SessionFactory sessionFactory;

	public List<T> findAll() {
		return getCurrentSession().createQuery("from " + getEntityType()).list();
	}

	public T find(String businessKey) {
		Query query = getCurrentSession().createQuery("from " + getEntityType() + " where businessKey = :businessKey");
		query.setString("businessKey", businessKey);
		return (T) query.uniqueResult();
	}

	public void save(T entity) {
		getCurrentSession().save(entity);
	}

	protected Session getCurrentSession() {
		return sessionFactory.getCurrentSession();
	}

	protected abstract String getEntityType();

}

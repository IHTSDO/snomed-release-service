package org.ihtsdo.buildcloud.dao;

public interface EntityDAO<T> {

	void save(T entity);

	void update(T entity);

	void delete(T entity);

}

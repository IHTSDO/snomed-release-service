package org.ihtsdo.buildcloud.core.dao;

import java.io.Serializable;

public interface EntityDAO<T> {

	void save(T entity);

	T load(Serializable id);

	void update(T entity);

	void delete(T entity);

}

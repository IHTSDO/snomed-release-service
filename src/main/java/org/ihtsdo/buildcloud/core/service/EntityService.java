package org.ihtsdo.buildcloud.core.service;

public interface EntityService<T> {

	void update(T entity);

	void delete(T entity);

}

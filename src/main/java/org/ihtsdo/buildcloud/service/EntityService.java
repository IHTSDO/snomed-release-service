package org.ihtsdo.buildcloud.service;

public interface EntityService<T> {

	void update(T entity);

	void delete(T entity);

}

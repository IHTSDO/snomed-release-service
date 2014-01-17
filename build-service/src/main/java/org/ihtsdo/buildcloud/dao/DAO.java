package org.ihtsdo.buildcloud.dao;

import org.ihtsdo.buildcloud.entity.ReleaseCentre;

import java.util.List;

public abstract interface DAO<T> {

	List<T> findAll();
	T find(String businessKey);
	void save(T entity);

}

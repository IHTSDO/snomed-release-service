package org.ihtsdo.buildcloud.service;

import org.ihtsdo.buildcloud.dao.EntityDAO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public abstract class EntityServiceImpl<T> implements EntityService<T> {

	private final EntityDAO<T> dao;

	protected EntityServiceImpl(EntityDAO<T> dao) {
		this.dao = dao;
	}

	@Override
	public void update(T entity) {
		dao.update(entity);
	}

	@Override
	public void delete(T entity) {
		dao.delete(entity);
	}

}

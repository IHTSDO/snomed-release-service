package org.ihtsdo.buildcloud.service;

import org.ihtsdo.buildcloud.dao.ReleaseCentreDAO;
import org.ihtsdo.buildcloud.entity.ReleaseCentre;
import org.ihtsdo.buildcloud.service.helper.LazyInitializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ReleaseCentreServiceImpl implements ReleaseCentreService {

	@Autowired
	private ReleaseCentreDAO dao;

	@Override
	public List<ReleaseCentre> findAll() {
		return dao.findAll();
	}

	@Override
	public ReleaseCentre find(String businessKey) {
		return dao.find(businessKey);
	}

	@Override
	public ReleaseCentre find(String businessKey, LazyInitializer<ReleaseCentre> lazyInitializer) {
		ReleaseCentre entity = dao.find(businessKey);
		lazyInitializer.initializeLazyRelationships(entity);
		return entity;
	}

	@Override
	public void save(ReleaseCentre releaseCentre) {
		dao.save(releaseCentre);
	}

}

package org.ihtsdo.buildcloud.service;

import org.ihtsdo.buildcloud.dao.ReleaseCentreDAO;
import org.ihtsdo.buildcloud.entity.ReleaseCentre;
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
	public List<ReleaseCentre> findAll(String oauthId) {
		return dao.findAll(oauthId);
	}

	@Override
	public ReleaseCentre find(String businessKey, String oauthId) {
		return dao.find(businessKey, oauthId);
	}

}

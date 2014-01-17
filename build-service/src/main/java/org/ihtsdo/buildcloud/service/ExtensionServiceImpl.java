package org.ihtsdo.buildcloud.service;

import org.ihtsdo.buildcloud.dao.ExtensionDAO;
import org.ihtsdo.buildcloud.dao.ReleaseCentreDAO;
import org.ihtsdo.buildcloud.entity.Extension;
import org.ihtsdo.buildcloud.entity.ReleaseCentre;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ExtensionServiceImpl implements ExtensionService {

	@Autowired
	private ExtensionDAO dao;

	@Override
	public List<Extension> findAll() {
		return dao.findAll();
	}

	@Override
	public Extension find(String businessKey) {
		return dao.find(businessKey);
	}

	@Override
	public void save(Extension extension) {
		dao.save(extension);
	}

}

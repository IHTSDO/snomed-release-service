package org.ihtsdo.buildcloud.service;

import org.hibernate.Hibernate;
import org.ihtsdo.buildcloud.dao.ExtensionDAO;
import org.ihtsdo.buildcloud.dao.ReleaseCentreDAO;
import org.ihtsdo.buildcloud.entity.Extension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ExtensionServiceImpl implements ExtensionService {

	@Autowired
	private ExtensionDAO extensionDAO;

	@Autowired
	private ReleaseCentreDAO releaseCentreDAO;

	@Override
	public List<Extension> findAll(String releaseCentreBusinessKey, String oauthId) {
		List<Extension> extensions = releaseCentreDAO.find(releaseCentreBusinessKey, oauthId).getExtensions();
		Hibernate.initialize(extensions);
		return extensions;
	}

	@Override
	public Extension find(String releaseCentreBusinessKey, String extensionBusinessKey, String oauthId) {
		return extensionDAO.find(releaseCentreBusinessKey, extensionBusinessKey, oauthId);
	}
}

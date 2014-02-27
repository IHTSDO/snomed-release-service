package org.ihtsdo.buildcloud.service;

import org.hibernate.Hibernate;
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
public class ExtensionServiceImpl extends EntityServiceImpl<Extension> implements ExtensionService {

	@Autowired
	private ExtensionDAO extensionDAO;

	@Autowired
	private ReleaseCentreDAO releaseCentreDAO;

	@Autowired
	protected ExtensionServiceImpl(ExtensionDAO dao) {
		super(dao);
	}

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
	
	@Override
	public Extension create(String releaseCentreBusinessKey, String name, String authenticatedId) {
		ReleaseCentre releaseCentre = releaseCentreDAO.find(releaseCentreBusinessKey, authenticatedId);
		Extension extension = new Extension(name);
		releaseCentre.addExtension(extension);
		extensionDAO.save(extension);
		return extension;
	}	
}

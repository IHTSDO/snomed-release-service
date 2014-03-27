package org.ihtsdo.buildcloud.service;

import org.hibernate.Hibernate;
import org.ihtsdo.buildcloud.dao.ExtensionDAO;
import org.ihtsdo.buildcloud.dao.ReleaseCenterDAO;
import org.ihtsdo.buildcloud.entity.Extension;
import org.ihtsdo.buildcloud.entity.ReleaseCenter;
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
	private ReleaseCenterDAO releaseCenterDAO;

	@Autowired
	protected ExtensionServiceImpl(ExtensionDAO dao) {
		super(dao);
	}

	@Override
	public List<Extension> findAll(String releaseCenterBusinessKey, String oauthId) {
		List<Extension> extensions = releaseCenterDAO.find(releaseCenterBusinessKey, oauthId).getExtensions();
		Hibernate.initialize(extensions);
		return extensions;
	}

	@Override
	public Extension find(String releaseCenterBusinessKey, String extensionBusinessKey, String oauthId) {
		return extensionDAO.find(releaseCenterBusinessKey, extensionBusinessKey, oauthId);
	}
	
	@Override
	public Extension create(String releaseCenterBusinessKey, String name, String authenticatedId) {
		ReleaseCenter releaseCenter = releaseCenterDAO.find(releaseCenterBusinessKey, authenticatedId);
		Extension extension = new Extension(name);
		releaseCenter.addExtension(extension);
		extensionDAO.save(extension);
		return extension;
	}	
}

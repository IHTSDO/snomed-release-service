package org.ihtsdo.buildcloud.service;

import org.hibernate.Hibernate;
import org.ihtsdo.buildcloud.dao.ExtensionDAO;
import org.ihtsdo.buildcloud.dao.ReleaseCenterDAO;
import org.ihtsdo.buildcloud.entity.Extension;
import org.ihtsdo.buildcloud.entity.ReleaseCenter;
import org.ihtsdo.buildcloud.entity.User;
import org.ihtsdo.buildcloud.service.exception.ResourceNotFoundException;
import org.ihtsdo.buildcloud.service.helper.CompositeKeyHelper;
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
	public List<Extension> findAll(String releaseCenterBusinessKey, User authenticatedUser) throws ResourceNotFoundException {
		ReleaseCenter releaseCenter = releaseCenterDAO.find(releaseCenterBusinessKey, authenticatedUser);
		if (releaseCenter == null) {
			throw new ResourceNotFoundException ("Unable to find release center: " +  releaseCenterBusinessKey);
		}				
				
		List<Extension> extensions = releaseCenter.getExtensions();
		Hibernate.initialize(extensions);
		return extensions;
	}

	@Override
	public Extension find(String releaseCenterBusinessKey, String extensionBusinessKey, User authenticatedUser) {
		return extensionDAO.find(releaseCenterBusinessKey, extensionBusinessKey, authenticatedUser);
	}
	
	@Override
	public Extension create(String releaseCenterBusinessKey, String name, User authenticatedUser) throws ResourceNotFoundException {
		ReleaseCenter releaseCenter = releaseCenterDAO.find(releaseCenterBusinessKey, authenticatedUser);
		if (releaseCenter == null) {
			throw new ResourceNotFoundException ("Unable to find release center: " +  releaseCenterBusinessKey);
		}
		Extension extension = new Extension(name);
		releaseCenter.addExtension(extension);
		extensionDAO.save(extension);
		return extension;
	}	
}

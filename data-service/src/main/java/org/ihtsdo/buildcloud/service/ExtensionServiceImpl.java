package org.ihtsdo.buildcloud.service;

import org.hibernate.Hibernate;
import org.ihtsdo.buildcloud.dao.ExtensionDAO;
import org.ihtsdo.buildcloud.dao.ReleaseCenterDAO;
import org.ihtsdo.buildcloud.entity.Extension;
import org.ihtsdo.buildcloud.entity.ReleaseCenter;
import org.ihtsdo.buildcloud.entity.User;
import org.ihtsdo.buildcloud.entity.helper.EntityHelper;
import org.ihtsdo.buildcloud.service.exception.EntityAlreadyExistsException;
import org.ihtsdo.buildcloud.service.exception.ResourceNotFoundException;
import org.ihtsdo.buildcloud.service.security.SecurityHelper;
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
	public List<Extension> findAll(String releaseCenterBusinessKey) throws ResourceNotFoundException {
		ReleaseCenter releaseCenter = releaseCenterDAO.find(releaseCenterBusinessKey, SecurityHelper.getRequiredUser());
		if (releaseCenter == null) {
			throw new ResourceNotFoundException("Unable to find release center: " + releaseCenterBusinessKey);
		}

		List<Extension> extensions = releaseCenter.getExtensions();
		Hibernate.initialize(extensions);
		return extensions;
	}

	@Override
	public Extension find(String releaseCenterBusinessKey, String extensionBusinessKey) {
		return extensionDAO.find(releaseCenterBusinessKey, extensionBusinessKey, SecurityHelper.getRequiredUser());
	}

	@Override
	public Extension create(String releaseCenterBusinessKey, String name) throws ResourceNotFoundException, EntityAlreadyExistsException {
		User user = SecurityHelper.getRequiredUser();
		ReleaseCenter releaseCenter = releaseCenterDAO.find(releaseCenterBusinessKey, user);
		if (releaseCenter == null) {
			throw new ResourceNotFoundException("Unable to find release center: " + releaseCenterBusinessKey);
		}

		//Check that we don't already have one of these
		String extensionBusinessKey = EntityHelper.formatAsBusinessKey(name);
		Extension existingProduct = extensionDAO.find(releaseCenterBusinessKey, extensionBusinessKey, user);
		if (existingProduct != null) {
			throw new EntityAlreadyExistsException(name + " already exists.");
		}
		Extension extension = new Extension(name);
		releaseCenter.addExtension(extension);
		extensionDAO.save(extension);
		return extension;
	}
}

package org.ihtsdo.buildcloud.service;

import org.ihtsdo.buildcloud.entity.Extension;
import org.ihtsdo.buildcloud.entity.User;
import org.ihtsdo.buildcloud.service.exception.EntityAlreadyExistsException;
import org.ihtsdo.buildcloud.service.exception.ResourceNotFoundException;

import java.util.List;

public interface ExtensionService extends EntityService<Extension> {

	List<Extension> findAll(String releaseCenterBusinessKey, User authenticatedUser) throws ResourceNotFoundException;
	Extension find(String releaseCenterBusinessKey, String extensionBusinessKey, User authenticatedUser);
	Extension create(String releaseCenterBusinessKey, String name, User authenticatedUser) throws ResourceNotFoundException, EntityAlreadyExistsException;

}

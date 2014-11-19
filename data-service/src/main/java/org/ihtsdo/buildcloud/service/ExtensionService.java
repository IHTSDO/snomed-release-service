package org.ihtsdo.buildcloud.service;

import org.ihtsdo.buildcloud.entity.Extension;
import org.ihtsdo.buildcloud.service.exception.EntityAlreadyExistsException;
import org.ihtsdo.buildcloud.service.exception.ResourceNotFoundException;

import java.util.List;

public interface ExtensionService extends EntityService<Extension> {

	List<Extension> findAll(String releaseCenterBusinessKey) throws ResourceNotFoundException;

	Extension find(String releaseCenterBusinessKey, String extensionBusinessKey);

	Extension create(String releaseCenterBusinessKey, String name) throws ResourceNotFoundException, EntityAlreadyExistsException;

}

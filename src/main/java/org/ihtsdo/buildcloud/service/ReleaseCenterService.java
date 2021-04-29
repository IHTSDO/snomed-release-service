package org.ihtsdo.buildcloud.service;

import org.ihtsdo.buildcloud.entity.ReleaseCenter;
import org.ihtsdo.otf.rest.exception.EntityAlreadyExistsException;
import org.ihtsdo.otf.rest.exception.ResourceNotFoundException;

import java.util.List;

public interface ReleaseCenterService extends EntityService<ReleaseCenter> {

	List<ReleaseCenter> findAll();

	ReleaseCenter find(String businessKey) throws ResourceNotFoundException;

	ReleaseCenter create(String name, String shortName, String codeSystem) throws EntityAlreadyExistsException;
}

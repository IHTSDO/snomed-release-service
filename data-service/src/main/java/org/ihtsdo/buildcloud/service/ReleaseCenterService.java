package org.ihtsdo.buildcloud.service;

import org.ihtsdo.buildcloud.entity.ReleaseCenter;
import org.ihtsdo.buildcloud.service.exception.EntityAlreadyExistsException;
import org.ihtsdo.buildcloud.service.exception.ResourceNotFoundException;

import java.util.List;

public interface ReleaseCenterService extends EntityService<ReleaseCenter> {

	List<ReleaseCenter> findAll();

	ReleaseCenter find(String businessKey) throws ResourceNotFoundException;

	ReleaseCenter create(String name, String shortName) throws EntityAlreadyExistsException;

	void update(ReleaseCenter center);

}

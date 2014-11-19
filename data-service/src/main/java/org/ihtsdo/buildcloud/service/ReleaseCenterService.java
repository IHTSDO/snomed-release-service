package org.ihtsdo.buildcloud.service;

import org.ihtsdo.buildcloud.entity.ReleaseCenter;
import org.ihtsdo.buildcloud.service.exception.EntityAlreadyExistsException;

import java.util.List;

public interface ReleaseCenterService extends EntityService<ReleaseCenter> {

	List<ReleaseCenter> findAll();

	ReleaseCenter find(String businessKey);

	ReleaseCenter create(String name, String shortName) throws EntityAlreadyExistsException;

	void update(ReleaseCenter center);

}

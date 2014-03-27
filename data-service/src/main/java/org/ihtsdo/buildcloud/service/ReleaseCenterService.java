package org.ihtsdo.buildcloud.service;

import org.ihtsdo.buildcloud.entity.ReleaseCenter;

import java.util.List;

public interface ReleaseCenterService extends EntityService<ReleaseCenter> {

	List<ReleaseCenter> findAll(String oauthId);

	ReleaseCenter find(String businessKey, String oauthId);

	ReleaseCenter create(String name, String shortName, String oauthId);

	void update(ReleaseCenter center);

}

package org.ihtsdo.buildcloud.service;

import org.ihtsdo.buildcloud.entity.ReleaseCentre;

import java.util.List;

public interface ReleaseCentreService extends EntityService<ReleaseCentre> {

	List<ReleaseCentre> findAll(String oauthId);

	ReleaseCentre find(String businessKey, String oauthId);

	ReleaseCentre create(String name, String shortName, String oauthId);

	void update(ReleaseCentre centre);

}

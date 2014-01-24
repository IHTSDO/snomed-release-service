package org.ihtsdo.buildcloud.dao;

import org.ihtsdo.buildcloud.entity.ReleaseCentre;

import java.util.List;

public interface ReleaseCentreDAO {

	List<ReleaseCentre> findAll(String oauthId);

	ReleaseCentre find(String businessKey, String oauthId);

	void save(ReleaseCentre entity);

}

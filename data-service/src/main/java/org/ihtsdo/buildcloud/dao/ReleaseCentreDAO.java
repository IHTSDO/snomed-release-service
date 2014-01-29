package org.ihtsdo.buildcloud.dao;

import org.ihtsdo.buildcloud.entity.ReleaseCentre;

import java.util.List;

public interface ReleaseCentreDAO {

	List<ReleaseCentre> findAll(String authenticatedId);

	ReleaseCentre find(String businessKey, String authenticatedId);

	void save(ReleaseCentre entity);

}

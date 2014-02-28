package org.ihtsdo.buildcloud.dao;

import org.ihtsdo.buildcloud.entity.ReleaseCentre;

import java.util.List;

public interface ReleaseCentreDAO extends EntityDAO<ReleaseCentre> {

	List<ReleaseCentre> findAll(String authenticatedId);

	ReleaseCentre find(String businessKey, String authenticatedId);

}

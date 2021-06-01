package org.ihtsdo.buildcloud.core.dao;

import org.ihtsdo.buildcloud.core.entity.ReleaseCenter;

import java.util.List;

public interface ReleaseCenterDAO extends EntityDAO<ReleaseCenter> {

	List<ReleaseCenter> findAll();

	ReleaseCenter find(String businessKey);

}

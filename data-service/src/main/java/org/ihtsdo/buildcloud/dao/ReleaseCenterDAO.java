package org.ihtsdo.buildcloud.dao;

import org.ihtsdo.buildcloud.entity.ReleaseCenter;

import java.util.List;

public interface ReleaseCenterDAO extends EntityDAO<ReleaseCenter> {

	List<ReleaseCenter> findAll();

	ReleaseCenter find(String businessKey);

}

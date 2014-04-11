package org.ihtsdo.buildcloud.dao;

import org.ihtsdo.buildcloud.entity.ReleaseCenter;
import org.ihtsdo.buildcloud.entity.User;

import java.util.List;

public interface ReleaseCenterDAO extends EntityDAO<ReleaseCenter> {

	List<ReleaseCenter> findAll(User user);

	ReleaseCenter find(String businessKey, User user);

}

package org.ihtsdo.buildcloud.dao;

import org.ihtsdo.buildcloud.entity.Build;

import java.util.List;

public interface BuildDAO extends EntityDAO<Build> {

	List<Build> findAll(String authenticatedId);

	Build find(Long id, String authenticatedId);

}

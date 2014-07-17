package org.ihtsdo.buildcloud.dao;

import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.User;
import org.ihtsdo.buildcloud.service.helper.FilterOption;

import java.util.EnumSet;
import java.util.List;

public interface BuildDAO extends EntityDAO<Build> {

	List<Build> findAll(EnumSet<FilterOption> filterOptions, User user);
	
	List<Build> findAll(String releaseCenterBusinessKey, String extensionBusinessKey, EnumSet<FilterOption> filterOptions, User user);

	Build find(String releaseCenterBusinessKey, String extensionBusinessKey, String productBusinessKey, String buildBusinessKey, User user);

	Build find(Long id, User user);

}

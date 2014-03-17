package org.ihtsdo.buildcloud.dao;

import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.service.helper.FilterOption;

import java.util.EnumSet;
import java.util.List;

public interface BuildDAO extends EntityDAO<Build> {

	List<Build> findAll(EnumSet<FilterOption> filterOptions, String authenticatedId);
	
	List<Build> findAll(String releaseCentreBusinessKey, String extensionBusinessKey, EnumSet<FilterOption> filterOptions, String authenticatedId);	

	Build find(Long id, String authenticatedId);

}

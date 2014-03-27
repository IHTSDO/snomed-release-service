package org.ihtsdo.buildcloud.service;

import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.service.helper.FilterOption;

import java.io.IOException;
import java.util.EnumSet;
import java.util.List;

public interface BuildService extends EntityService<Build> {

	List<Build> findAll(EnumSet<FilterOption> filterOptions, String authenticatedId);

	Build find(String buildCompositeKey, String authenticatedId);
	
	List<Build> findForExtension(String releaseCenterBusinessKey, String extensionBusinessKey, EnumSet<FilterOption> filterOptions, String authenticatedId);
	
	List<Build> findForProduct(String releaseCenterBusinessKey, String extensionBusinessKey, String productBusinessKey, String authenticatedId);

	Build create(String releaseCenterBusinessKey, String extensionBusinessKey, String productBusinessKey, String name, String authenticatedId) throws Exception;
	
}

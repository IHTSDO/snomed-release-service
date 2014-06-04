package org.ihtsdo.buildcloud.service;

import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.User;
import org.ihtsdo.buildcloud.service.exception.BadRequestException;
import org.ihtsdo.buildcloud.service.helper.FilterOption;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;

public interface BuildService extends EntityService<Build> {

	List<Build> findAll(EnumSet<FilterOption> filterOptions, User authenticatedUser);

	Build find(String buildCompositeKey, User authenticatedUser);
	
	List<Build> findForExtension(String releaseCenterBusinessKey, String extensionBusinessKey, EnumSet<FilterOption> filterOptions, User authenticatedUser);
	
	List<Build> findForProduct(String releaseCenterBusinessKey, String extensionBusinessKey, String productBusinessKey, User authenticatedUser);

	Build create(String releaseCenterBusinessKey, String extensionBusinessKey, String productBusinessKey, String name, User authenticatedUser) throws Exception;

	Build update(String buildCompositeKey, Map<String, String> newPropertyValues, User authenticatedUser) throws BadRequestException;

}

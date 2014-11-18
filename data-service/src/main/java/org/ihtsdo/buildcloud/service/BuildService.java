package org.ihtsdo.buildcloud.service;

import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.User;
import org.ihtsdo.buildcloud.service.exception.BadRequestException;
import org.ihtsdo.buildcloud.service.exception.ResourceNotFoundException;
import org.ihtsdo.buildcloud.service.helper.FilterOption;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface BuildService extends EntityService<Build> {

	static final String EFFECTIVE_TIME = "effectiveTime";

	List<Build> findAll(Set<FilterOption> filterOptions, User authenticatedUser);

	Build find(String buildCompositeKey, User authenticatedUser) throws ResourceNotFoundException;

	Build find(String releaseCenterBusinessKey, String extensionBusinessKey, String productBusinessKey, String buildName, User authenticatedUser) throws ResourceNotFoundException;

	List<Build> findForExtension(String releaseCenterBusinessKey, String extensionBusinessKey, Set<FilterOption> filterOptions, User authenticatedUser);

	List<Build> findForProduct(String releaseCenterBusinessKey, String extensionBusinessKey, String productBusinessKey, User authenticatedUser) throws ResourceNotFoundException;

	Build create(String releaseCenterBusinessKey, String extensionBusinessKey, String productBusinessKey, String name, User authenticatedUser) throws Exception;

	Build update(String buildCompositeKey, Map<String, String> newPropertyValues, User authenticatedUser) throws BadRequestException, ResourceNotFoundException;

}

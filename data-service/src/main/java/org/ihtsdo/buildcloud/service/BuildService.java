package org.ihtsdo.buildcloud.service;

import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.service.exception.AuthenticationException;
import org.ihtsdo.buildcloud.service.exception.BusinessServiceException;
import org.ihtsdo.buildcloud.service.exception.ResourceNotFoundException;
import org.ihtsdo.buildcloud.service.helper.FilterOption;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface BuildService extends EntityService<Build> {

	static final String EFFECTIVE_TIME = "effectiveTime";

	List<Build> findAll(Set<FilterOption> filterOptions) throws AuthenticationException;

	Build find(String buildCompositeKey) throws BusinessServiceException;

	Build find(String releaseCenterBusinessKey, String extensionBusinessKey, String productBusinessKey, String buildName) throws ResourceNotFoundException;

	List<Build> findForExtension(String releaseCenterBusinessKey, String extensionBusinessKey, Set<FilterOption> filterOptions) throws AuthenticationException;

	List<Build> findForProduct(String releaseCenterBusinessKey, String extensionBusinessKey, String productBusinessKey) throws ResourceNotFoundException;

	Build create(String releaseCenterBusinessKey, String extensionBusinessKey, String productBusinessKey, String name) throws BusinessServiceException;

	Build update(String buildCompositeKey, Map<String, String> newPropertyValues) throws BusinessServiceException;

}

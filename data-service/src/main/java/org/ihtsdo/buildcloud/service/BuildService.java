package org.ihtsdo.buildcloud.service;

import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.service.exception.AuthenticationException;
import org.ihtsdo.buildcloud.service.exception.BusinessServiceException;
import org.ihtsdo.buildcloud.service.helper.FilterOption;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface BuildService extends EntityService<Build> {

	String NAME = "name";
	String EFFECTIVE_TIME = "effectiveTime";
	String README_HEADER = "readmeHeader";
	String JUST_PACKAGE = "justPackage";
	String FIRST_TIME_RELEASE = "firstTimeRelease";
	String PREVIOUS_PUBLISHED_PACKAGE = "previousPublishedPackage";
	String README_END_DATE = "readmeEndDate";
	String WORKBENCH_DATA_FIXES_REQUIRED = "workbenchDataFixesRequired";
	String CREATE_INFERRED_RELATIONSHIPS = "createInferredRelationships";
	String CUSTOM_REFSET_COMPOSITE_KEYS = "customRefsetCompositeKeys";
	String NEW_RF2_INPUT_FILES = "newRF2InputFiles";
	String TRUE = "true";

	List<Build> findAll(String releaseCenterKey, Set<FilterOption> filterOptions) throws AuthenticationException;

	Build find(String releaseCenterKey, String buildKey) throws BusinessServiceException;

	Build create(String releaseCenterKey, String name) throws BusinessServiceException;

	Build update(String releaseCenterKey, String buildKey, Map<String, String> newPropertyValues) throws BusinessServiceException;

}

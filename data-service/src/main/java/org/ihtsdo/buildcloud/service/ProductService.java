package org.ihtsdo.buildcloud.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ihtsdo.buildcloud.entity.Product;
import org.ihtsdo.buildcloud.service.exception.AuthenticationException;
import org.ihtsdo.buildcloud.service.exception.BusinessServiceException;
import org.ihtsdo.buildcloud.service.helper.FilterOption;

public interface ProductService extends EntityService<Product> {

	String NAME = "name";
	String EFFECTIVE_TIME = "effectiveTime";
	String README_HEADER = "readmeHeader";
	String JUST_PACKAGE = "justPackage";
	String FIRST_TIME_RELEASE = "firstTimeRelease";
	String BETA_RELEASE = "betaRelease";
	String PREVIOUS_PUBLISHED_PACKAGE = "previousPublishedPackage";
	String README_END_DATE = "readmeEndDate";
	String WORKBENCH_DATA_FIXES_REQUIRED = "workbenchDataFixesRequired";
	String CREATE_INFERRED_RELATIONSHIPS = "createInferredRelationships";
	String CREATE_LEGACY_IDS = "createLegacyIds";
	String CUSTOM_REFSET_COMPOSITE_KEYS = "customRefsetCompositeKeys";
	String NEW_RF2_INPUT_FILES = "newRF2InputFiles";
	String TRUE = "true";
	String PREVIOUS_INTERNATIONAL_RELEASE = "previousInternationalRelease";
	String PREVIOUS_EXTENSION_RELEASE = "previousExtensionRelease";
	String ASSERTION_GROUP_NAMES = "assertionGroupNames";
	String EXTENSION_BASELINE_RELEASE = "extensionBaseLineRelease";

	List<Product> findAll(String releaseCenterKey, Set<FilterOption> filterOptions) throws AuthenticationException;

	Product find(String releaseCenterKey, String productKey) throws BusinessServiceException;

	Product create(String releaseCenterKey, String name) throws BusinessServiceException;

	Product update(String releaseCenterKey, String productKey, Map<String, String> newPropertyValues) throws BusinessServiceException;

}

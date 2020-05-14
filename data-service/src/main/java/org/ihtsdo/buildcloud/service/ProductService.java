package org.ihtsdo.buildcloud.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ihtsdo.buildcloud.entity.Product;
import org.ihtsdo.buildcloud.entity.ReleaseCenter;
import org.ihtsdo.buildcloud.service.helper.FilterOption;
import org.ihtsdo.otf.rest.exception.AuthenticationException;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;

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
	String INPUT_FILES_FIXES_REQUIRED = "inputFilesFixesRequired";
	String CREATE_LEGACY_IDS = "createLegacyIds";
	String CUSTOM_REFSET_COMPOSITE_KEYS = "customRefsetCompositeKeys";
	String NEW_RF2_INPUT_FILES = "newRF2InputFiles";
	String TRUE = "true";
	String PREVIOUS_INTERNATIONAL_RELEASE = "previousInternationalRelease";
	String PREVIOUS_EXTENSION_RELEASE = "previousExtensionRelease";
	String ASSERTION_GROUP_NAMES = "assertionGroupNames";
	String EXTENSION_DEPENDENCY_RELEASE = "extensionDependencyRelease";
	String DEPENDENCY_RELEASE_PACKAGE = "dependencyReleasePackage";
	String NAMESPACE_ID = "namespaceId";
	String MODULE_ID = "moduleId";
	String RELEASE_AS_AN_EDITION = "releaseExtensionAsAnEdition";
	String ENABLE_DROOLS = "enableDrools";
	String INCLUDED_PREV_RELEASE_FILES = "includePrevReleaseFiles";
	String DROOLS_RULES_GROUP_NAMES = "droolsRulesGroupNames";
	String ENABLE_MRCM = "enableMRCMValidation";
	String CLASSIFY_OUTPUT_FILES = "classifyOutputFiles";
	String LICENSE_STATEMENT = "licenseStatement";
	String RELEASE_INFORMATION_FIELDS = "releaseInformationFields";
	String USE_CLASSIFIER_PRECONDITION_CHECKS = "useClassifierPreConditionChecks";
	String CONCEPT_PREFERRED_TERMS = "conceptPreferredTerms";
	
	List<Product> findAll(String releaseCenterKey, Set<FilterOption> filterOptions) throws AuthenticationException;

	Product find(String releaseCenterKey, String productKey) throws BusinessServiceException;

	Product create(String releaseCenterKey, String name) throws BusinessServiceException;

	Product update(String releaseCenterKey, String productKey, Map<String, String> newPropertyValues) throws BusinessServiceException;

}

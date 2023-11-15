package org.ihtsdo.buildcloud.core.service;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.ihtsdo.buildcloud.core.entity.Product;
import org.ihtsdo.buildcloud.core.service.helper.FilterOption;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductService extends EntityService<Product> {

	String NAME = "name";
	String EFFECTIVE_TIME = "effectiveTime";
	String README_HEADER = "readmeHeader";
	String JUST_PACKAGE = "justPackage";
	String FIRST_TIME_RELEASE = "firstTimeRelease";
	String BETA_RELEASE = "betaRelease";
	String DAILY_BUILD = "dailyBuild";
	String PREVIOUS_PUBLISHED_PACKAGE = "previousPublishedPackage";
	String README_END_DATE = "readmeEndDate";
	String WORKBENCH_DATA_FIXES_REQUIRED = "workbenchDataFixesRequired";
	String INPUT_FILES_FIXES_REQUIRED = "inputFilesFixesRequired";
	String CREATE_LEGACY_IDS = "createLegacyIds";
	String CUSTOM_REFSET_COMPOSITE_KEYS = "customRefsetCompositeKeys";
	String NEW_RF2_INPUT_FILES = "newRF2InputFiles";
	String TRUE = "true";
	String ASSERTION_GROUP_NAMES = "assertionGroupNames";
	String EXTENSION_DEPENDENCY_RELEASE = "extensionDependencyRelease";
	String PREVIOUS_EDITION_DEPENDENCY_EFFECTIVE_DATE = "previousEditionDependencyEffectiveDate";
	String NAMESPACE_ID = "namespaceId";
	String DEFAULT_MODULE_ID = "defaultModuleId";
	String MODULE_IDS = "moduleIds";
	String RELEASE_AS_AN_EDITION = "releaseExtensionAsAnEdition";
	String ENABLE_DROOLS = "enableDrools";
	String ENABLE_MRCM = "enableMRCMValidation";
	String INCLUDED_PREV_RELEASE_FILES = "includePrevReleaseFiles";
	String EXCLUDE_REFSET_DESCRIPTOR_MEMBERS = "excludeRefsetDescriptorMembers";
	String EXCLUDE_LANGUAGE_REFSET_IDS = "excludeLanguageRefsetIds";
	String DROOLS_RULES_GROUP_NAMES = "droolsRulesGroupNames";
	String CLASSIFY_OUTPUT_FILES = "classifyOutputFiles";
	String LICENSE_STATEMENT = "licenseStatement";
	String RELEASE_INFORMATION_FIELDS = "releaseInformationFields";
	String ADDITIONAL_RELEASE_INFORMATION_FIELDS = "additionalReleaseInformationFields";
	String USE_CLASSIFIER_PRECONDITION_CHECKS = "useClassifierPreConditionChecks";
	String CONCEPT_PREFERRED_TERMS = "conceptPreferredTerms";
	String DEFAULT_BRANCH_PATH = "defaultBranchPath";
	String INTERNATIONAL = "international";
	String SNOMEDCT = "SNOMEDCT";
	
	Page<Product> findAll(String releaseCenterKey, Set<FilterOption> filterOptions, Pageable pageable, boolean includedLatestBuildStatusAndTags);

	Page<Product> findHiddenProducts(String releaseCenterKey, Pageable pageable);

	Product find(String releaseCenterKey, String productKey, boolean includedLatestBuildStatusAndTags);

	Product create(String releaseCenterKey, String name) throws BusinessServiceException;

	Product update(String releaseCenterKey, String productKey, Map<String, String> newPropertyValues) throws BusinessServiceException;

    void updateVisibility(String releaseCenterKey, String productKey, boolean visibility) throws IOException;

    void upgradeDependantVersion(String releaseCenterKey, String productKey) throws BusinessServiceException;
}

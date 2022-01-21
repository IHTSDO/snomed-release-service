package org.ihtsdo.buildcloud.core.service;

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
	String PREVIOUS_INTERNATIONAL_RELEASE = "previousInternationalRelease";
	String PREVIOUS_EXTENSION_RELEASE = "previousExtensionRelease";
	String ASSERTION_GROUP_NAMES = "assertionGroupNames";
	String EXTENSION_DEPENDENCY_RELEASE = "extensionDependencyRelease";
	String DEPENDENCY_RELEASE_PACKAGE = "dependencyReleasePackage";
	String PREVIOUS_EDITION_DEPENDENCY_EFFECTIVE_DATE = "previousEditionDependencyEffectiveDate";
	String NAMESPACE_ID = "namespaceId";
	String MODULE_ID = "moduleId";
	String RELEASE_AS_AN_EDITION = "releaseExtensionAsAnEdition";
	String ENABLE_DROOLS = "enableDrools";
	String ENABLE_JIRA = "jiraIssueCreationFlag";
	String ENABLE_MRCM = "enableMRCMValidation";
	String JIRA_PRODUCT_NAME = "jiraProductName";
	String JIRA_REPORTING_STAGE = "jiraReportingStage";
	String INCLUDED_PREV_RELEASE_FILES = "includePrevReleaseFiles";
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

	Product find(String releaseCenterKey, String productKey, boolean includedLatestBuildStatusAndTags);

	Product create(String releaseCenterKey, String name) throws BusinessServiceException;

	Product update(String releaseCenterKey, String productKey, Map<String, String> newPropertyValues) throws BusinessServiceException;

    void updateVisibility(String releaseCenterKey, String productKey, boolean visibility);
}

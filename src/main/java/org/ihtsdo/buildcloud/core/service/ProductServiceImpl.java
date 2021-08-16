package org.ihtsdo.buildcloud.core.service;

import java.text.ParseException;
import java.util.*;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.ihtsdo.buildcloud.core.entity.*;
import org.ihtsdo.buildcloud.core.service.helper.FilterOption;
import org.ihtsdo.buildcloud.core.dao.ExtensionConfigDAO;
import org.ihtsdo.buildcloud.core.dao.ProductDAO;
import org.ihtsdo.buildcloud.core.dao.ReleaseCenterDAO;
import org.ihtsdo.buildcloud.core.entity.*;
import org.ihtsdo.buildcloud.core.entity.helper.EntityHelper;
import org.ihtsdo.otf.rest.client.RestClientException;
import org.ihtsdo.otf.rest.client.terminologyserver.pojo.Branch;
import org.ihtsdo.otf.rest.client.terminologyserver.pojo.CodeSystem;
import org.ihtsdo.otf.rest.exception.BadConfigurationException;
import org.ihtsdo.otf.rest.exception.BadRequestException;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.otf.rest.exception.EntityAlreadyExistsException;
import org.ihtsdo.otf.rest.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class ProductServiceImpl extends EntityServiceImpl<Product> implements ProductService {

	private static final Logger LOGGER = LoggerFactory.getLogger(ProductServiceImpl.class);

	@Autowired
	private ProductDAO productDAO;

	@Autowired
	private ExtensionConfigDAO extensionConfigDAO;

	@Autowired
	private ReleaseCenterDAO releaseCenterDAO;

	@Autowired
	private PublishService publishService;

	@Autowired
   private BuildService buildService;

	@Autowired
	private TermServerService termServerService;

	@Autowired
	private ReleaseCenterService releaseCenterService;

	@Value("${srs.build.offlineMode}")
	private Boolean offlineMode;

	@Autowired
	public ProductServiceImpl(final ProductDAO dao) {
		super(dao);
	}

	@Override
	public Page<Product> findAll(final String releaseCenterKey, final Set<FilterOption> filterOptions, Pageable pageable) {
		Page<Product> page = productDAO.findAll(releaseCenterKey, filterOptions, pageable);
		if (!CollectionUtils.isEmpty(page.getContent())) {
			page.getContent().forEach(product -> {
				setLatestBuildStatusAndTag(releaseCenterKey, product);
			});
		}
		return page;
	}

	@Override
	public Product find(final String releaseCenterKey, final String productKey, final boolean includedLatestBuildStatusAndTags) {
		Product product = productDAO.find(releaseCenterKey, productKey);
		if (includedLatestBuildStatusAndTags && product != null) {
			setLatestBuildStatusAndTag(releaseCenterKey, product);
		}
		return product;
	}

	@Override
	public Product create(final String releaseCenterKey, final String productName) throws BusinessServiceException {
		LOGGER.info("create product, releaseCenterBusinessKey: {}", releaseCenterKey);
		final ReleaseCenter releaseCenter = releaseCenterDAO.find(releaseCenterKey);

		if (releaseCenter == null) {
			throw new ResourceNotFoundException("Unable to find Release Center: " + releaseCenterKey);
		}

		if (StringUtils.isEmpty(releaseCenter.getCodeSystem())) {
			throw new BusinessServiceException(String.format("Could not create new product as no code system specified for release center %s. Contact Admin Global for support", releaseCenterKey));
		}

		// Check that we don't already have one of these
		final String productBusinessKey = EntityHelper.formatAsBusinessKey(productName);
		final Product existingProduct = productDAO.find(releaseCenterKey, productBusinessKey);
		if (existingProduct != null) {
			throw new EntityAlreadyExistsException("Product named '" + productName + "' already exists.");
		}

		final Product product = new Product(productName);
		product.setVisibility(true);
		releaseCenter.addProduct(product);
		productDAO.save(product);

		// auto-discover product configurations form code-system
		if (!offlineMode) {
			List<CodeSystem> codeSystems = termServerService.getCodeSystems();

			CodeSystem codeSystem = codeSystems.stream()
					.filter(c -> c.getShortName().equals(releaseCenter.getCodeSystem()))
					.findAny()
					.orElse(null);

			Map<String, String> propertyValues = new HashMap<>();
			propertyValues.put(EFFECTIVE_TIME, "1970-01-01");
			propertyValues.put(USE_CLASSIFIER_PRECONDITION_CHECKS, TRUE);
			propertyValues.put(USE_CLASSIFIER_PRECONDITION_CHECKS, TRUE);
			propertyValues.put(CLASSIFY_OUTPUT_FILES, TRUE);
			propertyValues.put(ENABLE_DROOLS, TRUE);
			propertyValues.put(ENABLE_MRCM, TRUE);
			propertyValues.put(RELEASE_INFORMATION_FIELDS, "effectiveTime,deltaFromDate,deltaToDate,includedModules,languageRefsets,licenceStatement");
			propertyValues.put(README_END_DATE, String.valueOf(Calendar.getInstance().get(Calendar.YEAR)));
			if (INTERNATIONAL.equals(releaseCenter.getBusinessKey())) {
				propertyValues.put(CREATE_LEGACY_IDS, TRUE);
			}
			if (codeSystem != null && !StringUtils.isEmpty(codeSystem.getBranchPath())) {
				try {
					Branch branch = termServerService.getBranch(codeSystem.getBranchPath());
					if (branch.getMetadata() != null) {
						Map<String, Object> metaData = branch.getMetadata();
						propertyValues.put(DEFAULT_BRANCH_PATH, codeSystem.getBranchPath());

						if (metaData.containsKey("previousPackage")) {
							//propertyValues.put(PREVIOUS_PUBLISHED_PACKAGE, metaData.get("previousPackage").toString());
						}
						else {
							propertyValues.put(FIRST_TIME_RELEASE, TRUE);
						}
						if (metaData.containsKey("defaultNamespace")) {
							propertyValues.put(NAMESPACE_ID, metaData.get("defaultNamespace").toString());
						}
						if (metaData.containsKey("defaultModuleId")) {
							propertyValues.put(MODULE_ID, metaData.get("defaultModuleId").toString());
						}
						if (metaData.containsKey("assertionGroupNames")) {
							String assertionGroupNames = metaData.get("assertionGroupNames").toString();
							propertyValues.put(ASSERTION_GROUP_NAMES, assertionGroupNames);
							propertyValues.put(DROOLS_RULES_GROUP_NAMES, assertionGroupNames);
						}
					}

					branch = termServerService.getBranch(PermissionServiceCache.BRANCH_ROOT);
					if (branch.getMetadata() != null) {
						Map<String, Object> metaData = branch.getMetadata();
						if (metaData.containsKey("previousPackage")) {
							String latestInternationalPackage = metaData.get("previousPackage").toString();
							propertyValues.put(PREVIOUS_INTERNATIONAL_RELEASE, latestInternationalPackage);
							if (!INTERNATIONAL.equals(releaseCenter.getBusinessKey())) {
								propertyValues.put(EXTENSION_DEPENDENCY_RELEASE, latestInternationalPackage);
								propertyValues.put(DEPENDENCY_RELEASE_PACKAGE, latestInternationalPackage);
							}
						}
					}
				} catch (RestClientException e) {
					LOGGER.error("Unable to find branch path {}", codeSystem.getBranchPath());
				}
			}
			this.update(releaseCenter.getBusinessKey(), product.getBusinessKey(), propertyValues);
		}

		return product;
	}

	@Override
	public Product update(final String releaseCenterKey, final String productKey, final Map<String, String> newPropertyValues) throws BusinessServiceException {
		LOGGER.info("update product, newPropertyValues: {}", newPropertyValues);
		final Product product = find(releaseCenterKey, productKey, true);
		if (product == null) {
			throw new ResourceNotFoundException("No product found for product key:" + productKey);
		}
		updateProductBuildConfiguration(newPropertyValues, product);
		updateProductQaTestConfig(newPropertyValues, product);
		productDAO.update(product);
		return product;
	}

	@Override
	public void updateVisibility(final String releaseCenterKey, final String productKey, final boolean visibility) {
		Product product = find(releaseCenterKey, productKey, false);
		if (product == null) {
			throw new ResourceNotFoundException("No product found for product key:" + productKey);
		}

		product.setVisibility(visibility);
		productDAO.update(product);

		// Mark all builds shown/hidden
		List<Build> builds = buildService.findAllDesc(releaseCenterKey, product.getBusinessKey(), null, null, null, null);
		product.setLatestBuildStatus(!CollectionUtils.isEmpty(builds) ? builds.get(0).getStatus() : Build.Status.UNKNOWN);
		for (Build build : builds) {
			buildService.updateVisibility(build, visibility);
		}
	}

	private void updateProductQaTestConfig(final Map<String, String> newPropertyValues, final Product product) {
		QATestConfig qaTestConfig = product.getQaTestConfig();
		if (qaTestConfig == null) {
			qaTestConfig = new QATestConfig();
			qaTestConfig.setProduct(product);
			product.setQaTestConfig(qaTestConfig);
		}
		if (newPropertyValues.containsKey(PREVIOUS_INTERNATIONAL_RELEASE)) {
			qaTestConfig.setPreviousInternationalRelease(newPropertyValues.get(PREVIOUS_INTERNATIONAL_RELEASE));
		}
		if (newPropertyValues.containsKey(PREVIOUS_EXTENSION_RELEASE)) {
			qaTestConfig.setPreviousExtensionRelease(newPropertyValues.get(PREVIOUS_EXTENSION_RELEASE));
		}
		if (newPropertyValues.containsKey(ASSERTION_GROUP_NAMES)) {
			qaTestConfig.setAssertionGroupNames(newPropertyValues.get(ASSERTION_GROUP_NAMES));
		}
		if (newPropertyValues.containsKey(EXTENSION_DEPENDENCY_RELEASE)) {
			qaTestConfig.setExtensionDependencyRelease(newPropertyValues.get(EXTENSION_DEPENDENCY_RELEASE));
		}
		if (newPropertyValues.containsKey(ENABLE_DROOLS)) {
			qaTestConfig.setEnableDrools(TRUE.equals(newPropertyValues.get(ENABLE_DROOLS)));
		}
		if (newPropertyValues.containsKey(ENABLE_MRCM)) {
			qaTestConfig.setEnableMRCMValidation(TRUE.equals(newPropertyValues.get(ENABLE_MRCM)));
		}
		if (newPropertyValues.containsKey(ENABLE_JIRA)) {
			qaTestConfig.setJiraIssueCreationFlag(TRUE.equals(newPropertyValues.get(ENABLE_JIRA)));
		}
		if (newPropertyValues.containsKey(JIRA_PRODUCT_NAME)) {
			qaTestConfig.setProductName(newPropertyValues.get(JIRA_PRODUCT_NAME));
		}
		if (newPropertyValues.containsKey(JIRA_REPORTING_STAGE)) {
			qaTestConfig.setReportingStage(newPropertyValues.get(JIRA_REPORTING_STAGE));
		}
		if (newPropertyValues.containsKey(DROOLS_RULES_GROUP_NAMES)) {
			qaTestConfig.setDroolsRulesGroupNames(newPropertyValues.get(DROOLS_RULES_GROUP_NAMES));
		}
		if (newPropertyValues.containsKey(ENABLE_MRCM)) {
			qaTestConfig.setEnableMRCMValidation(TRUE.equals(newPropertyValues.get(ENABLE_MRCM)));
		}
	}

	private void updateProductBuildConfiguration(final Map<String, String> newPropertyValues, final Product product)
			throws ResourceNotFoundException, BusinessServiceException {
		BuildConfiguration configuration = product.getBuildConfiguration();
		if (configuration == null) {
			configuration = new BuildConfiguration();
			configuration.setProduct(product);
			product.setBuildConfiguration(configuration);
		}
		if (newPropertyValues.containsKey(EFFECTIVE_TIME)) {
			try {
				final Date date = DateFormatUtils.ISO_DATE_FORMAT.parse(newPropertyValues.get(EFFECTIVE_TIME));
				configuration.setEffectiveTime(date);
			} catch (final ParseException e) {
				throw new BadRequestException("Invalid " + EFFECTIVE_TIME + " format. Expecting format " + DateFormatUtils.ISO_DATE_FORMAT.getPattern() + ".", e);
			}
		}
		if (newPropertyValues.containsKey(JUST_PACKAGE)) {
			configuration.setJustPackage(TRUE.equals(newPropertyValues.get(JUST_PACKAGE)));
		}
		if (newPropertyValues.containsKey(FIRST_TIME_RELEASE)) {
			configuration.setFirstTimeRelease(TRUE.equals(newPropertyValues.get(FIRST_TIME_RELEASE)));
		}
		if (newPropertyValues.containsKey(BETA_RELEASE)) {
			configuration.setBetaRelease(TRUE.equals(newPropertyValues.get(BETA_RELEASE)));
		}
		if (newPropertyValues.containsKey(WORKBENCH_DATA_FIXES_REQUIRED)) {
			configuration.setWorkbenchDataFixesRequired(TRUE.equals(newPropertyValues.get(WORKBENCH_DATA_FIXES_REQUIRED)));
		}
		if (newPropertyValues.containsKey(INPUT_FILES_FIXES_REQUIRED)) {
			configuration.setInputFilesFixesRequired(TRUE.equals(newPropertyValues.get(INPUT_FILES_FIXES_REQUIRED)));
		}
		if (newPropertyValues.containsKey(CREATE_LEGACY_IDS)) {
			configuration.setCreateLegacyIds(TRUE.equals(newPropertyValues.get(CREATE_LEGACY_IDS)));
		}
		if (newPropertyValues.containsKey(CLASSIFY_OUTPUT_FILES)) {
			configuration.setClassifyOutputFiles(TRUE.equals(newPropertyValues.get(CLASSIFY_OUTPUT_FILES)));
		}
		if (newPropertyValues.containsKey(LICENSE_STATEMENT)) {
			configuration.setLicenceStatement(newPropertyValues.get(LICENSE_STATEMENT));
		}
		if (newPropertyValues.containsKey(CONCEPT_PREFERRED_TERMS)) {
			configuration.setConceptPreferredTerms(newPropertyValues.get(CONCEPT_PREFERRED_TERMS));
		}
		if (newPropertyValues.containsKey(RELEASE_INFORMATION_FIELDS)) {
			configuration.setReleaseInformationFields(newPropertyValues.get(RELEASE_INFORMATION_FIELDS));
		}
		if (newPropertyValues.containsKey(USE_CLASSIFIER_PRECONDITION_CHECKS)) {
			configuration.setUseClassifierPreConditionChecks(TRUE.equals(newPropertyValues.get(USE_CLASSIFIER_PRECONDITION_CHECKS)));
		}
		if (newPropertyValues.containsKey(DEFAULT_BRANCH_PATH) && configuration.getDefaultBranchPath() != newPropertyValues.get(DEFAULT_BRANCH_PATH).toUpperCase()) {
			String newDefaultBranchPath = newPropertyValues.get(DEFAULT_BRANCH_PATH);
			if (!StringUtils.isEmpty(newDefaultBranchPath)) {
				List<CodeSystem> codeSystems = termServerService.getCodeSystems();
				CodeSystem codeSystem = codeSystems.stream()
						.filter(c -> product.getReleaseCenter().getCodeSystem().equals(c.getShortName()))
						.findAny()
						.orElse(null);
				if (!newDefaultBranchPath.startsWith(codeSystem.getBranchPath())) {
					throw new BusinessServiceException(String.format("The new default branch must be resided within the same code system branch %s", codeSystem.getBranchPath()));
				}
			}
			configuration.setDefaultBranchPath(newDefaultBranchPath.toUpperCase());
		}
		if (newPropertyValues.containsKey(PREVIOUS_PUBLISHED_PACKAGE)) {
			final ReleaseCenter releaseCenter = product.getReleaseCenter();
			final String pPP = newPropertyValues.get(PREVIOUS_PUBLISHED_PACKAGE);
			if (StringUtils.isEmpty(pPP)) {
				configuration.setPreviousPublishedPackage(null);
			} else {
				//Validate that a file of that name actually exists
				boolean pppExists = false;
				Exception rootCause = new Exception("No further information");
				try {
					pppExists = publishService.exists(releaseCenter, pPP);
				} catch (final Exception e) {
					rootCause = e;
				}

				if (pppExists) {
					configuration.setPreviousPublishedPackage(pPP);
				} else {
					throw new ResourceNotFoundException("Could not find previously published package: " + pPP, rootCause);
				}
			}
		}

		if (newPropertyValues.containsKey(CUSTOM_REFSET_COMPOSITE_KEYS)) {
			final Map<String, List<Integer>> refsetCompositeKeyMap = new HashMap<>();
			try {
				final String refsetCompositeKeyIndexes = newPropertyValues.get(CUSTOM_REFSET_COMPOSITE_KEYS);
				if (!StringUtils.isEmpty(refsetCompositeKeyIndexes)) {
					final String[] split = refsetCompositeKeyIndexes.split("\\|");
					for (String refsetKeyAndIndexes : split) {
						refsetKeyAndIndexes = refsetKeyAndIndexes.trim();
						if (!refsetKeyAndIndexes.isEmpty()) {
							final String[] keyAndIndexes = refsetKeyAndIndexes.split("=", 2);
							final String refsetKey = keyAndIndexes[0].trim();
							final List<Integer> indexes = new ArrayList<>();
							final String value = keyAndIndexes[1];
							final String[] indexStrings = value.split(",");
							for (final String indexString : indexStrings) {
								final String trim = indexString.trim();
								indexes.add(Integer.parseInt(trim));
							}
							refsetCompositeKeyMap.put(refsetKey, indexes);
						}
					}
				}
			} catch (final NumberFormatException e) {
				throw new BadConfigurationException("Failed to parse " + CUSTOM_REFSET_COMPOSITE_KEYS);
			}
			configuration.setCustomRefsetCompositeKeys(refsetCompositeKeyMap);
		}

		if (newPropertyValues.containsKey(README_HEADER)) {
			final String readmeHeader = newPropertyValues.get(README_HEADER);
			configuration.setReadmeHeader(readmeHeader);
		}

		if (newPropertyValues.containsKey(README_END_DATE)) {
			final String readmeEndDate = newPropertyValues.get(README_END_DATE);
			configuration.setReadmeEndDate(readmeEndDate);
		}

		if (newPropertyValues.containsKey(NEW_RF2_INPUT_FILES)) {
			configuration.setNewRF2InputFiles(newPropertyValues.get(NEW_RF2_INPUT_FILES));
		}

		if (newPropertyValues.containsKey(INCLUDED_PREV_RELEASE_FILES)) {
			configuration.setIncludePrevReleaseFiles(newPropertyValues.get(INCLUDED_PREV_RELEASE_FILES));
		}

		setExtensionConfig(newPropertyValues, configuration);
	}

	private void setExtensionConfig(Map<String, String> newPropertyValues, BuildConfiguration configuration) {
		if (newPropertyValues.containsKey(DEPENDENCY_RELEASE_PACKAGE)
			|| newPropertyValues.containsKey(MODULE_ID)
			|| newPropertyValues.containsKey(NAMESPACE_ID)
			|| newPropertyValues.containsKey(RELEASE_AS_AN_EDITION)) {
			String dependencyPackageRelease = newPropertyValues.get(DEPENDENCY_RELEASE_PACKAGE);
			String moduleID = newPropertyValues.get(MODULE_ID);
			String namespaceID = newPropertyValues.get(NAMESPACE_ID);
			String releaseAsEdition = newPropertyValues.get(RELEASE_AS_AN_EDITION);

			if (StringUtils.isEmpty(dependencyPackageRelease)
				&& StringUtils.isEmpty(moduleID)
				&& StringUtils.isEmpty(namespaceID)
				&& (StringUtils.isEmpty(releaseAsEdition) || !TRUE.equals(releaseAsEdition))) {
				if (configuration.getExtensionConfig() != null) {
					ExtensionConfig extensionConfig = configuration.getExtensionConfig();
					extensionConfig.setBuildConfiguration(configuration);
					extensionConfigDAO.delete(extensionConfig);
					configuration.setExtensionConfig(null);
				}
			} else {
				if (configuration.getExtensionConfig() == null) {
					ExtensionConfig extConfig = new ExtensionConfig();
					configuration.setExtensionConfig(extConfig);
					extConfig.setBuildConfiguration(configuration);
				}

				if (newPropertyValues.containsKey(DEPENDENCY_RELEASE_PACKAGE)) {
					if (StringUtils.isEmpty(dependencyPackageRelease)) {
						configuration.getExtensionConfig().setDependencyRelease(null);
					} else {
						final ReleaseCenter releaseCenter = new ReleaseCenter();
						releaseCenter.setShortName("International");
						//Validate that a file of that name actually exists
						boolean pppExists = false;
						Exception rootCause = new Exception("No further information");
						try {
							pppExists = publishService.exists(releaseCenter, dependencyPackageRelease);
						} catch (final Exception e) {
							rootCause = e;
						}
						if (pppExists) {

							configuration.getExtensionConfig().setDependencyRelease(dependencyPackageRelease);
						} else {
							throw new ResourceNotFoundException("Could not find dependency release package: " + dependencyPackageRelease, rootCause);
						}
					}
				}

				if (newPropertyValues.containsKey(MODULE_ID)) {
					if (StringUtils.isEmpty(moduleID)) {
						configuration.getExtensionConfig().setModuleId(null);
					} else {
						configuration.getExtensionConfig().setModuleId(moduleID);
					}
				}

				if (newPropertyValues.containsKey(NAMESPACE_ID)) {
					if (StringUtils.isEmpty(namespaceID)) {
						configuration.getExtensionConfig().setNamespaceId(null);
					} else {
						configuration.getExtensionConfig().setNamespaceId(namespaceID);
					}
				}

				if (newPropertyValues.containsKey(RELEASE_AS_AN_EDITION)) {
					configuration.getExtensionConfig().setReleaseAsAnEdition(TRUE.equals(releaseAsEdition));
				}
			}
		}
	}

	private void setLatestBuildStatusAndTag(String releaseCenterKey, Product product) {
		List<Build> builds = buildService.findAllDesc(releaseCenterKey, product.getBusinessKey(), null, null, null, null);
		product.setLatestBuildStatus(!CollectionUtils.isEmpty(builds) ? builds.get(0).getStatus() : Build.Status.UNKNOWN);
		for (Build build : builds) {
			if (!CollectionUtils.isEmpty(build.getTags())) {
				product.setLatestTag(build.getTags().get(build.getTags().size() - 1));
				break;
			}
		}
	}
}

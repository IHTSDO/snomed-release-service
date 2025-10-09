package org.ihtsdo.buildcloud.core.service;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.ihtsdo.buildcloud.core.dao.ExtensionConfigDAO;
import org.ihtsdo.buildcloud.core.dao.ProductDAO;
import org.ihtsdo.buildcloud.core.dao.ReleaseCenterDAO;
import org.ihtsdo.buildcloud.core.entity.*;
import org.ihtsdo.buildcloud.core.entity.helper.EntityHelper;
import org.ihtsdo.buildcloud.core.service.build.RF2Constants;
import org.ihtsdo.buildcloud.core.service.helper.FilterOption;
import org.ihtsdo.buildcloud.rest.controller.helper.PageRequestHelper;
import org.ihtsdo.otf.rest.client.RestClientException;
import org.ihtsdo.otf.rest.client.terminologyserver.pojo.Branch;
import org.ihtsdo.otf.rest.client.terminologyserver.pojo.CodeSystem;
import org.ihtsdo.otf.rest.client.terminologyserver.pojo.CodeSystemVersion;
import org.ihtsdo.otf.rest.exception.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Field;
import java.text.ParseException;
import java.util.*;

import static org.apache.commons.lang.time.DateFormatUtils.ISO_DATE_FORMAT;

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
	private ModuleStorageCoordinatorCache moduleStorageCoordinatorCache;

	@Value("${srs.build.offlineMode}")
	private Boolean offlineMode;

	@Value("${srs.empty-release-file}")
	private String emptyRf2Filename;

	@Autowired
	public ProductServiceImpl(final ProductDAO dao) {
		super(dao);
	}

	@Override
	public Page<Product> findAll(final String releaseCenterKey, final Set<FilterOption> filterOptions, Pageable pageable, boolean includedLatestBuildStatusAndTags) {
		Page<Product> page = productDAO.findAll(releaseCenterKey, filterOptions, pageable);
		if (includedLatestBuildStatusAndTags && !CollectionUtils.isEmpty(page.getContent())) {
			page.getContent().forEach(product -> setLatestBuildStatusAndTag(releaseCenterKey, product));
		}
		return page;
	}

	@Override
	public Product getDailyBuildProductForCodeSystem(String codeSystemShortName) {
		List<ReleaseCenter> centers = releaseCenterDAO.findAll();
		ReleaseCenter releaseCenter = centers.stream().filter(center -> center.getCodeSystem() != null && center.getCodeSystem().equals(codeSystemShortName)).findFirst().orElse(null);
		if (releaseCenter == null) {
			LOGGER.error("No release center is associated with the code system {}", codeSystemShortName);
			return null;
		}

		Set<FilterOption> filterOptions = EnumSet.noneOf(FilterOption.class);
		PageRequest pageRequest = PageRequestHelper.createPageRequest(0, 100, null, null);
		Page<Product> page = productDAO.findAll(releaseCenter.getBusinessKey(), filterOptions, pageRequest);
		if (page.getTotalElements() == 0) return null;

		for (Product product : page.getContent()) {
			if (product.getBuildConfiguration().isDailyBuild()) {
				return product;
			}
		}
		return null;
	}

	@Override
	public Page<Product> findHiddenProducts(String releaseCenterKey, Pageable pageable) {
		return productDAO.findHiddenProducts(releaseCenterKey, pageable);
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
	public Product create(final String releaseCenterKey, final String productName, final String overriddenSnomedCtProduct) throws BusinessServiceException {
		LOGGER.info("create product, releaseCenterBusinessKey: {}", releaseCenterKey);
		final ReleaseCenter releaseCenter = releaseCenterDAO.find(releaseCenterKey);

		if (releaseCenter == null) {
			throw new ResourceNotFoundException("Unable to find Release Center: " + releaseCenterKey);
		}

		if (!StringUtils.hasLength(releaseCenter.getCodeSystem())) {
			throw new BusinessServiceException(String.format("Could not create new product as no code system specified for release center %s. Contact Admin Global for support", releaseCenterKey));
		}

		// Check that we don't already have one of these
		final String productBusinessKey = EntityHelper.formatAsBusinessKey(productName);
		final Product existingProduct = productDAO.find(productBusinessKey);
		if (existingProduct != null) {
			throw new EntityAlreadyExistsException("Product named '" + productName + "' already exists"
					+ (releaseCenterKey.equalsIgnoreCase(existingProduct.getReleaseCenter().getBusinessKey()) ? "." : " in the '" + existingProduct.getReleaseCenter().getName() +"'.")
					+ " Please select a different product name.");
		}

		final Product product = new Product(productName);
		product.setVisibility(true);
		if (StringUtils.hasLength(overriddenSnomedCtProduct)) {
			product.setOverriddenSnomedCtProduct(overriddenSnomedCtProduct);
		}
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
			propertyValues.put(CLASSIFY_OUTPUT_FILES, TRUE);
			propertyValues.put(ENABLE_DROOLS, TRUE);
			propertyValues.put(ENABLE_MRCM, TRUE);
			propertyValues.put(RELEASE_INFORMATION_FIELDS, "effectiveTime,deltaFromDate,deltaToDate,includedModules,languageRefsets,licenceStatement");
			propertyValues.put(README_END_DATE, String.valueOf(Calendar.getInstance().get(Calendar.YEAR)));
			if (INTERNATIONAL.equals(releaseCenter.getBusinessKey())) {
				propertyValues.put(CREATE_LEGACY_IDS, TRUE);
			}
			if (codeSystem != null && StringUtils.hasLength(codeSystem.getBranchPath())) {
				try {
					addValuesFromCodeSystem(codeSystem, propertyValues);
					addValuesFromBranchRoot(releaseCenter, propertyValues);
				} catch (Exception e) {
					String errorMsg = String.format("Failed to detect the branch metadata from code system branch %s. Error: %s", codeSystem.getBranchPath(), e.getMessage());
					LOGGER.error(errorMsg);
					throw new BusinessServiceException(errorMsg);
				}
			}
			this.update(releaseCenter.getBusinessKey(), product.getBusinessKey(), propertyValues);
		}

		return product;
	}

	private void addValuesFromBranchRoot(ReleaseCenter releaseCenter, Map<String, String> propertyValues) throws RestClientException {
		Branch branch = termServerService.getBranch(PermissionServiceCache.BRANCH_ROOT);
		if (branch.getMetadata() != null) {
			Map<String, Object> metaData = branch.getMetadata();
			if (metaData.containsKey(PREVIOUS_PACKAGE)) {
				String latestInternationalPackage = metaData.get(PREVIOUS_PACKAGE).toString();
				if (!INTERNATIONAL.equals(releaseCenter.getBusinessKey())) {
					propertyValues.put(EXTENSION_DEPENDENCY_RELEASE, latestInternationalPackage);
				}
			}
		}
	}

	private void addValuesFromCodeSystem(CodeSystem codeSystem, Map<String, String> propertyValues) throws RestClientException {
		Branch branch = termServerService.getBranch(codeSystem.getBranchPath());
		if (branch.getMetadata() != null) {
			Map<String, Object> metaData = branch.getMetadata();
			propertyValues.put(DEFAULT_BRANCH_PATH, codeSystem.getBranchPath());

			Set<String> modules = termServerService.getModulesForBranch(codeSystem.getBranchPath());
			if (modules != null) {
				propertyValues.put(MODULE_IDS, String.join(",", modules));
			}

			if (metaData.containsKey(PREVIOUS_PACKAGE) && !emptyRf2Filename.equals(metaData.get(PREVIOUS_PACKAGE).toString())) {
				propertyValues.put(PREVIOUS_PUBLISHED_PACKAGE, metaData.get(PREVIOUS_PACKAGE).toString());
			}
			else {
				propertyValues.put(FIRST_TIME_RELEASE, TRUE);
			}
			if (metaData.containsKey(DEFAULT_NAMESPACE)) {
				propertyValues.put(NAMESPACE_ID, metaData.get(DEFAULT_NAMESPACE).toString());
			}
			if (metaData.containsKey(DEFAULT_MODULE_ID)) {
				propertyValues.put(DEFAULT_MODULE_ID, metaData.get(DEFAULT_MODULE_ID).toString());
			}
			if (metaData.containsKey(ASSERTION_GROUP_NAMES)) {
				String assertionGroupNames = metaData.get(ASSERTION_GROUP_NAMES).toString();
				propertyValues.put(DROOLS_RULES_GROUP_NAMES, assertionGroupNames);
			}
			if (metaData.containsKey(RELEASE_ASSERTION_GROUP_NAMES)) {
				String releaseAssertionGroupNames = metaData.get(RELEASE_ASSERTION_GROUP_NAMES).toString();
				propertyValues.put(ASSERTION_GROUP_NAMES, releaseAssertionGroupNames);
			}
		}
	}

	@Override
	public Product update(final String releaseCenterKey, final String productKey, final Map<String, String> newPropertyValues) throws BusinessServiceException {
		LOGGER.info("update product, newPropertyValues: {}", newPropertyValues);
		final Product product = find(releaseCenterKey, productKey, true);
		if (product == null) {
			throw new ResourceNotFoundException("No product found for product key:" + productKey);
		}
		try {
			updateProductBuildConfiguration(newPropertyValues, product);
			updateProductQaTestConfig(newPropertyValues, product);

			if (newPropertyValues.containsKey(STAND_ALONE_PRODUCT)) {
				product.setStandAloneProduct(TRUE.equals(newPropertyValues.get(STAND_ALONE_PRODUCT)));
			}

			if (newPropertyValues.containsKey(OVERRIDDEN_SNOMEDCT_PRODUCT)) {
				product.setOverriddenSnomedCtProduct(newPropertyValues.get(OVERRIDDEN_SNOMEDCT_PRODUCT));
			}

			productDAO.update(product);
		} catch (Exception e) {
			String errorMsg = String.format("Failed to update product %s. Error: %s", product.getName(), e.getMessage());
			LOGGER.error(errorMsg);
			throw new BusinessServiceException(errorMsg);
		}
		return product;
	}

	@Override
	public void updateVisibility(final String releaseCenterKey, final String productKey, final boolean visibility) throws IOException {
		Product product = find(releaseCenterKey, productKey, false);
		if (product == null) {
			throw new ResourceNotFoundException("No product found for product key:" + productKey);
		}

		product.setVisibility(visibility);
		product.setLegacyProduct(false);
		productDAO.update(product);

		// Mark all builds shown/hidden
		List<Build> builds = buildService.findAllDesc(releaseCenterKey, product.getBusinessKey(), null, null, null, null);
		product.setLatestBuildStatus(!CollectionUtils.isEmpty(builds) ? builds.get(0).getStatus() : Build.Status.UNKNOWN);
		for (Build build : builds) {
			buildService.updateVisibility(build, visibility);
		}
	}

	@Override
	public void upgradeDependantVersion(String releaseCenterKey, String productKey)throws BusinessServiceException {
		Product product = find(releaseCenterKey, productKey, false);
		if (product == null) {
			throw new BusinessServiceException("The product with key " + productKey + " not found");
		} else if (!product.getBuildConfiguration().isDailyBuild()) {
			throw new BusinessServiceException("The product with key " + productKey + " is not a daily product");
		}

		List<CodeSystem> codeSystems = termServerService.getCodeSystems();
		CodeSystem codeSystem = codeSystems.stream().filter(cs -> cs.getShortName().equalsIgnoreCase(product.getReleaseCenter().getCodeSystem())).findAny().orElse(null);
		if (codeSystem == null) {
			throw new BusinessServiceException("Code System with name " + product.getReleaseCenter().getCodeSystem() + " not found");
		}

		List<CodeSystemVersion> intCodeSystemVersions = termServerService.getCodeSystemVersions(RF2Constants.SNOMEDCT, false, false);
		CodeSystemVersion newDependantVersion = intCodeSystemVersions.stream().filter(cv -> cv.getEffectiveDate().compareTo(codeSystem.getDependantVersionEffectiveTime()) == 0).findAny().orElse(null);
		if (newDependantVersion == null) {
			throw new ResourceNotFoundException("Could not find any dependant version with effectiveTime " + codeSystem.getDependantVersionEffectiveTime());
		} else if (!StringUtils.hasLength(newDependantVersion.getReleasePackage())) {
			throw new ResourceNotFoundException("Could not find release package from International versions with effectiveTime " + codeSystem.getDependantVersionEffectiveTime());
		}

		String newDependantReleasePackage = newDependantVersion.getReleasePackage();
		if (product.getBuildConfiguration().getExtensionConfig() != null) {
			product.getBuildConfiguration().getExtensionConfig().setDependencyRelease(newDependantReleasePackage);
		}

		update(product);
	}

	private void updateProductQaTestConfig(final Map<String, String> newPropertyValues, final Product product) {
		QATestConfig qaTestConfig = product.getQaTestConfig();
		if (qaTestConfig == null) {
			qaTestConfig = new QATestConfig();
			qaTestConfig.setProduct(product);
			product.setQaTestConfig(qaTestConfig);
		}
		if (newPropertyValues.containsKey(ASSERTION_GROUP_NAMES)) {
			qaTestConfig.setAssertionGroupNames(newPropertyValues.get(ASSERTION_GROUP_NAMES));
		}
		if (newPropertyValues.containsKey(ENABLE_DROOLS)) {
			qaTestConfig.setEnableDrools(TRUE.equals(newPropertyValues.get(ENABLE_DROOLS)));
		}
		if (newPropertyValues.containsKey(ENABLE_MRCM)) {
			qaTestConfig.setEnableMRCMValidation(TRUE.equals(newPropertyValues.get(ENABLE_MRCM)));
		}
		if (newPropertyValues.containsKey(DROOLS_RULES_GROUP_NAMES)) {
			qaTestConfig.setDroolsRulesGroupNames(newPropertyValues.get(DROOLS_RULES_GROUP_NAMES));
		}
	}

	private void updateProductBuildConfiguration(final Map<String, String> newPropertyValues, final Product product)
			throws ResourceNotFoundException, BusinessServiceException, NoSuchFieldException {
		BuildConfiguration configuration = product.getBuildConfiguration();
		if (configuration == null) {
			configuration = new BuildConfiguration();
			configuration.setProduct(product);
			product.setBuildConfiguration(configuration);
		}
		updateEffectiveTime(newPropertyValues, configuration);
		setConfigurationValueIfPresent(newPropertyValues, JUST_PACKAGE, configuration, JUST_PACKAGE, true);
		setConfigurationValueIfPresent(newPropertyValues, FIRST_TIME_RELEASE, configuration, FIRST_TIME_RELEASE, true);
		setConfigurationValueIfPresent(newPropertyValues, BETA_RELEASE, configuration, BETA_RELEASE, true);
		setConfigurationValueIfPresent(newPropertyValues, DAILY_BUILD, configuration, DAILY_BUILD, true);
		setConfigurationValueIfPresent(newPropertyValues, WORKBENCH_DATA_FIXES_REQUIRED, configuration, WORKBENCH_DATA_FIXES_REQUIRED, true);
		setConfigurationValueIfPresent(newPropertyValues, INPUT_FILES_FIXES_REQUIRED, configuration, INPUT_FILES_FIXES_REQUIRED, true);
		setConfigurationValueIfPresent(newPropertyValues, CREATE_LEGACY_IDS, configuration, CREATE_LEGACY_IDS, true);
		setConfigurationValueIfPresent(newPropertyValues, CLASSIFY_OUTPUT_FILES, configuration, CLASSIFY_OUTPUT_FILES, true);
		setConfigurationValueIfPresent(newPropertyValues, LICENSE_STATEMENT, configuration, LICENCE_STATEMENT, false);
		setConfigurationValueIfPresent(newPropertyValues, CONCEPT_PREFERRED_TERMS, configuration, CONCEPT_PREFERRED_TERMS, false);
		setConfigurationValueIfPresent(newPropertyValues, RELEASE_INFORMATION_FIELDS, configuration, RELEASE_INFORMATION_FIELDS, false);
		setAdditionalReleaseInformationFields(newPropertyValues, configuration);
		setConfigurationValueIfPresent(newPropertyValues, USE_CLASSIFIER_PRECONDITION_CHECKS, configuration, USE_CLASSIFIER_PRECONDITION_CHECKS, true);
		setDefaultBranch(newPropertyValues, product, configuration);
		setPreviousPublishedPackage(newPropertyValues, product, configuration);
		setConfigurationValueIfPresent(newPropertyValues, README_HEADER, configuration, README_HEADER, false);
		setConfigurationValueIfPresent(newPropertyValues, README_END_DATE, configuration, README_END_DATE, false);
		setConfigurationValueIfPresent(newPropertyValues, INCLUDED_PREV_RELEASE_FILES, configuration, INCLUDED_PREV_RELEASE_FILES, false);
		setConfigurationValueIfPresent(newPropertyValues, EXCLUDE_REFSET_DESCRIPTOR_MEMBERS, configuration, EXCLUDE_REFSET_DESCRIPTOR_MEMBERS, false);
		setConfigurationValueIfPresent(newPropertyValues, EXCLUDE_LANGUAGE_REFSET_IDS, configuration, EXCLUDE_LANGUAGE_REFSET_IDS, false);
		if (newPropertyValues.containsKey(CUSTOM_REFSET_COMPOSITE_KEYS)) {
			setCustomRefsetCompositeKeys(newPropertyValues, configuration);
		}
		validateDelaFilesThenSet(newPropertyValues, NEW_RF2_INPUT_FILES, "New RF2 Input Files only allow the Delta filenames", configuration);
		validateDelaFilesThenSet(newPropertyValues, REMOVE_RF2_FILES, "Remove RF2 Files only allow the Delta filenames", configuration);

		setExtensionConfig(newPropertyValues, configuration);
	}

	private void validateDelaFilesThenSet(Map<String, String> newPropertyValues, String propertyName, String message, BuildConfiguration configuration) throws BusinessServiceException, NoSuchFieldException {
		if (newPropertyValues.containsKey(propertyName)) {
			String value = newPropertyValues.get(propertyName);
			if (StringUtils.hasLength(value)) {
				String[] parts = Arrays.stream(value.split("\\|")).map(String::trim).toArray(String[]::new);
				boolean allContainDelta = Arrays.stream(parts).allMatch(s -> s.contains("Delta_") || s.contains("Delta-"));
				if (!allContainDelta) {
					throw new BusinessServiceException(message);
				}
				setConfigurationValueIfPresent(newPropertyValues, propertyName, configuration, propertyName, false);
			} else {
				setConfigurationValueIfPresent(newPropertyValues, propertyName, configuration, propertyName, false);
			}
		}
	}

	private void setAdditionalReleaseInformationFields(Map<String, String> newPropertyValues, BuildConfiguration configuration) throws BusinessServiceException {
		if (newPropertyValues.containsKey(ADDITIONAL_RELEASE_INFORMATION_FIELDS)) {
			String additionalFields = newPropertyValues.get(ADDITIONAL_RELEASE_INFORMATION_FIELDS);
			if (StringUtils.hasLength(additionalFields) && !isJSONValid((additionalFields))) {
				throw new BusinessServiceException("The additional release information is not valid JSON String");
			}
			configuration.setAdditionalReleaseInformationFields(newPropertyValues.get(ADDITIONAL_RELEASE_INFORMATION_FIELDS));
		}
	}

	private void setDefaultBranch(Map<String, String> newPropertyValues, Product product, BuildConfiguration configuration) throws BusinessServiceException {
		if (newPropertyValues.containsKey(DEFAULT_BRANCH_PATH)) {
			String newDefaultBranchPath = newPropertyValues.get(DEFAULT_BRANCH_PATH);
			if (!StringUtils.hasLength(newDefaultBranchPath)) {
				configuration.setDefaultBranchPath(null);
			} else {
				newDefaultBranchPath = newDefaultBranchPath.toUpperCase();
				if (!Objects.equals(configuration.getDefaultBranchPath(), newDefaultBranchPath)) {
					List<CodeSystem> codeSystems = termServerService.getCodeSystems();
					CodeSystem codeSystem = codeSystems.stream()
							.filter(c -> product.getReleaseCenter().getCodeSystem().equals(c.getShortName()))
							.findAny()
							.orElse(null);
					assert codeSystem != null;
					if (!newDefaultBranchPath.startsWith(codeSystem.getBranchPath())) {
						throw new BusinessServiceException(String.format("The new default branch must be resided within the same code system branch %s", codeSystem.getBranchPath()));
					}
					configuration.setDefaultBranchPath(newDefaultBranchPath);
				}
			}
		}
	}

	private void updateEffectiveTime(Map<String, String> newPropertyValues, BuildConfiguration configuration) throws BadRequestException {
		if (newPropertyValues.containsKey(EFFECTIVE_TIME)) {
			try {
				final Date date = DateFormatUtils.ISO_8601_EXTENDED_DATE_FORMAT.parse(newPropertyValues.get(EFFECTIVE_TIME));
				configuration.setEffectiveTime(date);
			} catch (final ParseException e) {
				throw new BadRequestException("Invalid " + EFFECTIVE_TIME + " format. Expecting format " + DateFormatUtils.ISO_8601_EXTENDED_DATE_FORMAT.getPattern() + ".", e);
			}
		}
	}

	private void setCustomRefsetCompositeKeys(Map<String, String> newPropertyValues, BuildConfiguration configuration) throws BadConfigurationException {
		final Map<String, List<Integer>> refsetCompositeKeyMap = new HashMap<>();
		try {
			final String refsetCompositeKeyIndexes = newPropertyValues.get(CUSTOM_REFSET_COMPOSITE_KEYS);
			if (StringUtils.hasLength(refsetCompositeKeyIndexes)) {
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

	private void setPreviousPublishedPackage(Map<String, String> newPropertyValues, Product product, BuildConfiguration configuration) {
		if (newPropertyValues.containsKey(PREVIOUS_PUBLISHED_PACKAGE)) {
			final ReleaseCenter releaseCenter = product.getReleaseCenter();
			final String pPP = newPropertyValues.get(PREVIOUS_PUBLISHED_PACKAGE);
			if (!StringUtils.hasLength(pPP)) {
				configuration.setPreviousPublishedPackage(null);
			} else {
				//Validate that a file of that name actually exists
				boolean pppExists = false;
				Exception rootCause = new Exception("No further information");
				try {
					pppExists = publishService.isReleaseFileExistInMSC(pPP);
					if (!pppExists) {
						pppExists = publishService.exists(releaseCenter, pPP);
					}
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
	}

	private void setConfigurationValueIfPresent(final Map<String, String> newPropertyValues, String propertyName, Object configuration, String fieldName, boolean isBooleanField) throws NoSuchFieldException {
		if (newPropertyValues.containsKey(propertyName)) {
			Field field = configuration.getClass().getDeclaredField(fieldName);
			ReflectionUtils.makeAccessible(field);
			if (isBooleanField) {
				boolean fieldValue = TRUE.equals(newPropertyValues.get(propertyName));
				ReflectionUtils.setField(field, configuration, fieldValue);
			} else {
				String fieldValue = newPropertyValues.get(propertyName);
				ReflectionUtils.setField(field, configuration, StringUtils.hasLength(fieldValue) ? fieldValue : null);
			}
		}
	}

	private void setExtensionConfig(Map<String, String> newPropertyValues, BuildConfiguration configuration) throws BadRequestException, NoSuchFieldException {
		boolean isExtensionConfigFieldPresent = newPropertyValues.containsKey(EXTENSION_DEPENDENCY_RELEASE)
				|| newPropertyValues.containsKey(MODULE_IDS)
				|| newPropertyValues.containsKey(DEFAULT_MODULE_ID)
				|| newPropertyValues.containsKey(NAMESPACE_ID)
				|| newPropertyValues.containsKey(PREVIOUS_EDITION_DEPENDENCY_EFFECTIVE_DATE)
				|| newPropertyValues.containsKey(RELEASE_EXTENSION_AS_AN_EDITION);
		if (isExtensionConfigFieldPresent) {
			String dependencyPackageRelease = newPropertyValues.get(EXTENSION_DEPENDENCY_RELEASE);
			String defaultModuleId = newPropertyValues.get(DEFAULT_MODULE_ID);
			String moduleIds = newPropertyValues.get(MODULE_IDS);
			String namespaceID = newPropertyValues.get(NAMESPACE_ID);
			String releaseAsEdition = newPropertyValues.get(RELEASE_EXTENSION_AS_AN_EDITION);
			String previousEditionDependencyEffectiveDate = newPropertyValues.get(PREVIOUS_EDITION_DEPENDENCY_EFFECTIVE_DATE);

			boolean isInvalidExtensionConfigValues = !StringUtils.hasLength(dependencyPackageRelease)
					&& !StringUtils.hasLength(moduleIds)
					&& !StringUtils.hasLength(defaultModuleId)
					&& !StringUtils.hasLength(namespaceID)
					&& (!StringUtils.hasLength(releaseAsEdition) || !TRUE.equals(releaseAsEdition));
			if (isInvalidExtensionConfigValues) {
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

				setExtensionDependencyPackage(newPropertyValues, configuration, dependencyPackageRelease);
				setConfigurationValueIfPresent(newPropertyValues, MODULE_IDS, configuration.getExtensionConfig(), MODULE_IDS, false);
				setConfigurationValueIfPresent(newPropertyValues, DEFAULT_MODULE_ID, configuration.getExtensionConfig(), DEFAULT_MODULE_ID, false);
				setConfigurationValueIfPresent(newPropertyValues, NAMESPACE_ID, configuration.getExtensionConfig(), NAMESPACE_ID, false);
				setConfigurationValueIfPresent(newPropertyValues, RELEASE_EXTENSION_AS_AN_EDITION, configuration.getExtensionConfig(), RELEASE_AS_AN_EDITION, true);
				setPreviousEditionDependencyEffectiveDate(newPropertyValues, configuration, previousEditionDependencyEffectiveDate);
			}
		}
	}

	private void setPreviousEditionDependencyEffectiveDate(Map<String, String> newPropertyValues, BuildConfiguration configuration, String previousEditionDependencyEffectiveDate) throws BadRequestException {
		if (newPropertyValues.containsKey(PREVIOUS_EDITION_DEPENDENCY_EFFECTIVE_DATE)) {
			Date previousDependencyDate;
			try {
				if (previousEditionDependencyEffectiveDate.contains("-")) {
					previousDependencyDate = DateFormatUtils.ISO_8601_EXTENDED_DATE_FORMAT.parse(previousEditionDependencyEffectiveDate);
				} else {
					previousDependencyDate = RF2Constants.DATE_FORMAT.parse(previousEditionDependencyEffectiveDate);
				}
				configuration.getExtensionConfig().setPreviousEditionDependencyEffectiveDate(previousDependencyDate);
			} catch (ParseException e) {
				throw new BadRequestException("Invalid " + previousEditionDependencyEffectiveDate + " format." +
						" Expecting format in either" + ISO_DATE_FORMAT.getPattern() + " or " + RF2Constants.DATE_FORMAT.getPattern(), e);
			}
		}
	}

	private void setExtensionDependencyPackage(Map<String, String> newPropertyValues, BuildConfiguration configuration, String dependencyPackageRelease) {
		if (newPropertyValues.containsKey(EXTENSION_DEPENDENCY_RELEASE)) {
			if (!StringUtils.hasLength(dependencyPackageRelease)) {
				configuration.getExtensionConfig().setDependencyRelease(null);
			} else {
				final ReleaseCenter releaseCenter = new ReleaseCenter();
				releaseCenter.setShortName(INTERNATIONAL);
				//Validate that a file of that name actually exists
				boolean pppExists = false;
				Exception rootCause = new Exception("No further information");
				try {
					pppExists = publishService.isReleaseFileExistInMSC(dependencyPackageRelease);
					if (!pppExists) {
						pppExists = publishService.exists(releaseCenter, dependencyPackageRelease);
					}
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

	public boolean isJSONValid(String test) {
		try {
			new JSONObject(test);
		} catch (JSONException ex) {
			return false;
		}
		return true;
	}
}

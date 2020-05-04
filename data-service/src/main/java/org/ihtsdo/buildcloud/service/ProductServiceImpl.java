package org.ihtsdo.buildcloud.service;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.ihtsdo.buildcloud.dao.ProductDAO;
import org.ihtsdo.buildcloud.dao.ReleaseCenterDAO;
import org.ihtsdo.buildcloud.entity.BuildConfiguration;
import org.ihtsdo.buildcloud.entity.ExtensionConfig;
import org.ihtsdo.buildcloud.entity.Product;
import org.ihtsdo.buildcloud.entity.QATestConfig;
import org.ihtsdo.buildcloud.entity.ReleaseCenter;
import org.ihtsdo.buildcloud.entity.User;
import org.ihtsdo.buildcloud.entity.helper.EntityHelper;
import org.ihtsdo.buildcloud.service.helper.FilterOption;
import org.ihtsdo.buildcloud.service.security.SecurityHelper;
import org.ihtsdo.otf.rest.exception.AuthenticationException;
import org.ihtsdo.otf.rest.exception.BadConfigurationException;
import org.ihtsdo.otf.rest.exception.BadRequestException;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.otf.rest.exception.EntityAlreadyExistsException;
import org.ihtsdo.otf.rest.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ProductServiceImpl extends EntityServiceImpl<Product> implements ProductService {

	private static final Logger LOGGER = LoggerFactory.getLogger(ProductServiceImpl.class);

	@Autowired
	private ProductDAO productDAO;

	@Autowired
	private ReleaseCenterDAO releaseCenterDAO;

	@Autowired
	PublishService publishService;

	@Autowired
	public ProductServiceImpl(final ProductDAO dao) {
		super(dao);
	}

	@Override
	public List<Product> findAll(final String releaseCenterKey, final Set<FilterOption> filterOptions) throws AuthenticationException {
		return productDAO.findAll(releaseCenterKey, filterOptions, SecurityHelper.getRequiredUser());
	}

	@Override
	public Product find(final String releaseCenterKey, final String productKey) throws BusinessServiceException {
		return productDAO.find(releaseCenterKey, productKey, SecurityHelper.getRequiredUser());
	}

	@Override
	public Product create(final String releaseCenterKey, final String productName) throws BusinessServiceException {
		final User user = SecurityHelper.getRequiredUser();
		LOGGER.info("create product, releaseCenterBusinessKey: {}", releaseCenterKey);

		final ReleaseCenter releaseCenter = releaseCenterDAO.find(releaseCenterKey, user);

		if (releaseCenter == null) {
			throw new ResourceNotFoundException("Unable to find Release Center: " + releaseCenterKey);
		}

		// Check that we don't already have one of these
		final String productBusinessKey = EntityHelper.formatAsBusinessKey(productName);
		final Product existingProduct = productDAO.find(releaseCenterKey, productBusinessKey, user);
		if (existingProduct != null) {
			throw new EntityAlreadyExistsException("Product named '" + productName + "' already exists.");
		}

		final Product product = new Product(productName);
		releaseCenter.addProduct(product);
		productDAO.save(product);
		return product;
	}

	@Override
	public Product update(final String releaseCenterKey, final String productKey, final Map<String, String> newPropertyValues) throws BusinessServiceException {
		LOGGER.info("update product, newPropertyValues: {}", newPropertyValues);
		final Product product = find(releaseCenterKey, productKey);
		if (product == null ) {
			throw new ResourceNotFoundException("No product found for product key:" + productKey);
		}
		updateProductBuildConfiguration(newPropertyValues, product);
		updateProductQaTestConfig(newPropertyValues, product);
		productDAO.update(product);
		return product;
	}

	private void updateProductQaTestConfig(final Map<String, String> newPropertyValues, final Product product) {
		QATestConfig qaTestConfig = product.getQaTestConfig();
		if( qaTestConfig == null ) {
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
			qaTestConfig.setEnableDrools(Boolean.parseBoolean(newPropertyValues.get(ENABLE_DROOLS)));
		}
		if(newPropertyValues.containsKey(DROOLS_RULES_GROUP_NAMES)) {
			qaTestConfig.setDroolsRulesGroupNames(newPropertyValues.get(DROOLS_RULES_GROUP_NAMES));
		}
		if(newPropertyValues.containsKey(ENABLE_MRCM)) {
			qaTestConfig.setEnableMRCMValidation(TRUE.equals(newPropertyValues.get(ENABLE_MRCM)));
		}
	}

	private void updateProductBuildConfiguration(final Map<String, String> newPropertyValues, final Product product)
			throws ResourceNotFoundException, BadRequestException, BadConfigurationException {
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

		if (newPropertyValues.containsKey(CREATE_INFERRED_RELATIONSHIPS)) {
			configuration.setCreateInferredRelationships(TRUE.equals(newPropertyValues.get(CREATE_INFERRED_RELATIONSHIPS)));
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
		
		if (newPropertyValues.containsKey(USE_EXTERNAL_CLASSIFIER)) {
			configuration.setUseExternalClassifier(TRUE.equals(newPropertyValues.get(USE_EXTERNAL_CLASSIFIER)));
		}

		if (newPropertyValues.containsKey(PREVIOUS_PUBLISHED_PACKAGE)) {
			final ReleaseCenter releaseCenter = product.getReleaseCenter();
			final String pPP = newPropertyValues.get(PREVIOUS_PUBLISHED_PACKAGE);
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
		
		if (newPropertyValues.containsKey(DEPENDENCY_RELEASE_PACKAGE)) {
			final ReleaseCenter releaseCenter = new ReleaseCenter();
			releaseCenter.setShortName("International");
			final String dependencyReleasePackage = newPropertyValues.get(DEPENDENCY_RELEASE_PACKAGE);
			//Validate that a file of that name actually exists
			boolean pppExists = false;
			Exception rootCause = new Exception("No further information");
			try {
				pppExists = publishService.exists(releaseCenter, dependencyReleasePackage);
			} catch (final Exception e) {
				rootCause = e;
			}
			
			if (pppExists) {
				if (configuration.getExtensionConfig() == null) {
				ExtensionConfig extConfig = new ExtensionConfig();
				configuration.setExtensionConfig(extConfig);
				extConfig.setBuildConfiguration(configuration);
				}
				configuration.getExtensionConfig().setDependencyRelease(dependencyReleasePackage);
			} else {
				throw new ResourceNotFoundException("Could not find dependency release package: " + dependencyReleasePackage, rootCause);
			}
		}
		
		if (newPropertyValues.containsKey(MODULE_ID)) {
			if (configuration.getExtensionConfig() == null) {
				ExtensionConfig extConfig = new ExtensionConfig();
				configuration.setExtensionConfig(extConfig);
				extConfig.setBuildConfiguration(configuration);
			}
			configuration.getExtensionConfig().setModuleId(newPropertyValues.get(MODULE_ID));
			
		}
		
		if (newPropertyValues.containsKey(NAMESPACE_ID)) {
			if (configuration.getExtensionConfig() == null) {
				ExtensionConfig extConfig = new ExtensionConfig();
				configuration.setExtensionConfig(extConfig);
				extConfig.setBuildConfiguration(configuration);
			}
			configuration.getExtensionConfig().setNamespaceId(newPropertyValues.get(NAMESPACE_ID));
			
		}
		
		if (newPropertyValues.containsKey(RELEASE_AS_AN_EDITION)) {
			if (configuration.getExtensionConfig() == null) {
				ExtensionConfig extConfig = new ExtensionConfig();
				configuration.setExtensionConfig(extConfig);
				extConfig.setBuildConfiguration(configuration);
			}
			configuration.getExtensionConfig().setReleaseAsAnEdition(TRUE.equals(newPropertyValues.get(RELEASE_AS_AN_EDITION)));
		}

		if (newPropertyValues.containsKey(CUSTOM_REFSET_COMPOSITE_KEYS)) {
			final Map<String, List<Integer>> refsetCompositeKeyMap = new HashMap<>();
			try {
				final String refsetCompositeKeyIndexes = newPropertyValues.get(CUSTOM_REFSET_COMPOSITE_KEYS);
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

		if(newPropertyValues.containsKey(INCLUDED_PREV_RELEASE_FILES)) {
			configuration.setIncludePrevReleaseFiles(newPropertyValues.get(INCLUDED_PREV_RELEASE_FILES));
		}

	}

}

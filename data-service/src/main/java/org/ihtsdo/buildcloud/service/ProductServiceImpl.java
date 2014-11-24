package org.ihtsdo.buildcloud.service;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.ihtsdo.buildcloud.dao.ProductDAO;
import org.ihtsdo.buildcloud.dao.ReleaseCenterDAO;
import org.ihtsdo.buildcloud.entity.Product;
import org.ihtsdo.buildcloud.entity.ReleaseCenter;
import org.ihtsdo.buildcloud.entity.User;
import org.ihtsdo.buildcloud.entity.helper.EntityHelper;
import org.ihtsdo.buildcloud.service.exception.*;
import org.ihtsdo.buildcloud.service.helper.FilterOption;
import org.ihtsdo.buildcloud.service.security.SecurityHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.ParseException;
import java.util.*;

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
	public ProductServiceImpl(ProductDAO dao) {
		super(dao);
	}

	@Override
	public List<Product> findAll(String releaseCenterKey, Set<FilterOption> filterOptions) throws AuthenticationException {
		return productDAO.findAll(filterOptions, SecurityHelper.getRequiredUser());
	}

	@Override
	public Product find(String releaseCenterKey, String productKey) throws BusinessServiceException {
		return productDAO.find(releaseCenterKey, productKey, SecurityHelper.getRequiredUser());
	}

	@Override
	public Product create(String releaseCenterKey, String productName) throws BusinessServiceException {
		User user = SecurityHelper.getRequiredUser();
		LOGGER.info("create product, releaseCenterBusinessKey: {}", releaseCenterKey);

		ReleaseCenter releaseCenter = releaseCenterDAO.find(releaseCenterKey, user);

		if (releaseCenter == null) {
			throw new ResourceNotFoundException("Unable to find Release Center: " + releaseCenterKey);
		}

		// Check that we don't already have one of these
		String productBusinessKey = EntityHelper.formatAsBusinessKey(productName);
		Product existingProduct = productDAO.find(releaseCenterKey, productBusinessKey, user);
		if (existingProduct != null) {
			throw new EntityAlreadyExistsException("Product named '" + productName + "' already exists.");
		}

		Product product = new Product(productName);
		releaseCenter.addProduct(product);
		productDAO.save(product);
		return product;
	}

	@Override
	public Product update(String releaseCenterKey, String productKey, Map<String, String> newPropertyValues) throws BusinessServiceException {
		LOGGER.info("update product, newPropertyValues: {}", newPropertyValues);
		Product product = find(releaseCenterKey, productKey);

		if (product == null) {
			throw new ResourceNotFoundException("Unable to find product: " + productKey);
		}
		if (newPropertyValues.containsKey(EFFECTIVE_TIME)) {
			try {
				Date date = DateFormatUtils.ISO_DATE_FORMAT.parse(newPropertyValues.get(EFFECTIVE_TIME));
				product.setEffectiveTime(date);
			} catch (ParseException e) {
				throw new BadRequestException("Invalid " + EFFECTIVE_TIME + " format. Expecting format " + DateFormatUtils.ISO_DATE_FORMAT.getPattern() + ".", e);
			}
		}
		if (newPropertyValues.containsKey(ProductService.JUST_PACKAGE)) {
			product.setJustPackage(TRUE.equals(newPropertyValues.get(ProductService.JUST_PACKAGE)));
		}

		if (newPropertyValues.containsKey(ProductService.FIRST_TIME_RELEASE)) {
			product.setFirstTimeRelease(TRUE.equals(newPropertyValues.get(ProductService.FIRST_TIME_RELEASE)));
		}

		if (newPropertyValues.containsKey(ProductService.CREATE_INFERRED_RELATIONSHIPS)) {
			product.setCreateInferredRelationships(TRUE.equals(newPropertyValues.get(ProductService.CREATE_INFERRED_RELATIONSHIPS)));
		}

		if (newPropertyValues.containsKey(ProductService.WORKBENCH_DATA_FIXES_REQUIRED)) {
			product.setWorkbenchDataFixesRequired(TRUE.equals(newPropertyValues.get(ProductService.WORKBENCH_DATA_FIXES_REQUIRED)));
		}

		if (newPropertyValues.containsKey(ProductService.PREVIOUS_PUBLISHED_PACKAGE)) {
			ReleaseCenter releaseCenter = product.getReleaseCenter();
			String pPP = newPropertyValues.get(ProductService.PREVIOUS_PUBLISHED_PACKAGE);
			//Validate that a file of that name actually exists
			boolean pppExists = false;
			Exception rootCause = new Exception("No further information");
			try {
				pppExists = publishService.exists(releaseCenter, pPP);
			} catch (Exception e) {
				rootCause = e;
			}

			if (pppExists) {
				product.setPreviousPublishedPackage(pPP);
			} else {
				throw new ResourceNotFoundException("Could not find previously published package: " + pPP, rootCause);
			}
		}

		if (newPropertyValues.containsKey(ProductService.CUSTOM_REFSET_COMPOSITE_KEYS)) {
			Map<String, List<Integer>> refsetCompositeKeyMap = new HashMap<>();
			try {
				String refsetCompositeKeyIndexes = newPropertyValues.get(ProductService.CUSTOM_REFSET_COMPOSITE_KEYS);
				String[] split = refsetCompositeKeyIndexes.split("\\|");
				for (String refsetKeyAndIndexes : split) {
					refsetKeyAndIndexes = refsetKeyAndIndexes.trim();
					if (!refsetKeyAndIndexes.isEmpty()) {
						String[] keyAndIndexes = refsetKeyAndIndexes.split("=", 2);
						String refsetKey = keyAndIndexes[0].trim();
						List<Integer> indexes = new ArrayList<>();
						String value = keyAndIndexes[1];
						String[] indexStrings = value.split(",");
						for (String indexString : indexStrings) {
							String trim = indexString.trim();
							indexes.add(Integer.parseInt(trim));
						}
						refsetCompositeKeyMap.put(refsetKey, indexes);
					}
				}
			} catch (NumberFormatException e) {
				throw new BadConfigurationException("Failed to parse " + ProductService.CUSTOM_REFSET_COMPOSITE_KEYS);
			}
			product.setCustomRefsetCompositeKeys(refsetCompositeKeyMap);
		}

		if (newPropertyValues.containsKey(ProductService.README_HEADER)) {
			String readmeHeader = newPropertyValues.get(ProductService.README_HEADER);
			product.setReadmeHeader(readmeHeader);
		}

		if (newPropertyValues.containsKey(ProductService.README_END_DATE)) {
			String readmeEndDate = newPropertyValues.get(ProductService.README_END_DATE);
			product.setReadmeEndDate(readmeEndDate);
		}

		if (newPropertyValues.containsKey(ProductService.NEW_RF2_INPUT_FILES)) {
			product.setNewRF2InputFiles(newPropertyValues.get(ProductService.NEW_RF2_INPUT_FILES));
		}

		productDAO.update(product);
		return product;
	}

}

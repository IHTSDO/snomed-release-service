package org.ihtsdo.buildcloud.service;

import org.ihtsdo.buildcloud.dao.BuildDAO;
import org.ihtsdo.buildcloud.dao.PackageDAO;
import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.Package;
import org.ihtsdo.buildcloud.entity.Product;
import org.ihtsdo.buildcloud.entity.User;
import org.ihtsdo.buildcloud.service.exception.BadConfigurationException;
import org.ihtsdo.buildcloud.service.exception.EntityAlreadyExistsException;
import org.ihtsdo.buildcloud.service.exception.ResourceNotFoundException;
import org.ihtsdo.buildcloud.service.helper.CompositeKeyHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional
public class PackageServiceImpl extends EntityServiceImpl<Package> implements PackageService {

	private static final String TRUE = "true";

	@Autowired
	PublishService publishService;

	@Autowired
	private PackageDAO packageDAO;

	@Autowired
	private BuildDAO buildDAO;

	@Autowired
	protected PackageServiceImpl(final PackageDAO dao) {
		super(dao);
	}

	@Override
	public final Package find(final String buildCompositeKey, final String packageBusinessKey, final User authenticatedUser) throws ResourceNotFoundException {
		Long buildId = CompositeKeyHelper.getId(buildCompositeKey);
		if (buildId == null) {
			throw new ResourceNotFoundException("Unable to find build: " + buildCompositeKey);
		}
		return packageDAO.find(buildId, packageBusinessKey, authenticatedUser);
	}

	@Override
	public final List<Package> findAll(final String buildCompositeKey, final User authenticatedUser) throws ResourceNotFoundException {
		Long buildId = CompositeKeyHelper.getId(buildCompositeKey);
		if (buildId == null) {
			throw new ResourceNotFoundException("Unable to find build: " + buildCompositeKey);
		}
		return new ArrayList<>(buildDAO.find(buildId, authenticatedUser).getPackages());
	}

	@Override
	public final Package create(final String buildCompositeKey, final String name, final User authenticatedUser) throws EntityAlreadyExistsException, ResourceNotFoundException {
		Long buildId = CompositeKeyHelper.getId(buildCompositeKey);
		if (buildId == null) {
			throw new ResourceNotFoundException("Unable to find build identifier from: " + buildCompositeKey);
		}
		Build build = buildDAO.find(buildId, authenticatedUser);
		if (build == null) {
			throw new ResourceNotFoundException("Unable to find build: " + buildCompositeKey);
		}
		Package pkg = new Package(name);
		if (build.addPackage(pkg)) {
			packageDAO.save(pkg);
			return pkg;
		} else {
			throw new EntityAlreadyExistsException("Package:"+ pkg.getBusinessKey() + " already exists!");
		}
	}

	@Override
	public final Package update(final String buildCompositeKey, final String packageBusinessKey,
			final Map<String, String> newPropertyValues, final User authenticatedUser) throws ResourceNotFoundException, BadConfigurationException {
		Long buildId = CompositeKeyHelper.getId(buildCompositeKey);
		if (buildId == null) {
			throw new ResourceNotFoundException("Unable to find build: " + buildCompositeKey);
		}
		Package aPackage = packageDAO.find(buildId, packageBusinessKey, authenticatedUser);
		if (aPackage == null) {
			String item = CompositeKeyHelper.getPath(buildCompositeKey, packageBusinessKey);
			throw new ResourceNotFoundException("Unable to find package: " + item);
		}
		Product product = aPackage.getBuild().getProduct();

		if (newPropertyValues.containsKey(PackageService.JUST_PACKAGE)) {
			aPackage.setJustPackage(TRUE.equals(newPropertyValues.get(JUST_PACKAGE)));
		}

		if (newPropertyValues.containsKey(PackageService.FIRST_TIME_RELEASE)) {
			aPackage.setFirstTimeRelease(TRUE.equals(newPropertyValues.get(FIRST_TIME_RELEASE)));
		}

		if (newPropertyValues.containsKey(PackageService.CREATE_INFERRED_RELATIONSHIPS)) {
			aPackage.setCreateInferredRelationships(TRUE.equals(newPropertyValues.get(CREATE_INFERRED_RELATIONSHIPS)));
		}

		if (newPropertyValues.containsKey(PackageService.WORKBENCH_DATA_FIXES_REQUIRED)) {
			aPackage.setWorkbenchDataFixesRequired(TRUE.equals(newPropertyValues.get(WORKBENCH_DATA_FIXES_REQUIRED)));
		}

		if (newPropertyValues.containsKey(PackageService.PREVIOUS_PUBLISHED_PACKAGE)) {
			String pPP = newPropertyValues.get(PREVIOUS_PUBLISHED_PACKAGE);
			//Validate that a file of that name actually exists
			boolean pppExists = false;
			Exception rootCause = new Exception("No further information");
			try {
				pppExists = publishService.exists(product, pPP);
			} catch (Exception e) {
				rootCause = e;
			}

			if (pppExists) {
				aPackage.setPreviousPublishedPackage(pPP);
			} else {
				throw new ResourceNotFoundException("Could not find previously published package: " + pPP, rootCause);
			}
		}

		if (newPropertyValues.containsKey(PackageService.CUSTOM_REFSET_COMPOSITE_KEYS)) {
			Map<String, List<Integer>> refsetCompositeKeyMap = new HashMap<>();
			try {
				String refsetCompositeKeyIndexes = newPropertyValues.get(CUSTOM_REFSET_COMPOSITE_KEYS);
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
				throw new BadConfigurationException("Failed to parse " + CUSTOM_REFSET_COMPOSITE_KEYS);
			}
			aPackage.setCustomRefsetCompositeKeys(refsetCompositeKeyMap);
		}

		if (newPropertyValues.containsKey(PackageService.README_HEADER)) {
			String readmeHeader = newPropertyValues.get(PackageService.README_HEADER);
			aPackage.setReadmeHeader(readmeHeader);
		}

		if (newPropertyValues.containsKey(PackageService.README_END_DATE)) {
			String readmeEndDate = newPropertyValues.get(PackageService.README_END_DATE);
			aPackage.setReadmeEndDate(readmeEndDate);
		}

		if (newPropertyValues.containsKey(PackageService.NEW_RF2_INPUT_FILES)) {
			aPackage.setNewRF2InputFiles(newPropertyValues.get(PackageService.NEW_RF2_INPUT_FILES));
		}

		packageDAO.update(aPackage);
		return aPackage;
	}

}

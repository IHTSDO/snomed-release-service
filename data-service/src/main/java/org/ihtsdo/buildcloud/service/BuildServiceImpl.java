package org.ihtsdo.buildcloud.service;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.ihtsdo.buildcloud.dao.BuildDAO;
import org.ihtsdo.buildcloud.dao.ReleaseCenterDAO;
import org.ihtsdo.buildcloud.entity.Build;
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
public class BuildServiceImpl extends EntityServiceImpl<Build> implements BuildService {

	private static final Logger LOGGER = LoggerFactory.getLogger(BuildServiceImpl.class);

	@Autowired
	private BuildDAO buildDAO;

	@Autowired
	private ReleaseCenterDAO releaseCenterDAO;

	@Autowired
	PublishService publishService;

	@Autowired
	public BuildServiceImpl(BuildDAO dao) {
		super(dao);
	}

	@Override
	public List<Build> findAll(String releaseCenterKey, Set<FilterOption> filterOptions) throws AuthenticationException {
		return buildDAO.findAll(filterOptions, SecurityHelper.getRequiredUser());
	}

	@Override
	public Build find(String releaseCenterKey, String buildKey) throws BusinessServiceException {
		return buildDAO.find(releaseCenterKey, buildKey, SecurityHelper.getRequiredUser());
	}

	@Override
	public Build create(String releaseCenterKey, String buildName) throws BusinessServiceException {
		User user = SecurityHelper.getRequiredUser();
		LOGGER.info("create build, releaseCenterBusinessKey: {}", releaseCenterKey);

		ReleaseCenter releaseCenter = releaseCenterDAO.find(releaseCenterKey, user);

		if (releaseCenter == null) {
			throw new ResourceNotFoundException("Unable to find Release Center: " + releaseCenterKey);
		}

		// Check that we don't already have one of these
		String buildBusinessKey = EntityHelper.formatAsBusinessKey(buildName);
		Build existingBuild = buildDAO.find(releaseCenterKey, buildBusinessKey, user);
		if (existingBuild != null) {
			throw new EntityAlreadyExistsException("Build named '" + buildName + "' already exists.");
		}

		Build build = new Build(buildName);
		releaseCenter.addBuild(build);
		buildDAO.save(build);
		return build;
	}

	@Override
	public Build update(String releaseCenterKey, String buildKey, Map<String, String> newPropertyValues) throws BusinessServiceException {
		LOGGER.info("update build, newPropertyValues: {}", newPropertyValues);
		Build build = find(releaseCenterKey, buildKey);

		if (build == null) {
			throw new ResourceNotFoundException("Unable to find build: " + buildKey);
		}
		if (newPropertyValues.containsKey(EFFECTIVE_TIME)) {
			try {
				Date date = DateFormatUtils.ISO_DATE_FORMAT.parse(newPropertyValues.get(EFFECTIVE_TIME));
				build.setEffectiveTime(date);
			} catch (ParseException e) {
				throw new BadRequestException("Invalid " + EFFECTIVE_TIME + " format. Expecting format " + DateFormatUtils.ISO_DATE_FORMAT.getPattern() + ".", e);
			}
		}
		if (newPropertyValues.containsKey(BuildService.JUST_PACKAGE)) {
			build.setJustPackage(TRUE.equals(newPropertyValues.get(BuildService.JUST_PACKAGE)));
		}

		if (newPropertyValues.containsKey(BuildService.FIRST_TIME_RELEASE)) {
			build.setFirstTimeRelease(TRUE.equals(newPropertyValues.get(BuildService.FIRST_TIME_RELEASE)));
		}

		if (newPropertyValues.containsKey(BuildService.CREATE_INFERRED_RELATIONSHIPS)) {
			build.setCreateInferredRelationships(TRUE.equals(newPropertyValues.get(BuildService.CREATE_INFERRED_RELATIONSHIPS)));
		}

		if (newPropertyValues.containsKey(BuildService.WORKBENCH_DATA_FIXES_REQUIRED)) {
			build.setWorkbenchDataFixesRequired(TRUE.equals(newPropertyValues.get(BuildService.WORKBENCH_DATA_FIXES_REQUIRED)));
		}

		if (newPropertyValues.containsKey(BuildService.PREVIOUS_PUBLISHED_PACKAGE)) {
			ReleaseCenter releaseCenter = build.getReleaseCenter();
			String pPP = newPropertyValues.get(BuildService.PREVIOUS_PUBLISHED_PACKAGE);
			//Validate that a file of that name actually exists
			boolean pppExists = false;
			Exception rootCause = new Exception("No further information");
			try {
				pppExists = publishService.exists(releaseCenter, pPP);
			} catch (Exception e) {
				rootCause = e;
			}

			if (pppExists) {
				build.setPreviousPublishedPackage(pPP);
			} else {
				throw new ResourceNotFoundException("Could not find previously published package: " + pPP, rootCause);
			}
		}

		if (newPropertyValues.containsKey(BuildService.CUSTOM_REFSET_COMPOSITE_KEYS)) {
			Map<String, List<Integer>> refsetCompositeKeyMap = new HashMap<>();
			try {
				String refsetCompositeKeyIndexes = newPropertyValues.get(BuildService.CUSTOM_REFSET_COMPOSITE_KEYS);
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
				throw new BadConfigurationException("Failed to parse " + BuildService.CUSTOM_REFSET_COMPOSITE_KEYS);
			}
			build.setCustomRefsetCompositeKeys(refsetCompositeKeyMap);
		}

		if (newPropertyValues.containsKey(BuildService.README_HEADER)) {
			String readmeHeader = newPropertyValues.get(BuildService.README_HEADER);
			build.setReadmeHeader(readmeHeader);
		}

		if (newPropertyValues.containsKey(BuildService.README_END_DATE)) {
			String readmeEndDate = newPropertyValues.get(BuildService.README_END_DATE);
			build.setReadmeEndDate(readmeEndDate);
		}

		if (newPropertyValues.containsKey(BuildService.NEW_RF2_INPUT_FILES)) {
			build.setNewRF2InputFiles(newPropertyValues.get(BuildService.NEW_RF2_INPUT_FILES));
		}

		buildDAO.update(build);
		return build;
	}

}

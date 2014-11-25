package org.ihtsdo.buildcloud.service;

import org.ihtsdo.buildcloud.entity.Package;
import org.ihtsdo.buildcloud.entity.User;
import org.ihtsdo.buildcloud.service.exception.BadConfigurationException;
import org.ihtsdo.buildcloud.service.exception.EntityAlreadyExistsException;
import org.ihtsdo.buildcloud.service.exception.ResourceNotFoundException;

import java.util.List;
import java.util.Map;

public interface PackageService extends EntityService<Package> {

	String README_HEADER = "readmeHeader";
	String JUST_PACKAGE = "justPackage";
	String FIRST_TIME_RELEASE = "firstTimeRelease";
	String PREVIOUS_PUBLISHED_PACKAGE = "previousPublishedPackage";
	String README_END_DATE = "readmeEndDate";
	String WORKBENCH_DATA_FIXES_REQUIRED = "workbenchDataFixesRequired";
	String CREATE_LEGACY_IDS = "createLegacyIds";
	String CREATE_INFERRED_RELATIONSHIPS = "createInferredRelationships";
	String CUSTOM_REFSET_COMPOSITE_KEYS = "customRefsetCompositeKeys";
	String NEW_RF2_INPUT_FILES = "newRF2InputFiles";

	Package find(String buildCompositeKey, String packageBusinessKey, User authenticatedUser) throws ResourceNotFoundException;

	List<Package> findAll(String buildCompositeKey, User authenticatedUser) throws ResourceNotFoundException;

	Package create(String buildBusinessKey, String name, User authenticatedUser) throws EntityAlreadyExistsException, ResourceNotFoundException;

	Package update(String buildCompositeKey, String packageBusinessKey, Map<String, String> newPropertyValues, User authenticatedUser) throws ResourceNotFoundException, BadConfigurationException;

}

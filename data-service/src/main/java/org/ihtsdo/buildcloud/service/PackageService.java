package org.ihtsdo.buildcloud.service;

import org.ihtsdo.buildcloud.entity.Package;
import org.ihtsdo.buildcloud.entity.User;
import org.ihtsdo.buildcloud.service.exception.ResourceNotFoundException;
import org.ihtsdo.buildcloud.service.exception.EntityAlreadyExistsException;

import java.util.List;
import java.util.Map;

public interface PackageService extends EntityService<Package> {
	

	static final String README_HEADER = "readmeHeader";
	
	static final String FIRST_TIME_RELEASE = "firstTimeRelease";

	static final String PREVIOUS_PUBLISHED_PACKAGE = "previousPublishedPackage";

	Package find(String buildCompositeKey, String packageBusinessKey, User authenticatedUser);

	List<Package> findAll(String buildCompositeKey, User authenticatedUser);
	
	Package create(String buildBusinessKey, String name, User authenticatedUser) throws EntityAlreadyExistsException;

	Package update(String buildCompositeKey, String packageBusinessKey, Map<String, String> newPropertyValues, User authenticatedUser) throws ResourceNotFoundException;

}

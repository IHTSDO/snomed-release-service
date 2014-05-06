package org.ihtsdo.buildcloud.service;

import org.ihtsdo.buildcloud.entity.Package;
import org.ihtsdo.buildcloud.entity.User;

import java.util.List;

public interface PackageService extends EntityService<Package> {

	Package find(String buildCompositeKey, String packageBusinessKey, User authenticatedUser);

	List<Package> findAll(String buildCompositeKey, User authenticatedUser);
	
	Package create(String buildBusinessKey, String name, User authenticatedUser);
	
}

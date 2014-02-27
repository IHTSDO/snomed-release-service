package org.ihtsdo.buildcloud.service;

import org.ihtsdo.buildcloud.entity.Package;

import java.util.List;

public interface PackageService extends EntityService<Package> {

	Package find(String buildCompositeKey, String packageBusinessKey, String authenticatedId);

	List<Package> findAll(String buildCompositeKey, String authenticatedId);
	
	Package create(String buildBusinessKey, String name, String authenticatedId);
	
}

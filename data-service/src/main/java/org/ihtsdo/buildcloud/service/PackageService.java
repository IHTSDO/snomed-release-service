package org.ihtsdo.buildcloud.service;

import org.ihtsdo.buildcloud.entity.Package;

import java.util.Set;

public interface PackageService {

	Set<Package> findAll(String releaseCentreBusinessKey, String extensionBusinessKey, String productBusinessKey,
						 String buildBusinessKey, String authenticatedId);

	Package find(String releaseCentreBusinessKey, String extensionBusinessKey, String productBusinessKey,
				 String buildBusinessKey, String packageBusinessKey, String authenticatedId);

}

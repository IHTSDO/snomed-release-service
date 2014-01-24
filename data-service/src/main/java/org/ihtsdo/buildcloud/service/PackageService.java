package org.ihtsdo.buildcloud.service;

import org.ihtsdo.buildcloud.entity.Package;

import java.util.Set;

public interface PackageService {

	Set<Package> findAll(String releaseCentreBusinessKey, String extensionBusinessKey, String productBusinessKey, String oauthId);

	Package find(String releaseCentreBusinessKey, String extensionBusinessKey, String productBusinessKey, String packageBusinessKey, String oauthId);

}

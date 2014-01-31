package org.ihtsdo.buildcloud.service;

import org.ihtsdo.buildcloud.entity.Build;

import java.util.Set;

public interface BuildService {

	Set<Build> findAll(String releaseCentreBusinessKey, String extensionBusinessKey, String productBusinessKey, String authenticatedId);

	Build find(String releaseCentreBusinessKey, String extensionBusinessKey, String productBusinessKey, String buildBusinessKey, String authenticatedId);

}

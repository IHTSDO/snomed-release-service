package org.ihtsdo.buildcloud.service;

import org.ihtsdo.buildcloud.entity.Build;

import java.util.List;
import java.util.Set;

public interface BuildService {

	List<Build> findAll(String authenticatedId);

	Build find(String buildCompositeKey, String authenticatedId);

	Set<Build> findForProduct(String releaseCentreBusinessKey, String extensionBusinessKey, String productBusinessKey, String authenticatedId);
}

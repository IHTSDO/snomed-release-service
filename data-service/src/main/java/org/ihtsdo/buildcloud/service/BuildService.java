package org.ihtsdo.buildcloud.service;

import org.ihtsdo.buildcloud.entity.Build;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface BuildService {

	List<Build> findAll(String authenticatedId);

	Build find(String buildCompositeKey, String authenticatedId);
	
	Map<String, Object> getConfig(String buildCompositeKey, String authenticatedId);

	List<Build> findForProduct(String releaseCentreBusinessKey, String extensionBusinessKey, String productBusinessKey, String authenticatedId);

	String run(Build build) throws IOException;

}

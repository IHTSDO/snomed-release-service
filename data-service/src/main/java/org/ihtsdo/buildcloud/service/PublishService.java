package org.ihtsdo.buildcloud.service;

import java.util.List;

import org.ihtsdo.buildcloud.entity.Execution;
import org.ihtsdo.buildcloud.entity.Package;

/**
 * Service to publish a build release after verification tests are 
 * carried out successfully. It also provides functions to list all available published release packages.
 */
public interface PublishService {
	List<String> findPublisheddReleaseFileNames( String releaseCentreKey, String extensionKey, String productKey);
	void publishExecutionPackage(Execution execution, Package pk);
}

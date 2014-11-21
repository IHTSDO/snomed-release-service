package org.ihtsdo.buildcloud.service;

import org.ihtsdo.buildcloud.entity.Execution;
import org.ihtsdo.buildcloud.entity.ReleaseCenter;
import org.ihtsdo.buildcloud.service.exception.BusinessServiceException;

import java.io.InputStream;
import java.util.List;

/**
 * Service to publish a build release after verification tests are
 * carried out successfully. It also provides functions to list all available published release packages.
 */
public interface PublishService {

	List<String> getPublishedPackages(ReleaseCenter releaseCenter);

	void publishExecution(Execution execution) throws BusinessServiceException;

	void publishAdHocFile(ReleaseCenter releaseCenter, InputStream inputStream, String originalFilename, long size) throws BusinessServiceException;

	boolean exists(ReleaseCenter releaseCenter, String previouslyPublishedPackageName);

}

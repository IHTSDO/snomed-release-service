package org.ihtsdo.buildcloud.service;

import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.ReleaseCenter;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;

import java.io.InputStream;
import java.util.List;

/**
 * Service to publish a product release after verification tests are
 * carried out successfully. It also provides functions to list all available published release packages.
 */
public interface PublishService {

	List<String> getPublishedPackages(ReleaseCenter releaseCenter);

	void publishBuild(Build build) throws BusinessServiceException;

	void publishAdHocFile(ReleaseCenter releaseCenter, InputStream inputStream, String originalFilename, long size) throws BusinessServiceException;

	boolean exists(ReleaseCenter releaseCenter, String previouslyPublishedPackageName);

}

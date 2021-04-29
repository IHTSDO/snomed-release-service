package org.ihtsdo.buildcloud.service;

import java.io.InputStream;
import java.util.List;

import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.ReleaseCenter;
import org.ihtsdo.buildcloud.service.helper.ProcessingStatus;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;

/**
 * Service to publish a product release after verification tests are
 * carried out successfully. It also provides functions to list all available published release packages.
 */
public interface PublishService {

	List<String> getPublishedPackages(ReleaseCenter releaseCenter);

	void publishBuild(Build build, boolean publishComponentIds, String env) throws BusinessServiceException;

	void publishBuildAsync(Build build, boolean publishComponentIds, String env) throws BusinessServiceException;

	ProcessingStatus getPublishingBuildStatus(Build build);

	void publishAdHocFile(ReleaseCenter releaseCenter, InputStream inputStream, String originalFilename, long size, boolean publishComponentIds) throws BusinessServiceException;

	boolean exists(ReleaseCenter releaseCenter, String previouslyPublishedPackageName);
}

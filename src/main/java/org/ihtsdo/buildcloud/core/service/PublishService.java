package org.ihtsdo.buildcloud.core.service;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.ihtsdo.buildcloud.core.service.helper.ProcessingStatus;
import org.ihtsdo.buildcloud.core.entity.Build;
import org.ihtsdo.buildcloud.core.entity.ReleaseCenter;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.otf.rest.exception.ResourceNotFoundException;

/**
 * Service to publish a product release after verification tests are
 * carried out successfully. It also provides functions to list all available published release packages.
 */
public interface PublishService {

	List<String> getPublishedPackages(ReleaseCenter releaseCenter);

	List<Build> findPublishedBuilds(String releaseCenterKey, String productKey, boolean includeProdPublishedReleases) throws ResourceNotFoundException;

	Map<String, String> getPublishedBuildPathMap(String releaseCenterKey, String productKey);

	void publishBuild(Build build, boolean publishComponentIds, String env) throws BusinessServiceException;

	void publishBuildAsync(Build build, boolean publishComponentIds, String env);

	ProcessingStatus getPublishingBuildStatus(Build build);

	void publishAdHocFile(ReleaseCenter releaseCenter, InputStream inputStream, String originalFilename, long size, boolean publishComponentIds) throws BusinessServiceException;

	boolean exists(ReleaseCenter releaseCenter, String previouslyPublishedPackageName);

	boolean exists(String releaseCenterKey, String previouslyPublishedPackageName);
}

package org.ihtsdo.buildcloud.service;

import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.service.termserver.GatherInputRequestPojo;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.springframework.security.core.Authentication;

public interface ReleaseService {

    Build createBuild(String releaseCenter, String productKey, GatherInputRequestPojo gatherInputRequestPojo, String currentUser) throws BusinessServiceException;

    CreateReleasePackageBuildRequest queueBuild(CreateReleasePackageBuildRequest build) throws BusinessServiceException;

    void triggerBuildAsync(String releaseCenter, String productKey, Build build, GatherInputRequestPojo gatherInputRequestPojo, Authentication authentication, String rootURL) throws BusinessServiceException;

    void clearConcurrentCache(String releaseCenterKey, String productKey);
}
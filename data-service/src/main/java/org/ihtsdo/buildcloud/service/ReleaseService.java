package org.ihtsdo.buildcloud.service;

import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.Product;
import org.ihtsdo.buildcloud.service.termserver.GatherInputRequestPojo;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;

public interface ReleaseService {

    Build createBuild(String releaseCenter, String productKey, GatherInputRequestPojo gatherInputRequestPojo, String currentUser) throws BusinessServiceException;

    void triggerBuildAsync(String releaseCenter, String productKey, Build build, GatherInputRequestPojo gatherInputRequestPojo, Authentication authentication) throws BusinessServiceException;

    void clearConcurrentCache(String releaseCenterKey, String productKey);
}

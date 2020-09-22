package org.ihtsdo.buildcloud.service;

import org.ihtsdo.buildcloud.entity.Product;
import org.ihtsdo.buildcloud.service.termserver.GatherInputRequestPojo;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;

public interface ReleaseService {

    void validateInProgressBuild(String releaseCenter, String productKey) throws BusinessServiceException;

    void createAndTriggerBuild(String releaseCenter, String productKey, GatherInputRequestPojo gatherInputRequestPojo, Authentication authentication, String currentUser) throws BusinessServiceException;

}

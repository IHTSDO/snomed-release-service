package org.ihtsdo.buildcloud.service;

import org.ihtsdo.buildcloud.service.termserver.GatherInputRequestPojo;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;

public interface ReleaseService {

    void createReleasePackage(String releaseCenter, String productKey, GatherInputRequestPojo gatherInputRequestPojo, Authentication authentication, String currentUser) throws BusinessServiceException;

}

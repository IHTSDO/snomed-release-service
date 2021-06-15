package org.ihtsdo.buildcloud.core.service;

import org.ihtsdo.buildcloud.core.service.inputfile.gather.GatherInputRequestPojo;
import org.ihtsdo.buildcloud.core.entity.Build;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.springframework.security.core.Authentication;

public interface ReleaseService {

    void runReleaseBuild(String releaseCenter, String productKey, Build build, GatherInputRequestPojo gatherInputRequestPojo, Authentication authentication);

}

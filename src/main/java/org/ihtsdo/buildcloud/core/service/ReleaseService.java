package org.ihtsdo.buildcloud.core.service;

import org.ihtsdo.buildcloud.core.service.inputfile.gather.BuildRequestPojo;
import org.ihtsdo.buildcloud.core.entity.Build;
import org.springframework.security.core.Authentication;

public interface ReleaseService {

    void runReleaseBuild(Build build, Authentication authentication);

}

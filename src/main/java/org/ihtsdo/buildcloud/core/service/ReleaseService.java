package org.ihtsdo.buildcloud.core.service;

import org.ihtsdo.buildcloud.core.entity.Build;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.springframework.security.core.Authentication;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.text.ParseException;

public interface ReleaseService {

    void runReleaseBuild(Build build, Authentication authentication) throws IOException;

    void startNewAuthoringCycle(String releaseCenterKey, String productKey, String effectiveTime, String productKeySource, String dependencyPackage) throws ParseException, JAXBException, IOException, BusinessServiceException;
}

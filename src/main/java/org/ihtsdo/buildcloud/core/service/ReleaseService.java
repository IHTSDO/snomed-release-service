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

    void startNewAuthoringCycleV2(String releaseCenterKey, String dailyBuildProductKey, Build publishedBuild, String nextCycleEffectiveTime, String previousRelease) throws BusinessServiceException;

    void replaceManifestFile(String releaseCenterKey, String productKey, Build build, String effectiveTime, String previousEffectiveTime) throws IOException;

    void copyExternallyMaintainedFiles(String releaseCenterKey, String source, String target, boolean isHeaderOnly) throws BusinessServiceException, IOException;
}

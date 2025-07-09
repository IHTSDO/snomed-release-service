package org.ihtsdo.buildcloud.core.service;

import org.ihtsdo.buildcloud.core.entity.Build;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.text.ParseException;

public interface ReleaseService {

    void runReleaseBuild(Build build) throws IOException;

    void startNewAuthoringCycleForDailyBuildProduct(String releaseCenterKey, String dailyBuildProductKey, String effectiveTime, String productKeySource, String dependencyPackage) throws ParseException, JAXBException, IOException, BusinessServiceException;

    void copyManifestFileAndReplaceEffectiveTime(String releaseCenterKey, String dailyBuildProductKey, Build previousPublishedBuild, String effectiveTime) throws IOException;
}

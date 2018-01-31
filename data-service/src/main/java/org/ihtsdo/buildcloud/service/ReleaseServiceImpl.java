package org.ihtsdo.buildcloud.service;

import org.apache.commons.codec.DecoderException;
import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.User;
import org.ihtsdo.buildcloud.service.security.SecurityHelper;
import org.ihtsdo.buildcloud.service.termserver.GatherInputRequestPojo;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

@Service
public class ReleaseServiceImpl implements ReleaseService{

    @Autowired
    ProductInputFileService productInputFileService;

    @Autowired
    BuildService buildService;

    @Autowired
    AuthenticationService authenticationService;

    private static final Logger LOGGER = LoggerFactory.getLogger(ReleaseServiceImpl.class);

    
    @Override
    @Async("securityContextAsyncTaskExecutor")
    public void createReleasePackage(String releaseCenter, String productKey, GatherInputRequestPojo gatherInputRequestPojo) {
        try {
            final User anonymousSubject = authenticationService.getAnonymousSubject();
            SecurityHelper.setUser(anonymousSubject);
            productInputFileService.gatherSourceFiles(releaseCenter, productKey, gatherInputRequestPojo);
            productInputFileService.prepareInputFiles(releaseCenter, productKey, true);
            Build build = buildService.createBuildFromProduct(releaseCenter, productKey);
            buildService.triggerBuild(releaseCenter, productKey, build.getId(), 10);
        } catch (Exception e) {
            LOGGER.error("Encounter error while creating package. Build process stopped. Error: ", e);
        }

    }
}

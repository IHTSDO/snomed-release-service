package org.ihtsdo.buildcloud.core.service;

import org.ihtsdo.buildcloud.core.entity.Build;
import org.ihtsdo.buildcloud.core.service.build.RF2Constants;
import org.ihtsdo.buildcloud.rest.pojo.PostReleaseRequest;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public class AdminServiceImpl implements AdminService {

    @Autowired
    private BuildService buildService;

    @Autowired
    private PublishService publishService;

    @Autowired
    private ReleaseService releaseService;

    @Override
    public void runPostReleaseTask(String releaseCenterKey, PostReleaseRequest request) throws BusinessServiceException, IOException {
        Build build = buildService.find(releaseCenterKey, request.releasedProductKey(), request.releasedBuildKey(), true, null, null, null);
        String publishedReleaseFileName = getReleaseFileName(releaseCenterKey, request.releasedProductKey(), request.releasedBuildKey());
        publishService.copyReleaseFileToPublishedStore(build);
        publishService.copyReleaseFileToVersionedContentStore(build);
        releaseService.replaceManifestFile(releaseCenterKey, request.dailyBuildProductKey(), build, request.nextCycleEffectiveTime().replace("-", ""), build.getConfiguration().getEffectiveTimeSnomedFormat());
        releaseService.copyExternallyMaintainedFiles(releaseCenterKey, build.getConfiguration().getEffectiveTimeSnomedFormat(), request.nextCycleEffectiveTime().replace("-", ""), true);
        releaseService.startNewAuthoringCycleV2(releaseCenterKey, request.dailyBuildProductKey(), build, request.nextCycleEffectiveTime(), publishedReleaseFileName);
    }

    private String getReleaseFileName(String releaseCenterKey, String productKey, String buildKey) throws BusinessServiceException {
        List<String> fileNames = buildService.getOutputFilePaths(releaseCenterKey, productKey, buildKey);
        for (String fileName : fileNames) {
            if (fileName.endsWith(RF2Constants.ZIP_FILE_EXTENSION)) {
                return fileName;
            }
        }
        throw new BusinessServiceException("Could not find the release package file for build " + buildKey);
    }
}

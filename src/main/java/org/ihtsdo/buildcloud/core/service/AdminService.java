package org.ihtsdo.buildcloud.core.service;

import org.ihtsdo.buildcloud.rest.pojo.BrowserUpdateJob;
import org.ihtsdo.buildcloud.rest.pojo.BrowserUpdateRequest;
import org.ihtsdo.buildcloud.rest.pojo.PostReleaseRequest;
import org.ihtsdo.otf.rest.client.RestClientException;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.springframework.security.core.context.SecurityContext;

import java.io.IOException;

public interface AdminService {

    BrowserUpdateJob getBrowserUpdateJob(String jobId);

    void triggerBrowserUpdate(String releaseCenterKey, BrowserUpdateRequest request, SecurityContext securityContext, String browserUpdateId);

    void runPostReleaseTask(String releaseCenterKey, PostReleaseRequest request) throws BusinessServiceException, RestClientException, IOException;
}

package org.ihtsdo.buildcloud.core.service;

import org.ihtsdo.buildcloud.rest.pojo.PostReleaseRequest;
import org.ihtsdo.otf.rest.client.RestClientException;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;

import java.io.IOException;

public interface AdminService {
    void runPostReleaseTask(String releaseCenterKey, PostReleaseRequest request) throws BusinessServiceException, RestClientException, IOException;
}

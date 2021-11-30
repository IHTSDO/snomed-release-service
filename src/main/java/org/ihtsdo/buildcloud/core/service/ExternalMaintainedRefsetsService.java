package org.ihtsdo.buildcloud.core.service;

import org.ihtsdo.otf.rest.exception.BusinessServiceException;

import java.io.IOException;

public interface ExternalMaintainedRefsetsService {

    void copyExternallyMaintainedFiles(String releaseCenterKey, String source, String target, boolean isHeaderOnly) throws BusinessServiceException, IOException;

}

package org.ihtsdo.buildcloud.service;

import org.ihtsdo.otf.rest.client.terminologyserver.SnowOwlRestClient;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.otf.rest.exception.ProcessWorkflowException;

import java.io.File;
import java.io.IOException;
import java.util.Set;

public interface TermServerService {

    /*File export(String snowowlUrl, String branchPath, String startEffectiveDate, String endEffectiveDate, String effectiveDate, Set<String> moduleIds, SnowOwlRestClient.ExportCategory exportCategory,
                SnowOwlRestClient.ExportType exportType, String namespaceId, Boolean includeUnpublished, String codeSystemShortName) throws BusinessServiceException, FileNotFoundException;*/

    File export(boolean useSnowOwl, String termServer, String branchPath, String effectiveDate, Set<String> excludedModuleId, SnowOwlRestClient.ExportCategory exportCategory) throws BusinessServiceException, IOException, ProcessWorkflowException;

}

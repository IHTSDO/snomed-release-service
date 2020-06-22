package org.ihtsdo.buildcloud.service;

import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.otf.rest.exception.ProcessWorkflowException;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import static org.ihtsdo.otf.rest.client.terminologyserver.SnowstormRestClient.*;

public interface TermServerService {
	File export(String termServer, String branchPath, String effectiveDate, Set<String> excludedModuleId, ExportCategory exportCategory) throws BusinessServiceException, IOException, ProcessWorkflowException;

}

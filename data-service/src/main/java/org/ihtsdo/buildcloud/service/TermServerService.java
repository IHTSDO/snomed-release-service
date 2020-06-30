package org.ihtsdo.buildcloud.service;

import org.ihtsdo.otf.rest.exception.BusinessServiceException;

import java.io.File;
import java.util.Set;

import static org.ihtsdo.otf.rest.client.terminologyserver.SnowstormRestClient.*;

public interface TermServerService {
	File export(String termServerUrl, String branchPath, String effectiveDate, Set<String> excludedModuleIds, ExportCategory exportCategory) throws BusinessServiceException;

}

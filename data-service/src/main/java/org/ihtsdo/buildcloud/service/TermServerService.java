package org.ihtsdo.buildcloud.service;

import org.ihtsdo.otf.rest.client.snowowl.SnowOwlRestClient;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Set;

public interface TermServerService {

    File export(String branchPath, String effectiveDate, Set<String> moduleIds, SnowOwlRestClient.ExportCategory exportCategory,
                SnowOwlRestClient.ExportType exportType) throws BusinessServiceException, FileNotFoundException;

}

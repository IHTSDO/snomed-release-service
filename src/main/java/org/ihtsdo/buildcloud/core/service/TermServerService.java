package org.ihtsdo.buildcloud.core.service;

import org.ihtsdo.otf.rest.client.RestClientException;
import org.ihtsdo.otf.rest.client.terminologyserver.pojo.Branch;
import org.ihtsdo.otf.rest.client.terminologyserver.pojo.CodeSystem;
import org.ihtsdo.otf.rest.client.terminologyserver.pojo.CodeSystemVersion;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;

import java.io.File;
import java.util.List;
import java.util.Set;

import static org.ihtsdo.otf.rest.client.terminologyserver.SnowstormRestClient.ExportCategory;

public interface TermServerService {
	File export(String branchPath, String effectiveDate, Set<String> excludedModuleIds, ExportCategory exportCategory) throws BusinessServiceException;

	List<CodeSystem> getCodeSystems();

	List<CodeSystemVersion> getCodeSystemVersions(String shortName, boolean showFutureVersions, boolean showInternalReleases);

	Branch getBranch(String branchPath) throws RestClientException;

	void updateCodeSystemVersionPackage(String codeSystemShortName, String effectiveDate, String releasePackage) throws RestClientException;
}

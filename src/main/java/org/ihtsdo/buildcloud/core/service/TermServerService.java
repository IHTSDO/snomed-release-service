package org.ihtsdo.buildcloud.core.service;

import org.ihtsdo.otf.rest.client.RestClientException;
import org.ihtsdo.otf.rest.client.terminologyserver.Page;
import org.ihtsdo.otf.rest.client.terminologyserver.pojo.*;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.ihtsdo.otf.rest.client.terminologyserver.SnowstormRestClient.ExportCategory;

public interface TermServerService {
	File export(String branchPath, String effectiveDate, Set<String> exportModuleIds, ExportCategory exportCategory) throws BusinessServiceException;

	List<CodeSystem> getCodeSystems();

	List<CodeSystemVersion> getCodeSystemVersions(String shortName, boolean showFutureVersions, boolean showInternalReleases);

	Branch getBranch(String branchPath) throws RestClientException;

	void updateCodeSystemVersionPackage(String codeSystemShortName, String effectiveDate, String releasePackage) throws RestClientException;

	Set<String> getModulesForBranch(String branchPath) throws RestClientException;

	Map<String, ConceptMiniPojo> getRefsetsWithTypeInformation(String branchPath, String module);

	ConceptMiniResponse getConcepts(String memberAnnotationStringRefset, String branchPath, String moduleFilter) throws RestClientException;

	Page<RefsetMember> getRefsetMembers(String refsetId, String branchPath, boolean activeOnly, int limit, String searchAfter);
}

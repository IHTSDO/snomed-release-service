package org.ihtsdo.buildcloud.core.service.browser.update;

import org.ihtsdo.otf.rest.client.RestClientException;
import org.ihtsdo.otf.rest.client.terminologyserver.pojo.CodeSystem;
import org.ihtsdo.otf.rest.client.terminologyserver.pojo.CodeSystemVersion;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;

import java.io.File;
import java.io.IOException;
import java.util.List;

public interface BrowserUpdateServerService {

    void checkConnection() throws BusinessServiceException;

    CodeSystem getCodeSystem(String shortName);

    List<CodeSystemVersion> getCodeSystemVersions(String codeSystemShortname, boolean showFutureVersions);

    void rollBackDailyBuild(String codeSystemShortName) throws RestClientException;

    void upgradeCodeSystem(String codeSystemShortName, Integer newDependantVersion) throws BusinessServiceException;

    void createAndStartFileImport(String type, String branchPath, File file) throws BusinessServiceException, IOException;

}

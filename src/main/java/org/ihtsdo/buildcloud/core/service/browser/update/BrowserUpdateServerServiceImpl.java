package org.ihtsdo.buildcloud.core.service.browser.update;

import org.ihtsdo.otf.rest.client.RestClientException;
import org.ihtsdo.otf.rest.client.terminologyserver.pojo.CodeSystem;
import org.ihtsdo.otf.rest.client.terminologyserver.pojo.CodeSystemUpgradeJob;
import org.ihtsdo.otf.rest.client.terminologyserver.pojo.CodeSystemVersion;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.ihtsdo.otf.rest.client.terminologyserver.pojo.CodeSystemUpgradeJob.UpgradeStatus.FAILED;
import static org.ihtsdo.otf.rest.client.terminologyserver.pojo.CodeSystemUpgradeJob.UpgradeStatus.RUNNING;

@Service
public class BrowserUpdateServerServiceImpl implements BrowserUpdateServerService {

    private static final Logger logger = LoggerFactory.getLogger(BrowserUpdateServerServiceImpl.class);

    @Autowired
    private BrowserUpdateClientFactory browserUpdateClientFactory;

    @Override
    public void checkConnection() throws BusinessServiceException {
        BrowserUpdateClient client = browserUpdateClientFactory.getClient();
        client.checkVersion();
    }

    @Override
    public CodeSystem getCodeSystem(String shortName) {
        BrowserUpdateClient client = browserUpdateClientFactory.getClient();
        return client.getCodeSystem(shortName);
    }

    @Override
    public List<CodeSystemVersion> getCodeSystemVersions(String codeSystemShortname, boolean showFutureVersions) {
        BrowserUpdateClient client = browserUpdateClientFactory.getClient();
        return client.getCodeSystemVersions(codeSystemShortname, showFutureVersions);
    }


    @Override
    public void rollBackDailyBuild(String codeSystemShortName) throws RestClientException {
        logger.info("Starting rollback daily build for {}", codeSystemShortName);
        BrowserUpdateClient client = browserUpdateClientFactory.getClient();
        client.rollBackDailyBuild(codeSystemShortName);
        logger.info("Finished rollback daily build for {}", codeSystemShortName);
    }

    @Override
    public void upgradeCodeSystem(String codeSystemShortName, Integer newDependantVersion) throws BusinessServiceException {
        logger.info("Upgrading code system {} to new INT version {}", codeSystemShortName, newDependantVersion);
        BrowserUpdateClient client = browserUpdateClientFactory.getClient();
        String location = client.upgradeCodeSystem(codeSystemShortName, newDependantVersion);
        String upgradeId = location.substring(location.lastIndexOf("/") + 1);
        waitForCodeSystemUpgradeToComplete(upgradeId);
        logger.info("Finished upgrading code system {} to new INT version {}", codeSystemShortName, newDependantVersion);
    }

    @Override
    public void createAndStartFileImport(String type, String branchPath, File file) throws BusinessServiceException, IOException {
        if (file == null) throw new BusinessServiceException("Import file must not be null");
        BrowserUpdateClient client = browserUpdateClientFactory.getClient();
        String importId = client.createImportJob(type, branchPath);
        logger.info("Starting the import with ID: {}", importId);
        client.uploadImportRf2Archive(importId, file);
        waitForCodeSystemImportToComplete(importId);
        logger.info("Finished the import with ID: {}", importId);
    }

    private void waitForCodeSystemUpgradeToComplete(String jobId) throws BusinessServiceException {
        BrowserUpdateClient client = browserUpdateClientFactory.getClient();
        CodeSystemUpgradeJob codeSystemUpgradeJob;
        int sleepSeconds = 5;
        int totalWait = 0;
        int maxTotalWait = 5 * 60 * 60;
        try {
            do {
                Thread.sleep(1000L * sleepSeconds);
                totalWait += sleepSeconds;
                codeSystemUpgradeJob = client.getCodeSystemUpgradeJob(jobId);
            } while (totalWait < maxTotalWait && RUNNING.equals(codeSystemUpgradeJob.getStatus()));

            if (codeSystemUpgradeJob != null && FAILED.equals(codeSystemUpgradeJob.getStatus())) {
                throw new BusinessServiceException("Code system failed to upgrade. Error message: " + codeSystemUpgradeJob.getErrorMessage());
            }
        } catch (InterruptedException e) {
            logger.error("Failed to fetch code system upgrade status.", e);
            Thread.currentThread().interrupt();
        }
    }

    private void waitForCodeSystemImportToComplete(String jobId) throws BusinessServiceException {
        BrowserUpdateClient client = browserUpdateClientFactory.getClient();
        ImportJob importJob;
        int sleepSeconds = 5;
        int totalWait = 0;
        int maxTotalWait = 5 * 60 * 60;
        try {
            do {
                Thread.sleep(1000L * sleepSeconds);
                totalWait += sleepSeconds;
                importJob = client.getImportJob(jobId);
            } while (totalWait < maxTotalWait && ImportJob.ImportStatus.RUNNING.equals(importJob.getStatus()));

            if (importJob != null && ImportJob.ImportStatus.FAILED.equals(importJob.getStatus())) {
                throw new BusinessServiceException("Code system failed to import. Error message: " + importJob.getErrorMessage());
            }
        } catch (InterruptedException e) {
            logger.error("Failed to fetch code system import status.", e);
            Thread.currentThread().interrupt();
        }
    }
}

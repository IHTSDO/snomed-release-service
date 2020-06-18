package org.ihtsdo.buildcloud.service;

import com.google.common.io.Files;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.ihtsdo.otf.constants.Concepts;
import org.ihtsdo.otf.rest.client.RestClientException;
import org.ihtsdo.otf.rest.client.terminologyserver.SnowOwlRestClient;
import org.ihtsdo.otf.rest.client.terminologyserver.SnowOwlRestClientFactory;
import org.ihtsdo.otf.rest.client.terminologyserver.pojo.Branch;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.otf.rest.exception.ProcessWorkflowException;
import org.ihtsdo.otf.utils.DateUtils;
import org.ihtsdo.otf.utils.ZipFileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

@Service
public class TermServerServiceImpl implements TermServerService{

    @Value("${snowowl.reasonerId}")
    private String reasonerId;

    @Value("${snowowl.path}")
    private String snowowlPath;

    private static final Logger logger = LoggerFactory.getLogger(TermServerService.class);

    private static final String DELTA = "Delta";
    private static final String SNAPSHOT = "Snapshot";


    @Override
    public File export(String termServerUrl, String branchPath, String effectiveDate, Set<String> excludedModuleId, SnowOwlRestClient.ExportCategory exportCategory) throws BusinessServiceException, IOException, ProcessWorkflowException {
        String snowOwlUrl = termServerUrl + snowowlPath;
        SnowOwlRestClientFactory clientFactory = new SnowOwlRestClientFactory(snowOwlUrl, reasonerId, true);
        SnowOwlRestClient snowOwlRestClient = clientFactory.getClient();
        SnowOwlRestClient.ExportType exportType = SnowOwlRestClient.ExportType.DELTA;
        Set<String> moduleList = buildModulesList(snowOwlRestClient, branchPath, excludedModuleId);
        ExportConfigurationExtensionBuilder configurationExtensionBuilder = buildExportConfiguration(branchPath,effectiveDate,moduleList,
                exportCategory, exportType,snowOwlRestClient);
        File export = snowOwlRestClient.export(configurationExtensionBuilder);
        try {
            ZipFile zipFile = new ZipFile(export);
            File extractDir = Files.createTempDir();
            unzipFlat(export, extractDir);
            renameFiles(extractDir, SNAPSHOT, DELTA);
            File tempDir = Files.createTempDir();
            File newZipFile = new File(tempDir,"term-server.zip");
            ZipFileUtils.zip(extractDir.getAbsolutePath(), newZipFile.getAbsolutePath());
            return newZipFile;
        } catch (IOException e) {
            String returnedError = org.apache.commons.io.FileUtils.readFileToString(export);
            logger.error("Failed export data from term server. Term server returned error: {}", returnedError);
            throw new BusinessServiceException("Failed export data from term server. Term server returned error:" + returnedError);
        }

    }


    public void unzipFlat(File archive, File targetDir) throws ProcessWorkflowException, IOException {

        if (!targetDir.exists() || !targetDir.isDirectory()) {
            throw new ProcessWorkflowException(targetDir + " is not a viable directory in which to extract archive");
        }

        ZipInputStream zis = new ZipInputStream(new FileInputStream(archive));
        ZipEntry ze = zis.getNextEntry();
        try {
            while (ze != null) {
                if (!ze.isDirectory()) {
                    Path p = Paths.get(ze.getName());
                    String extractedFileName = p.getFileName().toString();
                    File extractedFile = new File(targetDir, extractedFileName);
                    OutputStream out = new FileOutputStream(extractedFile);
                    IOUtils.copy(zis, out);
                    IOUtils.closeQuietly(out);
                }
                ze = zis.getNextEntry();
            }
        } finally {
            zis.closeEntry();
            zis.close();
        }
    }

    private void renameFiles(File targetDirectory, String find, String replace) {
        Assert.isTrue(targetDirectory.isDirectory(), targetDirectory.getAbsolutePath()
                + " must be a directory in order to rename files from " + find + " to " + replace);
        for (File thisFile : targetDirectory.listFiles()) {
            renameFile(targetDirectory, thisFile, find, replace);
        }
    }

    private void renameFile(File parentDir, File thisFile, String find, String replace) {
        if (thisFile.exists() && !thisFile.isDirectory()) {
            String currentName = thisFile.getName();
            String newName = currentName.replace(find, replace);
            if (!newName.equals(currentName)) {
                File newFile = new File(parentDir, newName);
                thisFile.renameTo(newFile);
            }
        }
    }



    private ExportConfigurationExtensionBuilder buildExportConfiguration(String branchPath, String effectiveDate, Set<String> moduleList, SnowOwlRestClient.ExportCategory exportCategory,
                                                                         SnowOwlRestClient.ExportType exportType, SnowOwlRestClient snowOwlRestClient) throws BusinessServiceException {
        ExportConfigurationExtensionBuilder exportConfig = new ExportConfigurationExtensionBuilder();
        exportConfig.setBranchPath(branchPath);
        exportConfig.setType(exportType);
        exportConfig.setIncludeUnpublished(false);
        if(moduleList != null) {
            exportConfig.addModuleIds(moduleList);
        }
        try {
            Branch branch = snowOwlRestClient.getBranch(branchPath);
            if(branch == null) {
                logger.error("Failed to get branch {}", branchPath);
                throw new BusinessServiceException("Failed to get branch " + branchPath);
            }
            Object codeSystemShortNameObj = branch.getMetadata().get("codeSystemShortName");
            if(codeSystemShortNameObj != null && StringUtils.isNotBlank((String) codeSystemShortNameObj)) {
                exportConfig.setCodeSystemShortName((String) codeSystemShortNameObj);
            }
        } catch (RestClientException e) {
            logger.error("Failed to get branch {}: {}", branchPath, e);
            throw new BusinessServiceException("Failed to get branch " + branchPath);
        }
        switch (exportCategory) {
            case UNPUBLISHED:
                String tet = (effectiveDate == null) ? DateUtils.now(DateUtils.YYYYMMDD) : effectiveDate;
                exportConfig.setTransientEffectiveTime(tet);
                exportConfig.setType(SnowOwlRestClient.ExportType.DELTA);
                break;
            case PUBLISHED:
                if(effectiveDate == null) {
                    throw new BusinessServiceException("Cannot export published data without an effective date");
                }
                exportConfig.setStartEffectiveTime(effectiveDate);
                exportConfig.setTransientEffectiveTime(effectiveDate);
                exportConfig.setType(SnowOwlRestClient.ExportType.SNAPSHOT);
                break;
            case FEEDBACK_FIX:
                if(effectiveDate == null) {
                    throw new BusinessServiceException("Cannot export feedback-fix data without an effective date");
                }
                exportConfig.setStartEffectiveTime(effectiveDate);
                exportConfig.setIncludeUnpublished(true);
                exportConfig.setTransientEffectiveTime(effectiveDate);
                exportConfig.setType(SnowOwlRestClient.ExportType.SNAPSHOT);
                break;
                default:
                    logger.error("Export category {} not recognized", exportCategory);
                    throw new BusinessServiceException("Export type " + exportCategory + " not recognized");
        }

        return exportConfig;
    }

    private Set<String> buildModulesList(SnowOwlRestClient snowOwlRestClient, String branchPath, Set<String> excludedModuleIds) throws BusinessServiceException {
        // If any modules are excluded build a list of modules to include
        Set<String> exportModuleIds = null;
        if (excludedModuleIds != null && !excludedModuleIds.isEmpty()) {
            try {
                Set<String> allModules = snowOwlRestClient.eclQuery(branchPath, "<<" + Concepts.MODULE, 1000);
                allModules.removeAll(excludedModuleIds);
                exportModuleIds = new HashSet<>();
                exportModuleIds.addAll(allModules);
                logger.info("Excluded modules are {}, included modules are {} for release on {}", excludedModuleIds, exportModuleIds, branchPath);
            } catch (RestClientException e) {
                throw new BusinessServiceException("Failed to build list of modules for export.", e);
            }
        }
        return exportModuleIds;
    }

    public static class ExportConfigurationExtensionBuilder extends SnowOwlRestClient.ExportConfigurationBuilder {

        private String codeSystemShortName;

        public String getCodeSystemShortName() {
            return codeSystemShortName;
        }

        public void setCodeSystemShortName(String codeSystemShortName) {
            this.codeSystemShortName = codeSystemShortName;
        }
    }

}

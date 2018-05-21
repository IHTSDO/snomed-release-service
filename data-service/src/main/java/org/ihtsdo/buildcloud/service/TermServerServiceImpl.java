package org.ihtsdo.buildcloud.service;

import org.apache.commons.lang3.StringUtils;
import org.ihtsdo.otf.constants.Concepts;
import org.ihtsdo.otf.rest.client.RestClientException;
import org.ihtsdo.otf.rest.client.snowowl.SnowOwlRestClient;
import org.ihtsdo.otf.rest.client.snowowl.SnowOwlRestClientFactory;
import org.ihtsdo.otf.rest.client.snowowl.pojo.Branch;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.otf.rest.exception.ProcessWorkflowException;
import org.ihtsdo.otf.utils.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class TermServerServiceImpl implements TermServerService{

    @Value("${snowowl.flatIndexExportStyle}")
    private Boolean exportFlatType;

    @Value("${snowowl.reasonerId}")
    private String reasonerId;

    @Value("${snowowl.path}")
    private String snowowlPath;

    private static final Logger logger = LoggerFactory.getLogger(TermServerService.class);


   /* public File export(String snowowlUrl, String branchPath, String startEffectiveDate, String endEffectiveDate, String effectiveDate, Set<String> excludedModuleIds, SnowOwlRestClient.ExportCategory exportCategory,
                       SnowOwlRestClient.ExportType exportType, String namespaceId, Boolean includeUnpublished, String codeSystemShortName) throws BusinessServiceException, FileNotFoundException {
        String snowOwlUrl = snowowlUrl + snowowlPath;
        SnowOwlRestClientFactory clientFactory = new SnowOwlRestClientFactory(snowOwlUrl, reasonerId);
        SnowOwlRestClient snowOwlRestClient =  clientFactory.getClient();
        snowOwlRestClient.setFlatIndexExportStyle(exportFlatType != null ? exportFlatType : true);
        ExportConfigurationExtensionBuilder configurationBuilder = new ExportConfigurationExtensionBuilder();
        configurationBuilder.setStartEffectiveTime(StringUtils.isNotBlank(startEffectiveDate) ? startEffectiveDate : effectiveDate);
        configurationBuilder.setEndEffectiveTime(StringUtils.isNotBlank(endEffectiveDate) ? endEffectiveDate : endEffectiveDate);
        configurationBuilder.setTransientEffectiveTime(effectiveDate);
        configurationBuilder.setBranchPath(branchPath);
        configurationBuilder.setType(exportType);
        configurationBuilder.setNamespaceId(namespaceId);
        configurationBuilder.setIncludeUnpublished(includeUnpublished != null ? includeUnpublished : false);
        if(StringUtils.isNotBlank(codeSystemShortName)) configurationBuilder.setCodeSystemShortName(codeSystemShortName);
        if(excludedModuleIds != null) {
            configurationBuilder.addModuleIds(excludedModuleIds);
        }
        return snowOwlRestClient.export(configurationBuilder);
    }*/
    

    @Override
    public File export(String termServerUrl, String branchPath, String effectiveDate, Set<String> excludedModuleId, SnowOwlRestClient.ExportCategory exportCategory) throws BusinessServiceException {
        String snowOwlUrl = termServerUrl + snowowlPath;
        SnowOwlRestClientFactory clientFactory = new SnowOwlRestClientFactory(snowOwlUrl, reasonerId);
        SnowOwlRestClient snowOwlRestClient = clientFactory.getClient();
        SnowOwlRestClient.ExportType exportType = exportFlatType ? SnowOwlRestClient.ExportType.SNAPSHOT : SnowOwlRestClient.ExportType.DELTA;
        Set<String> moduleList = buildModulesList(snowOwlRestClient, branchPath, excludedModuleId);
        ExportConfigurationExtensionBuilder configurationExtensionBuilder = buildExportConfiguration(branchPath,effectiveDate,moduleList,
                exportCategory, exportType,snowOwlRestClient);
        return snowOwlRestClient.export(configurationExtensionBuilder);

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
                break;
            case PUBLISHED:
                if(effectiveDate == null) {
                    throw new BusinessServiceException("Cannot export published data without an effective date");
                }
                exportConfig.setStartEffectiveTime(effectiveDate);
                exportConfig.setTransientEffectiveTime(effectiveDate);
                exportConfig.setEndEffectiveTime(effectiveDate);
                break;
            case FEEDBACK_FIX:
                if(effectiveDate == null) {
                    throw new BusinessServiceException("Cannot export feedback-fix data without an effective date");
                }
                exportConfig.setStartEffectiveTime(effectiveDate);
                exportConfig.setIncludeUnpublished(true);
                exportConfig.setTransientEffectiveTime(effectiveDate);
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

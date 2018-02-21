package org.ihtsdo.buildcloud.service;

import org.apache.commons.lang3.StringUtils;
import org.ihtsdo.otf.constants.Concepts;
import org.ihtsdo.otf.rest.client.RestClientException;
import org.ihtsdo.otf.rest.client.snowowl.SnowOwlRestClient;
import org.ihtsdo.otf.rest.client.snowowl.SnowOwlRestClientFactory;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.otf.rest.exception.ProcessWorkflowException;
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
        //Set<String> moduleList = buildModulesList(snowOwlRestClient, branchPath, excludedModuleId);
        Set<String> moduleList = new HashSet<>();
        return snowOwlRestClient.export(branchPath, effectiveDate, moduleList, exportCategory, exportType);
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

package org.ihtsdo.buildcloud.service;

import org.ihtsdo.otf.rest.client.snowowl.SnowOwlRestClient;
import org.ihtsdo.otf.rest.client.snowowl.SnowOwlRestClientFactory;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Set;

@Service
public class TermServerServiceImpl implements TermServerService{

    @Autowired
    private SnowOwlRestClientFactory snowOwlRestClientFactory;

    @Value("${snowowl.flatIndexExportStyle}")
    private Boolean exportFlatType;


    public File export(String branchPath, String startEffectiveDate, String endEffectiveDate, String effectiveDate, Set<String> moduleIds, SnowOwlRestClient.ExportCategory exportCategory,
                       SnowOwlRestClient.ExportType exportType, String namespaceId, Boolean includeUnpublished, String codeSystemShortName ) throws BusinessServiceException, FileNotFoundException {
        SnowOwlRestClient snowOwlRestClient =  snowOwlRestClientFactory.getClient();
        snowOwlRestClient.setFlatIndexExportStyle(exportFlatType != null ? exportFlatType : true);
        ExportConfigurationExtensionBuilder configurationBuilder = new ExportConfigurationExtensionBuilder();
        configurationBuilder.setStartEffectiveTime(startEffectiveDate);
        configurationBuilder.setEndEffectiveTime(endEffectiveDate);
        configurationBuilder.setTransientEffectiveTime(effectiveDate);
        configurationBuilder.setBranchPath(branchPath);
        configurationBuilder.setType(exportType);
        configurationBuilder.setNamespaceId(namespaceId);
        configurationBuilder.setIncludeUnpublished(includeUnpublished != null ? includeUnpublished : false);
        configurationBuilder.setCodeSystemShortName(codeSystemShortName);
        if(moduleIds != null) {
            configurationBuilder.addModuleIds(moduleIds);
        }
        return snowOwlRestClient.export(configurationBuilder);
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

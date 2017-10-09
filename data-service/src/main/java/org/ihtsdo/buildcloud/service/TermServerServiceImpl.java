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


    public File export(String branchPath, String effectiveDate, Set<String> moduleIds, SnowOwlRestClient.ExportCategory exportCategory,
                       SnowOwlRestClient.ExportType exportType, String namespaceId, Boolean includeUnpublished ) throws BusinessServiceException, FileNotFoundException {
        SnowOwlRestClient snowOwlRestClient =  snowOwlRestClientFactory.getClient();
        /*SnowOwlRestClient snowOwlRestClient = new SnowOwlRestClient("https://dev-ms-authoring.ihtsdotools.org/snowowl"
                , "dev-ims-ihtsdo=76D0z0XJD00Y0QYIwoFYjg00");*/
        snowOwlRestClient.setFlatIndexExportStyle(exportFlatType != null ? exportFlatType : true);
        SnowOwlRestClient.ExportConfigurationBuilder configurationBuilder = new SnowOwlRestClient.ExportConfigurationBuilder();
        configurationBuilder.setStartEffectiveTime(effectiveDate);
        configurationBuilder.setEndEffectiveTime(effectiveDate);
        configurationBuilder.setTransientEffectiveTime(effectiveDate);
        configurationBuilder.setBranchPath(branchPath);
        configurationBuilder.setType(exportType);
        configurationBuilder.setNamespaceId(namespaceId);
        configurationBuilder.setIncludeUnpublished(includeUnpublished != null ? includeUnpublished : true);
        if(moduleIds != null) {
            configurationBuilder.addModuleIds(moduleIds);
        }
        return snowOwlRestClient.export(configurationBuilder);
    }

}

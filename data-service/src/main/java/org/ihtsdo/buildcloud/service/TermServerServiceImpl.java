package org.ihtsdo.buildcloud.service;

import org.ihtsdo.otf.rest.client.snowowl.SnowOwlRestClient;
import org.ihtsdo.otf.rest.client.snowowl.SnowOwlRestClientFactory;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Set;

@Service
public class TermServerServiceImpl implements TermServerService{

    @Autowired
    private SnowOwlRestClientFactory snowOwlRestClientFactory;


    public File export(String branchPath, String effectiveDate, Set<String> moduleIds, SnowOwlRestClient.ExportCategory exportCategory,
                       SnowOwlRestClient.ExportType exportType) throws BusinessServiceException, FileNotFoundException {
        //SnowOwlRestClient snowOwlRestClient = snowOwlRestClientFactory.getClient();
        SnowOwlRestClient snowOwlRestClient = new SnowOwlRestClient("https://dev-ms-authoring.ihtsdotools.org/snowowl","dev-ims-ihtsdo=iQd00pFHmuw7a2qlS08NoA00");
        snowOwlRestClient.setFlatIndexExportStyle(false);
        /*SnowOwlRestClient.ExportConfigurationBuilder exportConfigurationBuilder = new SnowOwlRestClient.ExportConfigurationBuilder();
        exportConfigurationBuilder.setType(exportType).setBranchPath(branchPath)
                .setTransientEffectiveTime(effectiveDate)
                .setStartEffectiveTime(effectiveDate)
                .setEndEffectiveTime(effectiveDate);*/
        return  snowOwlRestClient.export(branchPath, effectiveDate, moduleIds, exportCategory, exportType);
        //return snowOwlRestClient.export(exportConfigurationBuilder);
    }

}

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
    private SnowOwlRestClient snowOwlRestClient;


    public File export(String branchPath, String effectiveDate, Set<String> moduleIds, SnowOwlRestClient.ExportCategory exportCategory,
                       SnowOwlRestClient.ExportType exportType) throws BusinessServiceException, FileNotFoundException {
        return  snowOwlRestClient.export(branchPath, effectiveDate, moduleIds, exportCategory, exportType);
    }

}

package org.ihtsdo.buildcloud.service;

import org.apache.commons.codec.DecoderException;
import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.User;
import org.ihtsdo.buildcloud.service.inputfile.gather.InputGatherReport;
import org.ihtsdo.buildcloud.service.inputfile.prepare.FileProcessingReportDetail;
import org.ihtsdo.buildcloud.service.inputfile.prepare.ReportType;
import org.ihtsdo.buildcloud.service.inputfile.prepare.SourceFileProcessingReport;
import org.ihtsdo.buildcloud.service.security.SecurityHelper;
import org.ihtsdo.buildcloud.service.termserver.GatherInputRequestPojo;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.otf.rest.exception.BusinessServiceRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

@Service
public class ReleaseServiceImpl implements ReleaseService{

    @Autowired
    ProductInputFileService productInputFileService;

    @Autowired
    BuildService buildService;

    @Autowired
    AuthenticationService authenticationService;

    private static final Logger LOGGER = LoggerFactory.getLogger(ReleaseServiceImpl.class);

    
    @Override
    @Async("securityContextAsyncTaskExecutor")
    public void createReleasePackage(String releaseCenter, String productKey, GatherInputRequestPojo gatherInputRequestPojo) throws DecoderException, JAXBException, NoSuchAlgorithmException, BusinessServiceException, IOException {
        try {
            final User anonymousSubject = authenticationService.getAnonymousSubject();
            SecurityHelper.setUser(anonymousSubject);
            //Gather all files in term server and externally maintain buckets if specified to source directories
            InputGatherReport inputGatherReport = productInputFileService.gatherSourceFiles(releaseCenter, productKey, gatherInputRequestPojo);
            if(inputGatherReport.getStatus().equals(InputGatherReport.Status.ERROR)) {
                LOGGER.error("Error occurred when gathering source files: ");
                for (String source : inputGatherReport.getDetails().keySet()) {
                    InputGatherReport.Details details = inputGatherReport.getDetails().get(source);
                    if(InputGatherReport.Status.ERROR.equals(details.getStatus())) {
                        LOGGER.error("Source: {} -> Error Details: {}", source, details.getMessage());
                        throw new BusinessServiceRuntimeException("Failed when gathering source files. Please check input gather report for details");
                    }
                }
            }
            //After gathering all sources, start to transform and put them into input directories
            SourceFileProcessingReport sourceFileProcessingReport = productInputFileService.prepareInputFiles(releaseCenter, productKey, true);
            if(sourceFileProcessingReport.getDetails().get(ReportType.ERROR) != null) {
                LOGGER.error("Error occurred when processing input files");
                List<FileProcessingReportDetail> errorDetails = sourceFileProcessingReport.getDetails().get(ReportType.ERROR);
                for (FileProcessingReportDetail errorDetail : errorDetails) {
                    LOGGER.error("File: {} -> Error Details: {}", errorDetail.getFileName(), errorDetail.getMessage());
                    throw new BusinessServiceRuntimeException("Failed when processing source files into input files. Please check input prepare report for details");
                }
            }
            //Create and trigger new build
            Build build = buildService.createBuildFromProduct(releaseCenter, productKey);
            buildService.triggerBuild(releaseCenter, productKey, build.getId(), 10);
        } catch (Exception e) {
            LOGGER.error("Encounter error while creating package. Build process stopped. Details: {}", e.getMessage());
            throw e;
        }

    }
}

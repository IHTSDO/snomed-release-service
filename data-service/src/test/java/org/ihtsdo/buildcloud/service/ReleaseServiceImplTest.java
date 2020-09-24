package org.ihtsdo.buildcloud.service;

import org.ihtsdo.buildcloud.dao.helper.BuildS3PathHelper;
import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.User;
import org.ihtsdo.buildcloud.service.inputfile.gather.InputGatherReport;
import org.ihtsdo.buildcloud.service.inputfile.prepare.FileProcessingReportDetail;
import org.ihtsdo.buildcloud.service.inputfile.prepare.ReportType;
import org.ihtsdo.buildcloud.service.inputfile.prepare.SourceFileProcessingReport;
import org.ihtsdo.buildcloud.service.security.SecurityHelper;
import org.ihtsdo.buildcloud.service.termserver.GatherInputRequestPojo;
import org.ihtsdo.buildcloud.test.TestUtils;
import org.ihtsdo.otf.dao.s3.S3Client;
import org.ihtsdo.otf.rest.exception.BadRequestException;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.otf.rest.exception.BusinessServiceRuntimeException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ReleaseServiceImplTest {

    @Mock
    ProductInputFileService productInputFileService;

    @Mock
    BuildService buildService;

    @Mock
    AuthenticationService authenticationService;

    @Mock
    S3Client s3Client;

    @Mock
    BuildS3PathHelper s3PathHelper;

    @Mock
    ProductService productService;

    @Spy
    @InjectMocks
    ReleaseServiceImpl releaseService;


    @Before
    public void setup() {
        SecurityHelper.setUser(TestUtils.TEST_USER);
        MockitoAnnotations.initMocks(this);
    }

    @Test(expected = BadRequestException.class)
    public void testSourceFileGatheringError() throws BusinessServiceException, IOException {
        InputGatherReport inputGatherReport = new InputGatherReport();
        inputGatherReport.setStatus(InputGatherReport.Status.ERROR);
        inputGatherReport.addDetails(InputGatherReport.Status.ERROR, "terminology-server","Failed export data from term server");
        when(productInputFileService.gatherSourceFiles(Matchers.anyString(), Matchers.anyString(), any(GatherInputRequestPojo.class), any(SecurityContext.class))).thenReturn(inputGatherReport);
        Build build = releaseService.createBuild("center", "product", new GatherInputRequestPojo(), User.ANONYMOUS_USER);
        releaseService.triggerBuildAsync("center", "product", build, new GatherInputRequestPojo(), SecurityContextHolder.getContext().getAuthentication());
    }

    @Test(expected = BadRequestException.class)
    public void testInputPrepareError() throws BusinessServiceException, IOException {
        when(productInputFileService.gatherSourceFiles(Matchers.anyString(), Matchers.anyString(), any(GatherInputRequestPojo.class), any(SecurityContext.class))).thenReturn(new InputGatherReport());
        SourceFileProcessingReport sourceFileProcessingReport = new SourceFileProcessingReport();
        FileProcessingReportDetail fileProcessingReportDetail = new FileProcessingReportDetail();
        fileProcessingReportDetail.setFileName("sct2_Identifier_Delta_DK1000005_20170930.txt");
        fileProcessingReportDetail.setMessage("Required by manifest but not found in source [externally-maintained]");
        fileProcessingReportDetail.setType(ReportType.ERROR);
        sourceFileProcessingReport.addReportDetail(fileProcessingReportDetail);
        when(productInputFileService.prepareInputFiles(Matchers.anyString(), Matchers.anyString(), Matchers.anyBoolean())).thenReturn(sourceFileProcessingReport);
        Build build = releaseService.createBuild("center", "product", new GatherInputRequestPojo(), User.ANONYMOUS_USER);
        releaseService.triggerBuildAsync("center", "product", build, new GatherInputRequestPojo(), SecurityContextHolder.getContext().getAuthentication());
    }



}

package org.ihtsdo.buildcloud.service;

import org.apache.commons.codec.DecoderException;
import org.ihtsdo.buildcloud.service.inputfile.gather.InputGatherReport;
import org.ihtsdo.buildcloud.service.inputfile.prepare.FileProcessingReportDetail;
import org.ihtsdo.buildcloud.service.inputfile.prepare.ReportType;
import org.ihtsdo.buildcloud.service.inputfile.prepare.SourceFileProcessingReport;
import org.ihtsdo.buildcloud.service.security.SecurityHelper;
import org.ihtsdo.buildcloud.service.termserver.GatherInputRequestPojo;
import org.ihtsdo.buildcloud.test.TestUtils;
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

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ReleaseServiceImplTest {

    @Mock
    ProductInputFileService productInputFileService;

    @Mock
    BuildService buildService;

    @Mock
    AuthenticationService authenticationService;

    @Spy
    @InjectMocks
    ReleaseServiceImpl releaseService;


    @Before
    public void setup() {
        SecurityHelper.setUser(TestUtils.TEST_USER);
        MockitoAnnotations.initMocks(this);
    }

    @Test(expected = BusinessServiceRuntimeException.class)
    public void testSourceFileGatheringError() throws BusinessServiceException, IOException, NoSuchAlgorithmException, JAXBException, DecoderException {
        InputGatherReport inputGatherReport = new InputGatherReport();
        inputGatherReport.setStatus(InputGatherReport.Status.ERROR);
        inputGatherReport.addDetails(InputGatherReport.Status.ERROR, "terminology-server","Failed export data from term server");
        when(productInputFileService.gatherSourceFiles(Matchers.anyString(), Matchers.anyString(), Matchers.any(GatherInputRequestPojo.class))).thenReturn(inputGatherReport);
        releaseService.createReleasePackage("center", "product", new GatherInputRequestPojo());
    }

    @Test(expected = BusinessServiceRuntimeException.class)
    public void testInputPrepareError() throws BusinessServiceException, IOException, NoSuchAlgorithmException, JAXBException, DecoderException {
        when(productInputFileService.gatherSourceFiles(Matchers.anyString(), Matchers.anyString(), Matchers.any(GatherInputRequestPojo.class))).thenReturn(new InputGatherReport());
        SourceFileProcessingReport sourceFileProcessingReport = new SourceFileProcessingReport();
        FileProcessingReportDetail fileProcessingReportDetail = new FileProcessingReportDetail();
        fileProcessingReportDetail.setFileName("sct2_Identifier_Delta_DK1000005_20170930.txt");
        fileProcessingReportDetail.setMessage("Required by manifest but not found in source [externally-maintained]");
        fileProcessingReportDetail.setType(ReportType.ERROR);
        sourceFileProcessingReport.addReportDetail(fileProcessingReportDetail);
        when(productInputFileService.prepareInputFiles(Matchers.anyString(), Matchers.anyString(), Matchers.anyBoolean())).thenReturn(sourceFileProcessingReport);
        releaseService.createReleasePackage("center", "product", new GatherInputRequestPojo());
    }



}

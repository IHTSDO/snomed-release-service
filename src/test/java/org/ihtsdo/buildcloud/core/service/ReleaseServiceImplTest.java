package org.ihtsdo.buildcloud.core.service;

import org.ihtsdo.buildcloud.core.dao.BuildStatusTrackerDao;
import org.ihtsdo.buildcloud.core.entity.Build;
import org.ihtsdo.buildcloud.core.entity.Product;
import org.ihtsdo.buildcloud.core.entity.ReleaseCenter;
import org.ihtsdo.buildcloud.core.service.inputfile.gather.InputGatherReport;
import org.ihtsdo.buildcloud.core.service.inputfile.prepare.FileProcessingReportDetail;
import org.ihtsdo.buildcloud.core.service.inputfile.prepare.ReportType;
import org.ihtsdo.buildcloud.core.service.inputfile.prepare.SourceFileProcessingReport;
import org.ihtsdo.buildcloud.core.service.manager.ReleaseBuildManager;
import org.ihtsdo.buildcloud.core.service.inputfile.gather.BuildRequestPojo;
import org.ihtsdo.otf.rest.exception.BadRequestException;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ReleaseServiceImplTest {

	@Mock
	InputFileService inputFileService;

	@Mock
	ProductService productService;

	@Spy
	@InjectMocks
	ReleaseServiceImpl releaseService;

	@Spy
	@InjectMocks
	ReleaseBuildManager releaseBuildManager;


	@Mock
	private BuildStatusTrackerDao buildStatusTrackerDao;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
	}

	@Test(expected = BadRequestException.class)
	public void testSourceFileGatheringError() throws BusinessServiceException, IOException {
		InputGatherReport inputGatherReport = new InputGatherReport();
		inputGatherReport.setStatus(InputGatherReport.Status.ERROR);
		inputGatherReport.addDetails(InputGatherReport.Status.ERROR, "terminology-server","Failed export data from term server");
		when(productService.find(anyString(), anyString(), anyBoolean()))
				.thenReturn(createMockProduct("testProduct"));
		Build build = releaseBuildManager.createBuild("international", "product", new BuildRequestPojo(), null);
		releaseService.runReleaseBuild("center", "testProduct", build, new BuildRequestPojo(), SecurityContextHolder.getContext().getAuthentication());
	}


	@Test(expected = BadRequestException.class)
	public void testInputPrepareError() throws BusinessServiceException, IOException {
		SourceFileProcessingReport sourceFileProcessingReport = new SourceFileProcessingReport();
		FileProcessingReportDetail fileProcessingReportDetail = new FileProcessingReportDetail();
		fileProcessingReportDetail.setFileName("sct2_Identifier_Delta_DK1000005_20170930.txt");
		fileProcessingReportDetail.setMessage("Required by manifest but not found in source [externally-maintained]");
		fileProcessingReportDetail.setType(ReportType.ERROR);
		sourceFileProcessingReport.addReportDetail(fileProcessingReportDetail);
		when(productService.find(anyString(), anyString(), anyBoolean()))
				.thenReturn(createMockProduct("test"));
		Build build = releaseBuildManager.createBuild("International", "product", new BuildRequestPojo(), null);
		releaseService.runReleaseBuild("International", "test", build, new BuildRequestPojo(), SecurityContextHolder.getContext().getAuthentication());
	}

	private Product createMockProduct(String productName) {
		Product testProduct = new Product(productName);
		ReleaseCenter releaseCenter = new ReleaseCenter("International", "int");
		releaseCenter.setCodeSystem("SNOMEDCT");
		testProduct.setReleaseCenter(releaseCenter);
		return testProduct;
	}

}

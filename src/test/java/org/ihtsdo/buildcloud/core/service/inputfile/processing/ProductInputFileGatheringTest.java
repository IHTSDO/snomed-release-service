package org.ihtsdo.buildcloud.core.service.inputfile.processing;

import org.apache.commons.io.IOUtils;
import org.ihtsdo.buildcloud.core.dao.ProductDAO;
import org.ihtsdo.buildcloud.core.dao.InputFileDAO;
import org.ihtsdo.buildcloud.core.entity.Build;
import org.ihtsdo.buildcloud.core.entity.BuildConfiguration;
import org.ihtsdo.buildcloud.core.entity.Product;
import org.ihtsdo.buildcloud.core.service.InputFileServiceImpl;
import org.ihtsdo.buildcloud.core.service.TermServerService;
import org.ihtsdo.buildcloud.core.service.inputfile.gather.InputGatherReport;
import org.ihtsdo.otf.rest.client.terminologyserver.SnowstormRestClient;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * User: huyle
 * Date: 8/27/2017
 * Time: 1:15 PM
 */
@ExtendWith(MockitoExtension.class)
public class ProductInputFileGatheringTest {

	private static final String TERMINOLOGY_SERVER = "terminology-server";
	private static final String INPUT_SOURCE_TEST_DATA_ZIP = "input_source_test_data.zip";
	private static final String FAILED_EXPORT_DATA_ZIP = "failedExport.zip";

	@Mock
	TermServerService termServerService;

	@Mock
	ProductDAO productDAO;

	@Mock
	InputFileDAO inputFileDAO;

	@Spy
	@InjectMocks
	InputFileServiceImpl productInputFileService;

	File testArchive;

	File failedExportArchive;

	@BeforeEach
	public void setup() throws Exception {
		MockitoAnnotations.initMocks(this);
		String testFile = getClass().getResource(INPUT_SOURCE_TEST_DATA_ZIP).getFile();
		String failedExportFile = getClass().getResource(FAILED_EXPORT_DATA_ZIP).getFile();
		testArchive = new File(testFile);
		failedExportArchive = new File(failedExportFile);
		when(productDAO.find(anyString(), anyString())).thenReturn(new Product());
		doNothing().when(productInputFileService).putSourceFile(anyString(), anyString(), anyString(), anyString(), any(InputStream.class), anyString(), anyLong());
	}

	@Test
	@Disabled
	public void testGetTermServerExportSucceeded() throws BusinessServiceException, IOException {
		when(termServerService.export(anyString(), anyString(), anySet(), any(SnowstormRestClient.ExportCategory.class))).thenReturn(testArchive);
		BuildConfiguration buildConfiguration = new BuildConfiguration();
		buildConfiguration.setLoadTermServerData(true);
		buildConfiguration.setLoadExternalRefsetData(false);
		Build build = new Build(new Date(), "centerKey", "productKey", buildConfiguration, null);

		FileInputStream fileInputStream = new FileInputStream(testArchive);
		InputGatherReport inputGatherReport = productInputFileService.gatherSourceFiles(build, SecurityContextHolder.getContext());
		verify(productInputFileService, times(1))
				.putSourceFile(eq(TERMINOLOGY_SERVER), eq("centerkey"), eq("productkey"), eq("buildId"),
						argThat(new InputStreamMatcher(fileInputStream)), eq(INPUT_SOURCE_TEST_DATA_ZIP), eq(testArchive.length()));
		assertEquals(InputGatherReport.Status.COMPLETED, inputGatherReport.getDetails().get(TERMINOLOGY_SERVER).getStatus());
		assertEquals(InputGatherReport.Status.COMPLETED, inputGatherReport.getStatus());
		assertEquals("Successfully export file input_source_test_data.zip from term server and upload to source \"terminology-server\"",
				inputGatherReport.getDetails().get("terminology-server").getMessage());
	}

	@Test
	@Disabled
	public void testGetTermServerExportFailed() throws BusinessServiceException, IOException {
		when(termServerService.export(anyString(), anyString(), anySet(), any(SnowstormRestClient.ExportCategory.class))).thenReturn(failedExportArchive);
		BuildConfiguration buildConfiguration = new BuildConfiguration();
		buildConfiguration.setLoadTermServerData(true);
		Build build = new Build(new Date(), "centerKey", "productKey", buildConfiguration, null);

		InputGatherReport inputGatherReport = productInputFileService.gatherSourceFiles(build, SecurityContextHolder.getContext());
		verify(productInputFileService, times(0)).putSourceFile(anyString(), anyString(), anyString(), anyString(),
				any(InputStream.class), anyString(), anyLong());

		assertEquals(InputGatherReport.Status.ERROR, inputGatherReport.getDetails().get(TERMINOLOGY_SERVER).getStatus());
		assertEquals(InputGatherReport.Status.ERROR, inputGatherReport.getStatus());
		assertEquals("Failed export data from term server. Term server returned error:{\"status\":500,\"code\":0,\"message\":\"Something went wrong during the processing of your request.\"}", inputGatherReport.getDetails().get(TERMINOLOGY_SERVER).getMessage());
	}

	public static class InputStreamMatcher implements ArgumentMatcher<InputStream> {
		private final InputStream expected;

		public InputStreamMatcher(InputStream expected) {
			this.expected = expected;
		}

		@Override
		public boolean matches(InputStream actual) {
			try {
				return IOUtils.toByteArray(actual).length == IOUtils.toByteArray(expected).length;
			} catch (IOException e) {
				e.printStackTrace();
			}
			return false;
		}
	}

}

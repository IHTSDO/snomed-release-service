package org.ihtsdo.buildcloud.service.inputfile.processing;

import org.apache.commons.io.IOUtils;
import org.ihtsdo.buildcloud.dao.ProductDAO;
import org.ihtsdo.buildcloud.dao.ProductInputFileDAO;
import org.ihtsdo.buildcloud.entity.Product;
import org.ihtsdo.buildcloud.entity.User;
import org.ihtsdo.buildcloud.service.ProductInputFileServiceImpl;
import org.ihtsdo.buildcloud.service.TermServerService;
import org.ihtsdo.buildcloud.service.inputfile.gather.InputGatherReport;
import org.ihtsdo.buildcloud.service.security.SecurityHelper;
import org.ihtsdo.buildcloud.service.termserver.TermserverReleaseRequestPojo;
import org.ihtsdo.buildcloud.test.TestUtils;
import org.ihtsdo.otf.rest.client.snowowl.SnowOwlRestClient;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;

import static org.mockito.Mockito.*;

/**
 * User: huyle
 * Date: 8/27/2017
 * Time: 1:15 PM
 */
@RunWith(MockitoJUnitRunner.class)
public class ProductInputFileGatheringTest {

    private static final String TERMINOLOGY_SERVER = "terminology-server";
    public static final String INPUT_SOURCE_TEST_DATA_ZIP = "input_source_test_data.zip";
    @Mock
    TermServerService termServerService;

    @Mock
    ProductDAO productDAO;

    @Mock
    ProductInputFileDAO productInputFileDAO;

    @Spy
    @InjectMocks
    ProductInputFileServiceImpl productInputFileService;

    File testArchive;

    @Before
    public void setup() throws InvocationTargetException, IllegalAccessException, IOException {
        SecurityHelper.setUser(TestUtils.TEST_USER);
        MockitoAnnotations.initMocks(this);
        String testFile = getClass().getResource(INPUT_SOURCE_TEST_DATA_ZIP).getFile();
        testArchive = new File(testFile);
        when(productDAO.find(Matchers.anyString(), Matchers.anyString(), Matchers.any(User.class))).thenReturn(new Product());
        doNothing().when(productInputFileService).putSourceFile(Matchers.anyString(), Matchers.anyString(), Matchers.anyString(), Matchers.any(InputStream.class), Matchers.anyString(), Matchers.anyLong());
    }

    @Test
    public void testGatherInputFiles() throws BusinessServiceException, IOException {
        when(termServerService.export(Matchers.anyString(), Matchers.anyString(), Matchers.anySet(),
                Matchers.any(SnowOwlRestClient.ExportCategory.class), Matchers.any(SnowOwlRestClient.ExportType.class))).thenReturn(testArchive);
        FileInputStream fileInputStream = new FileInputStream(testArchive);
        InputGatherReport inputGatherReport = productInputFileService.gatherSourceFilesFromTermServer
                ("centerkey", "productkey", new TermserverReleaseRequestPojo());
        verify(productInputFileService, times(1))
                .putSourceFile(eq(TERMINOLOGY_SERVER), eq("centerkey"), eq("productkey"),
                        argThat(new InputStreamMatcher(fileInputStream)), eq(INPUT_SOURCE_TEST_DATA_ZIP), eq(testArchive.length()));
        Assert.assertEquals(InputGatherReport.Status.COMPLETED, inputGatherReport.getDetails().get(TERMINOLOGY_SERVER).getStatus());
        Assert.assertEquals(InputGatherReport.Status.COMPLETED, inputGatherReport.getStatus());
        Assert.assertEquals("Successfully export file input_source_test_data.zip from term server and upload to source \"terminology-server\"",
                inputGatherReport.getDetails().get("terminology-server").getMessage());
    }

    @Test
    public void testGatherInputFilesFailed() throws BusinessServiceException, IOException {
        when(termServerService.export(Matchers.anyString(), Matchers.anyString(), Matchers.anySet(),
                Matchers.any(SnowOwlRestClient.ExportCategory.class), Matchers.any(SnowOwlRestClient.ExportType.class)))
                .thenThrow(new BusinessServiceException("Failed to gather files from term server"));
        InputGatherReport inputGatherReport = productInputFileService.gatherSourceFilesFromTermServer
                ("centerkey", "productkey", new TermserverReleaseRequestPojo());
        verify(productInputFileService, times(0)).putSourceFile(Matchers.anyString(), Matchers.anyString(), Matchers.anyString(),
                Matchers.any(InputStream.class), Matchers.anyString(), Matchers.anyLong());
        Assert.assertEquals(InputGatherReport.Status.ERROR, inputGatherReport.getDetails().get(TERMINOLOGY_SERVER).getStatus());
        Assert.assertEquals(InputGatherReport.Status.ERROR, inputGatherReport.getStatus());
        Assert.assertEquals("Failed to gather files from term server", inputGatherReport.getDetails().get(TERMINOLOGY_SERVER).getMessage());
    }

    private class InputStreamMatcher extends ArgumentMatcher<InputStream> {
        private final InputStream expected;

        public InputStreamMatcher(InputStream expected) {
            this.expected = expected;
        }

        @Override
        public boolean matches(Object o) {
            if (!(o instanceof FileInputStream)) {
                return false;
            }
            FileInputStream actual = (FileInputStream) o;
            try {
                return IOUtils.toByteArray(actual).length == IOUtils.toByteArray(expected).length;
            } catch (IOException e) {
                e.printStackTrace();
            }
            return false;
        }
    }

}

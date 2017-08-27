package org.ihtsdo.buildcloud.service.inputfile.processing;

import org.apache.commons.beanutils.BeanUtils;
import org.ihtsdo.buildcloud.dao.ProductDAO;
import org.ihtsdo.buildcloud.dao.ProductDAOImpl;
import org.ihtsdo.buildcloud.dao.ProductInputFileDAO;
import org.ihtsdo.buildcloud.entity.Product;
import org.ihtsdo.buildcloud.entity.User;
import org.ihtsdo.buildcloud.service.ProductInputFileService;
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
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;

/**
 * User: huyle
 * Date: 8/27/2017
 * Time: 1:15 PM
 */
@RunWith(MockitoJUnitRunner.class)
public class ProductInputFileGatheringTest {

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
    public void setup() throws InvocationTargetException, IllegalAccessException {
        SecurityHelper.setUser(TestUtils.TEST_USER);
        MockitoAnnotations.initMocks(this);
        String testFile = getClass().getResource("input_source_test_data.zip").getFile();
        testArchive = new File(testFile);

    }

    @Test
    public void testGatherInputFiles() throws BusinessServiceException, IOException {
        Mockito.when(productDAO.find(Matchers.anyString(), Matchers.anyString(), Matchers.any(User.class))).thenReturn(new Product());
        Mockito.when(termServerService.export(Matchers.anyString(), Matchers.anyString(), Matchers.anySet(),
                Matchers.any(SnowOwlRestClient.ExportCategory.class), Matchers.any(SnowOwlRestClient.ExportType.class))).thenReturn(testArchive);
        Mockito.doNothing().when(productInputFileService).putSourceFile(Matchers.anyString(), Matchers.anyString(), Matchers.anyString(), Matchers.any(InputStream.class), Matchers.anyString(), Matchers.anyLong());
        InputGatherReport inputGatherReport = productInputFileService.gatherSourceFilesFromTermServer
                ("centerkey", "productkey", new TermserverReleaseRequestPojo());
        Assert.assertEquals(InputGatherReport.Status.COMPLETED, inputGatherReport.getDetails().get("terminology-server").getStatus());
        Assert.assertEquals(InputGatherReport.Status.COMPLETED, inputGatherReport.getStatus());
    }

}

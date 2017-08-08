package org.ihtsdo.buildcloud.integration.fileprocessing;

import org.codehaus.jackson.map.ObjectMapper;
import org.ihtsdo.buildcloud.controller.AbstractControllerTest;
import org.ihtsdo.buildcloud.controller.helper.IntegrationTestHelper;
import org.ihtsdo.buildcloud.service.fileprocessing.FileProcessingReport;
import org.ihtsdo.buildcloud.service.fileprocessing.FileProcessingReportDetail;
import org.ihtsdo.buildcloud.service.fileprocessing.FileProcessingReportType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;

/**
 * User: huyle
 * Date: 5/30/2017
 * Time: 3:10 PM
 */
public class FileProcessingIntegrationTest extends AbstractControllerTest {

    private IntegrationTestHelper integrationTestHelper;
    private static final String REFSET_TOOL = "reference-set-tool";
    private static final String MAPPING_TOOLS = "mapping-tools";
    private static final String MANUAL = "manual";
    private static final String MANIFEST_DIR = "manifest/";
    private ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Before
    public void setup() throws Exception {
        super.setup();
        integrationTestHelper = new IntegrationTestHelper(mockMvc, "FileProcessingTest");
        integrationTestHelper.loginAsManager();
        integrationTestHelper.createTestProductStructure();
    }

    @Test
    public void testUploadTextSourceFile() throws Exception {
        integrationTestHelper.uploadSourceFile("der2_cRefset_AlternativeAssociationReferenceSetDelta_INT_20170731.txt", REFSET_TOOL, this.getClass());
        integrationTestHelper.uploadSourceFile("der2_cRefset_ConceptInactivationIndicatorReferenceSetDelta_INT_20170731.txt", MAPPING_TOOLS, this.getClass());
        integrationTestHelper.uploadSourceFile("der2_cRefset_DescriptionInactivationIndicatorReferenceSetDelta_INT_20170731.txt", MANUAL, this.getClass());
        String fileList = integrationTestHelper.listSourceFiles().replace("\r\n", "\n").replace("\r", "\n");
        Assert.assertEquals("[ {\n" +
                "  \"id\" : \"manual/der2_cRefset_DescriptionInactivationIndicatorReferenceSetDelta_INT_20170731.txt\",\n" +
                "  \"url\" : \"http://localhost/centers/international/products/fileprocessingtest_product/sourcefiles/manual/der2_cRefset_DescriptionInactivationIndicatorReferenceSetDelta_INT_20170731.txt\"\n" +
                "}, {\n" +
                "  \"id\" : \"mapping-tools/der2_cRefset_ConceptInactivationIndicatorReferenceSetDelta_INT_20170731.txt\",\n" +
                "  \"url\" : \"http://localhost/centers/international/products/fileprocessingtest_product/sourcefiles/mapping-tools/der2_cRefset_ConceptInactivationIndicatorReferenceSetDelta_INT_20170731.txt\"\n" +
                "}, {\n" +
                "  \"id\" : \"reference-set-tool/der2_cRefset_AlternativeAssociationReferenceSetDelta_INT_20170731.txt\",\n" +
                "  \"url\" : \"http://localhost/centers/international/products/fileprocessingtest_product/sourcefiles/reference-set-tool/der2_cRefset_AlternativeAssociationReferenceSetDelta_INT_20170731.txt\"\n" +
                "} ]", fileList);
        integrationTestHelper.deleteTxtSourceFiles();
    }

    @Test
    public void testUploadZipSourceFile() throws Exception {
        integrationTestHelper.uploadSourceFile("simple.zip", MANUAL, this.getClass());
        String fileList = integrationTestHelper.listSourceFiles().replace("\r\n", "\n").replace("\r", "\n");
        Assert.assertEquals("[ {\n" +
                "  \"id\" : \"manual/der2_Refset_SimpleDelta_INT_20140131.txt\",\n" +
                "  \"url\" : \"http://localhost/centers/international/products/fileprocessingtest_product/sourcefiles/manual/der2_Refset_SimpleDelta_INT_20140131.txt\"\n" +
                "} ]", fileList);
        integrationTestHelper.deleteTxtSourceFiles();
    }

    @Test
    public void testProcessSourceFileNoFile() throws Exception {
        integrationTestHelper.uploadManifest(MANIFEST_DIR + "manifest_all_sources.xml", this.getClass());
        integrationTestHelper.prepareSourceFile();
        String report = integrationTestHelper.getInputPrepareReport();
        FileProcessingReport fileProcessingReport = objectMapper.readValue(report, FileProcessingReport.class);
        Map<String,List<FileProcessingReportDetail>> reportDetails = fileProcessingReport.getDetails();
        Assert.assertEquals(1, reportDetails.size());
        Assert.assertTrue(reportDetails.containsKey(FileProcessingReportType.ERROR.name()));
        List<FileProcessingReportDetail> fileProcessingReportDetails = reportDetails.get(FileProcessingReportType.ERROR.name());
        Assert.assertEquals("Failed to load files from source directory", fileProcessingReportDetails.get(0).getMessage());
    }

    @Test
    public void testProcessSourceFileNoManifest() throws Exception {
        integrationTestHelper.uploadSourceFile("der2_cRefset_AlternativeAssociationReferenceSetDelta_INT_20170731.txt", REFSET_TOOL, this.getClass());
        integrationTestHelper.prepareSourceFile();
        String report = integrationTestHelper.getInputPrepareReport();
        FileProcessingReport fileProcessingReport = objectMapper.readValue(report, FileProcessingReport.class);
        Map<String,List<FileProcessingReportDetail>> reportDetails = fileProcessingReport.getDetails();
        Assert.assertEquals(1, reportDetails.size());
        Assert.assertTrue(reportDetails.containsKey(FileProcessingReportType.ERROR.name()));
        List<FileProcessingReportDetail> fileProcessingReportDetails = reportDetails.get(FileProcessingReportType.ERROR.name());
        Assert.assertEquals("Failed to load manifest", fileProcessingReportDetails.get(0).getMessage());
        integrationTestHelper.deleteTxtSourceFiles();
    }

    @Test
    public void testProcessSourceFile() throws Exception {
        integrationTestHelper.uploadManifest(MANIFEST_DIR + "manifest_all_sources.xml", this.getClass());
        integrationTestHelper.uploadSourceFile("file_processing.zip", MANUAL, this.getClass());
        integrationTestHelper.uploadSourceFile("file_processing.zip", REFSET_TOOL, this.getClass());
        integrationTestHelper.prepareSourceFile();
        String report = integrationTestHelper.getInputPrepareReport();
        FileProcessingReport fileProcessingReport = objectMapper.readValue(report, FileProcessingReport.class);
        Map<String,List<FileProcessingReportDetail>> reportDetails = fileProcessingReport.getDetails();
        Assert.assertEquals(24, reportDetails.get(FileProcessingReportType.INFO.name()).size());
        for (Map.Entry<String,List<FileProcessingReportDetail>>  reportDetail : reportDetails.entrySet()) {
            Assert.assertEquals(FileProcessingReportType.INFO.name(), reportDetail.getKey());
        }
        integrationTestHelper.getInputFile("xder2_cRefset_AssociationReferenceDelta_INT_20170731.txt");
        integrationTestHelper.getInputFile("xder2_cRefset_AttributeValueDelta_INT_20170731.txt");
        integrationTestHelper.getInputFile("xsct2_Description_Delta-en_INT_20170731.txt");
        integrationTestHelper.getInputFile("xsct2_TextDefinition_Delta-en_INT_20170731.txt");
        integrationTestHelper.deleteTxtSourceFiles();
        integrationTestHelper.deletePreviousTxtInputFiles();
    }

    @Test
    public void testProcessSourceFileRefsetNotUsed() throws Exception {
        integrationTestHelper.uploadManifest(MANIFEST_DIR + "manifest_missing_refsets.xml", this.getClass());
        integrationTestHelper.uploadSourceFile("file_processing.zip", MANUAL, this.getClass());
        integrationTestHelper.prepareSourceFile();
        String report = integrationTestHelper.getInputPrepareReport();
        FileProcessingReport fileProcessingReport = objectMapper.readValue(report, FileProcessingReport.class);
        Map<String,List<FileProcessingReportDetail>> reportDetails = fileProcessingReport.getDetails();
        int countWarning = 0;
        /*for (FileProcessingReportDetail reportDetail : reportDetails) {
            if (reportDetail.getType().equals(FileProcessingReportType.WARN)) {
                countWarning++;
            }
        }*/
        if(reportDetails.containsKey(FileProcessingReportType.WARNING.name())){
            countWarning = reportDetails.get(FileProcessingReportType.WARNING.name()).size();
        }
        Assert.assertEquals(3, countWarning);
        integrationTestHelper.deleteTxtSourceFiles();
        integrationTestHelper.deletePreviousTxtInputFiles();
    }

}

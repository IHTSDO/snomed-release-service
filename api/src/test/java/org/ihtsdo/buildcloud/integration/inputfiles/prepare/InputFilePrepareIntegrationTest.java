package org.ihtsdo.buildcloud.integration.inputfiles.prepare;

import org.codehaus.jackson.map.ObjectMapper;
import org.ihtsdo.buildcloud.controller.AbstractControllerTest;
import org.ihtsdo.buildcloud.controller.helper.IntegrationTestHelper;
import org.ihtsdo.buildcloud.service.inputfile.prepare.FileProcessingReportDetail;
import org.ihtsdo.buildcloud.service.inputfile.prepare.SourceFileProcessingReport;
import org.ihtsdo.buildcloud.service.inputfile.prepare.ReportType;
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
public class InputFilePrepareIntegrationTest extends AbstractControllerTest {

    private static final String TEST_DATA = "test_input_files.zip";
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
        SourceFileProcessingReport fileProcessingReport = objectMapper.readValue(report, SourceFileProcessingReport.class);
        Map<ReportType,List<FileProcessingReportDetail>> reportDetails = fileProcessingReport.getDetails();
        Assert.assertEquals(1, reportDetails.size());
        Assert.assertTrue(reportDetails.containsKey(ReportType.ERROR));
        List<FileProcessingReportDetail> fileProcessingReportDetails = reportDetails.get(ReportType.ERROR);
        Assert.assertEquals("Failed to load files from source directory", fileProcessingReportDetails.get(0).getMessage());
    }

    @Test
    public void testProcessSourceFileNoManifest() throws Exception {
        integrationTestHelper.uploadSourceFile("der2_cRefset_AlternativeAssociationReferenceSetDelta_INT_20170731.txt", REFSET_TOOL, this.getClass());
        integrationTestHelper.prepareSourceFile();
        String report = integrationTestHelper.getInputPrepareReport();
        SourceFileProcessingReport fileProcessingReport = objectMapper.readValue(report, SourceFileProcessingReport.class);
        Map<ReportType,List<FileProcessingReportDetail>> reportDetails = fileProcessingReport.getDetails();
        Assert.assertEquals(1, reportDetails.size());
        Assert.assertTrue(reportDetails.containsKey(ReportType.ERROR));
        List<FileProcessingReportDetail> fileProcessingReportDetails = reportDetails.get(ReportType.ERROR);
        Assert.assertEquals("Failed to load manifest", fileProcessingReportDetails.get(0).getMessage());
        integrationTestHelper.deleteTxtSourceFiles();
    }

    @Test
    public void testProcessSourceFile() throws Exception {
        integrationTestHelper.uploadManifest(MANIFEST_DIR + "manifest_all_sources.xml", this.getClass());
        integrationTestHelper.uploadSourceFile(TEST_DATA, MANUAL, this.getClass());
        integrationTestHelper.uploadSourceFile(TEST_DATA, REFSET_TOOL, this.getClass());
        integrationTestHelper.prepareSourceFile();
        String report = integrationTestHelper.getInputPrepareReport();
        SourceFileProcessingReport fileProcessingReport = objectMapper.readValue(report, SourceFileProcessingReport.class);
        Map<ReportType,List<FileProcessingReportDetail>> reportDetails = fileProcessingReport.getDetails();
        Assert.assertEquals(null, reportDetails.get(ReportType.ERROR));
        integrationTestHelper.deleteTxtSourceFiles();
        integrationTestHelper.deletePreviousTxtInputFiles();
    }

    @Test
    public void testProcessSourceFileRefsetNotUsed() throws Exception {
        integrationTestHelper.uploadManifest(MANIFEST_DIR + "manifest_missing_refsets.xml", this.getClass());
        integrationTestHelper.uploadSourceFile(TEST_DATA, MANUAL, this.getClass());
        integrationTestHelper.prepareSourceFile();
        String report = integrationTestHelper.getInputPrepareReport();
        SourceFileProcessingReport fileProcessingReport = objectMapper.readValue(report, SourceFileProcessingReport.class);
        Map<ReportType,List<FileProcessingReportDetail>> reportDetails = fileProcessingReport.getDetails();
        Assert.assertEquals(null, reportDetails.get(ReportType.ERROR));
        integrationTestHelper.deleteTxtSourceFiles();
        integrationTestHelper.deletePreviousTxtInputFiles();
    }

}

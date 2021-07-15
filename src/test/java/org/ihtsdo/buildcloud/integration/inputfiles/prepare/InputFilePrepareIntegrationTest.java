package org.ihtsdo.buildcloud.integration.inputfiles.prepare;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ihtsdo.buildcloud.rest.controller.AbstractControllerTest;
import org.ihtsdo.buildcloud.rest.controller.helper.IntegrationTestHelper;
import org.ihtsdo.buildcloud.core.service.inputfile.prepare.FileProcessingReportDetail;
import org.ihtsdo.buildcloud.core.service.inputfile.prepare.ReportType;
import org.ihtsdo.buildcloud.core.service.inputfile.prepare.SourceFileProcessingReport;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class InputFilePrepareIntegrationTest extends AbstractControllerTest {

	private static final String TEST_DATA = "test_input_files.zip";
	private IntegrationTestHelper integrationTestHelper;
	private static final String REFSET_TOOL = "reference-set-tool";
	private static final String MAPPING_TOOLS = "mapping-tools";
	private static final String MANUAL = "manual";
	private static final String TERMINOLOGY_SERVER = "terminology-server";
	private static final String MANIFEST_DIR = "manifest/";
	private static final String SPLITTING_TEST_FILE = "test_splitting.zip";
	private String buildUrl;
	@Autowired
	private ObjectMapper objectMapper;

	@Override
	@Before
	public void setup() throws Exception {
		super.setup();
		integrationTestHelper = new IntegrationTestHelper(mockMvc, "FileProcessingTest");
		integrationTestHelper.createTestProductStructure();
		integrationTestHelper.setBetaRelease(false);
		integrationTestHelper.setEffectiveTime("20170731");
		integrationTestHelper.setJustPackage(true);
	}

	@Test
	public void testUploadTextSourceFile() throws Exception {
		integrationTestHelper.createBuild();
		buildUrl = integrationTestHelper.getLocalHostBuildUrl();
		integrationTestHelper.uploadSourceFile("der2_cRefset_AlternativeAssociationReferenceSetDelta_INT_20170731.txt", REFSET_TOOL, this.getClass());
		integrationTestHelper.uploadSourceFile("der2_cRefset_ConceptInactivationIndicatorReferenceSetDelta_INT_20170731.txt", MAPPING_TOOLS, this.getClass());
		integrationTestHelper.uploadSourceFile("der2_cRefset_DescriptionInactivationIndicatorReferenceSetDelta_INT_20170731.txt", MANUAL, this.getClass());
		String fileList = integrationTestHelper.listSourceFiles().replace("\r\n", "\n").replace("\r", "\n");
		assertEquals("[ {\n" +
				"  \"id\" : \"manual/der2_cRefset_DescriptionInactivationIndicatorReferenceSetDelta_INT_20170731.txt\",\n" +
				"  \"url\" : \"" + buildUrl + "/sourcefiles/manual/der2_cRefset_DescriptionInactivationIndicatorReferenceSetDelta_INT_20170731.txt\"\n" +
				"}, {\n" +
				"  \"id\" : \"mapping-tools/der2_cRefset_ConceptInactivationIndicatorReferenceSetDelta_INT_20170731.txt\",\n" +
				"  \"url\" : \"" + buildUrl + "/sourcefiles/mapping-tools/der2_cRefset_ConceptInactivationIndicatorReferenceSetDelta_INT_20170731.txt\"\n" +
				"}, {\n" +
				"  \"id\" : \"reference-set-tool/der2_cRefset_AlternativeAssociationReferenceSetDelta_INT_20170731.txt\",\n" +
				"  \"url\" : \"" + buildUrl + "/sourcefiles/reference-set-tool/der2_cRefset_AlternativeAssociationReferenceSetDelta_INT_20170731.txt\"\n" +
				"} ]", fileList);
	}

	@Test
	public void testUploadZipSourceFile() throws Exception {
		integrationTestHelper.createBuild();
		buildUrl = integrationTestHelper.getLocalHostBuildUrl();
		integrationTestHelper.uploadSourceFile("simple.zip", MANUAL, this.getClass());
		String fileList = integrationTestHelper.listSourceFiles().replace("\r\n", "\n").replace("\r", "\n");

		assertEquals("[ {\n" +
				"  \"id\" : \"manual/der2_Refset_SimpleDelta_INT_20140131.txt\",\n" +
				"  \"url\" : \"" + buildUrl + "/sourcefiles/manual/der2_Refset_SimpleDelta_INT_20140131.txt" + "\"\n" +
				"} ]", fileList);
	}

	@Test
	public void testProcessSourceFileNoFile() throws Exception {
		integrationTestHelper.uploadManifest(MANIFEST_DIR + "manifest_all_sources.xml", this.getClass());
		integrationTestHelper.createBuild();
		buildUrl = integrationTestHelper.getLocalHostBuildUrl();

		integrationTestHelper.prepareSourceFile();
		String report = integrationTestHelper.getInputPrepareReport();
		SourceFileProcessingReport fileProcessingReport = objectMapper.readValue(report, SourceFileProcessingReport.class);
		Map<ReportType,List<FileProcessingReportDetail>> reportDetails = fileProcessingReport.getDetails();
		assertEquals(1, reportDetails.size());
		Assert.assertTrue(reportDetails.containsKey(ReportType.ERROR));
		List<FileProcessingReportDetail> fileProcessingReportDetails = reportDetails.get(ReportType.ERROR);
		assertEquals("Failed to find files from source directory", fileProcessingReportDetails.get(0).getMessage());
	}

	@Test
	public void testProcessSourceFileNoManifest() throws Exception {
		integrationTestHelper.createBuild();
		buildUrl = integrationTestHelper.getLocalHostBuildUrl();

		integrationTestHelper.uploadSourceFile("der2_cRefset_AlternativeAssociationReferenceSetDelta_INT_20170731.txt", REFSET_TOOL, this.getClass());
		integrationTestHelper.prepareSourceFile();
		String report = integrationTestHelper.getInputPrepareReport();
		SourceFileProcessingReport fileProcessingReport = objectMapper.readValue(report, SourceFileProcessingReport.class);
		Map<ReportType,List<FileProcessingReportDetail>> reportDetails = fileProcessingReport.getDetails();
		assertEquals(1, reportDetails.size());
		Assert.assertTrue(reportDetails.containsKey(ReportType.ERROR));
		List<FileProcessingReportDetail> fileProcessingReportDetails = reportDetails.get(ReportType.ERROR);
		assertTrue(fileProcessingReportDetails.get(0).getMessage().startsWith("No manifest.xml found for product fileprocessingtest_product and build "));
	}

	@Test
	public void testProcessSourceFile() throws Exception {
		integrationTestHelper.uploadManifest(MANIFEST_DIR + "manifest_all_sources.xml", this.getClass());
		integrationTestHelper.createBuild();
		buildUrl = integrationTestHelper.getLocalHostBuildUrl();

		integrationTestHelper.uploadSourceFile(TEST_DATA, MANUAL, this.getClass());
		integrationTestHelper.uploadSourceFile(TEST_DATA, REFSET_TOOL, this.getClass());
		integrationTestHelper.prepareSourceFile();
		String report = integrationTestHelper.getInputPrepareReport();
		SourceFileProcessingReport fileProcessingReport = objectMapper.readValue(report, SourceFileProcessingReport.class);
		Map<ReportType,List<FileProcessingReportDetail>> reportDetails = fileProcessingReport.getDetails();
		Assert.assertNotNull(reportDetails.get(ReportType.ERROR));
		assertEquals(30, reportDetails.get(ReportType.ERROR).size());
	}

	@Test
	public void testProcessSourceFileRefsetNotUsed() throws Exception {
		integrationTestHelper.uploadManifest(MANIFEST_DIR + "manifest_missing_refsets.xml", this.getClass());
		integrationTestHelper.createBuild();
		buildUrl = integrationTestHelper.getLocalHostBuildUrl();
		
		integrationTestHelper.uploadSourceFile(TEST_DATA, MANUAL, this.getClass());
		integrationTestHelper.prepareSourceFile();
		String report = integrationTestHelper.getInputPrepareReport();
		SourceFileProcessingReport fileProcessingReport = objectMapper.readValue(report, SourceFileProcessingReport.class);
		Map<ReportType,List<FileProcessingReportDetail>> reportDetails = fileProcessingReport.getDetails();
		Assert.assertNotNull(reportDetails.get(ReportType.ERROR));
		assertEquals(1, reportDetails.get(ReportType.ERROR).size());
	}

	@Test
	public void testSplitRefsetFiles() throws Exception {
		integrationTestHelper.uploadManifest(MANIFEST_DIR + "manifest_refset_split.xml", this.getClass());
		integrationTestHelper.createBuild();
		buildUrl = integrationTestHelper.getLocalHostBuildUrl();

		integrationTestHelper.uploadSourceFile(SPLITTING_TEST_FILE, TERMINOLOGY_SERVER, this.getClass());integrationTestHelper.prepareSourceFile();
		String report = integrationTestHelper.getInputPrepareReport();
		SourceFileProcessingReport fileProcessingReport = objectMapper.readValue(report, SourceFileProcessingReport.class);
		Map<ReportType,List<FileProcessingReportDetail>> reportDetails = fileProcessingReport.getDetails();
		FileProcessingReportDetail reportDetailForDa = new FileProcessingReportDetail();
		reportDetailForDa.setType(ReportType.INFO);
		reportDetailForDa.setFileName("der2_cRefset_LanguageDelta-da_DK1000005_20180331.txt");
		reportDetailForDa.setSource(TERMINOLOGY_SERVER);
		reportDetailForDa.setRefsetId("554461000005103");
		reportDetailForDa.setMessage("Added source terminology-server/der2_cRefset_LanguageDelta_INT_20180331.txt");

		Assert.assertTrue(reportDetails.get(ReportType.INFO).contains(reportDetailForDa));

		FileProcessingReportDetail reportDetailForEn = new FileProcessingReportDetail();
		reportDetailForEn.setType(ReportType.INFO);
		reportDetailForEn.setFileName("der2_cRefset_LanguageDelta-en_DK1000005_20180331.txt");
		reportDetailForEn.setSource(TERMINOLOGY_SERVER);
		reportDetailForEn.setRefsetId("900000000000509007");
		reportDetailForEn.setMessage("Added source terminology-server/der2_cRefset_LanguageDelta_INT_20180331.txt");

		Assert.assertTrue(reportDetails.get(ReportType.INFO).contains(reportDetailForEn));

		String actualRefsetData = integrationTestHelper.getInputFile("rel2_cRefset_LanguageDelta-en_DK1000005_20180331.txt");
		String expected = "id	effectiveTime	active	moduleId	refsetId	referencedComponentId	acceptabilityId\r\n" +
				"894e9c94-7fba-4f87-bce5-fd9a8879737c	20180331	1	554471000005108	900000000000509007	4926731000005112	900000000000548007\r\n" +
				"fede5943-7b6e-54e9-ba22-acd675deb7d2	20180331	0	554471000005108	900000000000509007	4852791000005118	900000000000548007\r\n";

		assertEquals(expected, actualRefsetData);

		String daRefsetDataActual = integrationTestHelper.getInputFile("rel2_cRefset_LanguageDelta-da_DK1000005_20180331.txt");
		String expectedDaRefsetData = "id	effectiveTime	active	moduleId	refsetId	referencedComponentId	acceptabilityId\r\n" +
				"17b9020e-c5f2-4fa7-aca5-bf2c3de51e8b	20180331	1	554471000005108	554461000005103	3299811000005110	900000000000548007\r\n" +
				"bd6b1181-ebbc-46ce-95bc-6b02c0cab392	20180331	1	554471000005108	554461000005103	4924031000005119	900000000000548007\r\n" +
				"24dea357-abd4-48d8-b950-75cfd80688d5	20180331	1	554471000005108	554461000005103	4924271000005113	900000000000548007\r\n";
		assertEquals(expectedDaRefsetData, daRefsetDataActual);
	}


	@Test
	public void testSplittingDescriptionByLanguageCode() throws Exception {
		integrationTestHelper.uploadManifest(MANIFEST_DIR + "manifest_description_split.xml", this.getClass());
		integrationTestHelper.createBuild();
		buildUrl = integrationTestHelper.getLocalHostBuildUrl();

		integrationTestHelper.uploadSourceFile(SPLITTING_TEST_FILE, TERMINOLOGY_SERVER, this.getClass());
		integrationTestHelper.prepareSourceFile();
		String report = integrationTestHelper.getInputPrepareReport();
		SourceFileProcessingReport fileProcessingReport = objectMapper.readValue(report, SourceFileProcessingReport.class);
		Map<ReportType,List<FileProcessingReportDetail>> reportDetails = fileProcessingReport.getDetails();
		FileProcessingReportDetail detailWithEn = new FileProcessingReportDetail();
		detailWithEn.setType(ReportType.INFO);
		detailWithEn.setFileName("rel2_Description_Delta-en_DK1000005_20180331.txt");
		detailWithEn.setMessage("Uploaded to product input files directory");
		Assert.assertTrue(reportDetails.get(ReportType.INFO).contains(detailWithEn));

		FileProcessingReportDetail detailWithDa = new FileProcessingReportDetail();
		detailWithDa.setType(ReportType.INFO);
		detailWithDa.setFileName("rel2_Description_Delta-da_DK1000005_20180331.txt");
		detailWithDa.setMessage("Uploaded to product input files directory");
		Assert.assertTrue(reportDetails.get(ReportType.INFO).contains(detailWithDa));

		String expectedDescInEn = "id	effectiveTime	active	moduleId	conceptId	languageCode	typeId	term	caseSignificanceId\r\n" +
				"4927621000005118	20180331	1	554471000005108	554071000005100	en	900000000000003001	Hospital pharmacy (environment)	900000000000448009\r\n" +
				"4927681000005119	20180331	1	554471000005108	550891000005100	en	900000000000003001	Private (environment)	900000000000448009\r\n";

		String actualDesInEn = integrationTestHelper.getInputFile("rel2_Description_Delta-en_DK1000005_20180331.txt");

		assertEquals(expectedDescInEn, actualDesInEn);
		String expectedDescInDa = "id	effectiveTime	active	moduleId	conceptId	languageCode	typeId	term	caseSignificanceId\r\n" +
				"4923861000005118	20180331	1	554471000005108	10829007	da	900000000000013009	øsofagusinfusionstest – Bernstein	900000000000448009\r\n" +
				"2367611000005115	20180331	0	554471000005108	43179002	da	900000000000013009	Punkturaspiration af hæmatom i hud	900000000000448009\r\n" +
				"4921641000005119	20180331	1	554471000005108	405098003	da	900000000000013009	opfattelse af helbredstilstand	900000000000448009\r\n" +
				"4927251000005110	20180331	1	554471000005108	734452007	da	900000000000013009	saxagliptinhydrochlorid	900000000000448009\r\n" +
				"4923271000005110	20180331	1	554471000005108	285873000	da	900000000000013009	evne til at anvende komfur	900000000000448009\r\n" +
				"2178411000005115	20180331	0	554471000005108	66865009	da	900000000000013009	dobbeltanlæg af oesophagus	900000000000448009\r\n";

		String actualDescInDa = integrationTestHelper.getInputFile("rel2_Description_Delta-da_DK1000005_20180331.txt");
		assertEquals(expectedDescInDa, actualDescInDa);
	}
}

package org.ihtsdo.buildcloud.core.service.inputfile.prepare;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.io.IOUtils;
import org.ihtsdo.buildcloud.TestConfig;
import org.ihtsdo.buildcloud.core.dao.BuildDAO;
import org.ihtsdo.buildcloud.core.dao.ProductDAO;
import org.ihtsdo.buildcloud.core.entity.Build;
import org.ihtsdo.buildcloud.core.entity.BuildConfiguration;
import org.ihtsdo.buildcloud.core.entity.Product;
import org.ihtsdo.buildcloud.core.entity.helper.TestEntityGenerator;
import org.ihtsdo.buildcloud.core.service.InputFileService;
import org.ihtsdo.buildcloud.core.service.build.RF2Constants;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.otf.rest.exception.ResourceNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfig.class)
@Transactional
public class InputFileServiceImplTest extends TestEntityGenerator {

	@Autowired
	protected ProductDAO productDAO;
	@Autowired
	private InputFileService inputFileService;
	@Autowired
	private BuildDAO buildDAO;

	protected Product product;
	protected File testArchive;
	protected Set<String> subDirectories;
	private final boolean isDebugRun = true;

	private static final String JULY_RELEASE = "20140731";
	private static final String SRC_EXERTNALLY_MAINTAINED = "externally-maintained";
	private static final String SRC_MAPPING_TOOL = "mapping-tools";
	private static final String SRC_TERM_SERVER = "terminology-server";
	private static final String SRC_REFSET_TOOL = "reference-set-tool";
	private static final String TEST_ARCHIVE_FILE = "input_source_test_data.zip";
	private static final String EMPTY_DATA_FILE = "emptyDelta.zip";

	private Build build;


	@BeforeEach
	public void setup() throws Exception {
		String testFile = getClass().getResource(TEST_ARCHIVE_FILE).getFile();
		testArchive = new File(testFile);
		product = productDAO.find(1L);
		if(product.getBuildConfiguration() == null) {
			product.setBuildConfiguration(new BuildConfiguration());
		}
		if (product.getBuildConfiguration().getEffectiveTime() == null) {
			product.getBuildConfiguration().setEffectiveTime(RF2Constants.DATE_FORMAT.parse(JULY_RELEASE));
		}
		subDirectories = new HashSet<>();
		subDirectories.add(SRC_EXERTNALLY_MAINTAINED);
		subDirectories.add(SRC_MAPPING_TOOL);
		subDirectories.add(SRC_TERM_SERVER);
		subDirectories.add(SRC_REFSET_TOOL);
		build = new Build(new Date(), product.getReleaseCenter().getBusinessKey(), product.getBusinessKey(), product.getBuildConfiguration(), product.getQaTestConfig());
	}

	@AfterEach
	public void tearDown() {
		buildDAO.delete(build.getReleaseCenterKey(), build.getProductKey(), build.getId());
	}


	@Test
	public void testPutSourceFile() throws IOException, ResourceNotFoundException, DecoderException {
		addTestArchiveFileToSourceDirectory(SRC_EXERTNALLY_MAINTAINED);
		List<String> fileList = inputFileService.listSourceFilePaths(product.getReleaseCenter().getBusinessKey(), product.getBusinessKey(), build.getId());
		assertTrue(fileList.size() > 0);
	}

	@Test
	public void listSourceFilePaths() throws ResourceNotFoundException, IOException, DecoderException {
		addEmptyFileToSourceDirectory(SRC_EXERTNALLY_MAINTAINED, "test1.txt");
		addEmptyFileToSourceDirectory(SRC_EXERTNALLY_MAINTAINED, "test2.txt");
		addEmptyFileToSourceDirectory(SRC_MAPPING_TOOL, "test3.txt");
		addEmptyFileToSourceDirectory(SRC_TERM_SERVER, "test4.txt");
		addEmptyFileToSourceDirectory(SRC_REFSET_TOOL, "test5.txt");
		List<String> fileList = inputFileService.listSourceFilePaths(product.getReleaseCenter().getBusinessKey(), product.getBusinessKey(), build.getId());
		assertEquals(5, fileList.size());
	}

	@Test
	public void listSourceFilePathsInSubDirectories() throws ResourceNotFoundException, IOException, DecoderException {
		addEmptyFileToSourceDirectory(SRC_EXERTNALLY_MAINTAINED, "test1.txt");
		addEmptyFileToSourceDirectory(SRC_EXERTNALLY_MAINTAINED, "test2.txt");
		addEmptyFileToSourceDirectory(SRC_MAPPING_TOOL, "test3.txt");
		addEmptyFileToSourceDirectory(SRC_TERM_SERVER, "test4.txt");
		addEmptyFileToSourceDirectory(SRC_REFSET_TOOL, "test5.txt");
		Set<String> subs = new HashSet<>();
		subs.add(SRC_EXERTNALLY_MAINTAINED);
		subs.add(SRC_MAPPING_TOOL);
		List<String> fileList = inputFileService.listSourceFilePathsFromSubDirectories(build.getReleaseCenterKey(), build.getProductKey(), build.getId(), subs);
		assertEquals(3, fileList.size());
	}

	@Test
	public void testPrepareInputFilesInAllSources() throws ResourceNotFoundException, IOException, XPathExpressionException, ParserConfigurationException, SAXException, JAXBException, DecoderException, NoSuchAlgorithmException, BusinessServiceException {
		String testManifestFile = getClass().getResource("manifest_without_sources_specified.xml").getFile();
		File testManifest = new File(testManifestFile);
		addTestArchiveFileToSourceDirectory(SRC_EXERTNALLY_MAINTAINED);
		addTestArchiveFileToSourceDirectory(SRC_REFSET_TOOL);
		addTestArchiveFileToSourceDirectory(SRC_TERM_SERVER);
		buildDAO.putManifestFile(build, new FileInputStream(testManifest));
		SourceFileProcessingReport report = inputFileService.prepareInputFiles(build, false);
		printReport(report);
		assertNotNull(report.getDetails().get(ReportType.ERROR));
		assertEquals(24, report.getDetails().get(ReportType.ERROR).size());
		verifyFileProcessingInAllSources();
	}


	@Test
	public void testPrepareInputFilesForOneSource() throws ResourceNotFoundException, IOException, XPathExpressionException, ParserConfigurationException, SAXException, JAXBException, DecoderException, NoSuchAlgorithmException, BusinessServiceException {
		String testManifestFile = getClass().getResource("manifest_without_sources_specified.xml").getFile();
		File testManifest = new File(testManifestFile);
		addTestArchiveFileToSourceDirectory(SRC_TERM_SERVER);
		buildDAO.putManifestFile(build, new FileInputStream(testManifest));
		SourceFileProcessingReport report = inputFileService.prepareInputFiles(build, false);
		printReport(report);
		assertNull(report.getDetails().get(ReportType.ERROR));
		List<String> inputFileList = buildDAO.listInputFileNames(build);
		printReport(report);
		String [] inputFileCreated = {
		"rel2_Description_Delta-en_INT_20140731.txt",
		"rel2_Refset_SimpleDelta_INT_20140731.txt",
		"rel2_TextDefinition_Delta-en_INT_20140731.txt",
		"rel2_cRefset_AssociationReferenceDelta_INT_20140731.txt",
		"rel2_cRefset_AttributeValueDelta_INT_20140731.txt"};
		for (String filename : inputFileList) {
			assertTrue(Arrays.asList(inputFileCreated).contains(filename), "must contain " + filename);
		}
	}

	@Test
	public void testPrepareInputFilesInRestrictedSources() throws ResourceNotFoundException, IOException, XPathExpressionException, ParserConfigurationException, SAXException, JAXBException, DecoderException, NoSuchAlgorithmException, BusinessServiceException {
		String testManifestFile = getClass().getResource("manifest_restricted_sources.xml").getFile();
		File testManifest = new File(testManifestFile);
		addTestArchiveFileToSourceDirectory(SRC_MAPPING_TOOL);
		addTestArchiveFileToSourceDirectory(SRC_TERM_SERVER);
		buildDAO.putManifestFile(build, new FileInputStream(testManifest));
		SourceFileProcessingReport report = inputFileService.prepareInputFiles(build, false);
		printReport(report);

		assertEquals(2, report.getDetails().get(ReportType.ERROR).size());
		verifyForFileProcessingInRestrictedSources();

	}

	@Test
	public void testPrepareInputFilesMissingRefsets() throws ResourceNotFoundException, IOException, NoSuchAlgorithmException, JAXBException, DecoderException, BusinessServiceException {
		String testManifestFile = getClass().getResource("manifest_missing_refsets.xml").getFile();
		File testManifest = new File(testManifestFile);
		addTestArchiveFileToSourceDirectory(SRC_REFSET_TOOL);
		buildDAO.putManifestFile(build, new FileInputStream(testManifest));
		SourceFileProcessingReport fileProcessingReport = inputFileService.prepareInputFiles(build,false);
		verifyForFileProcessingMissingRefsets(fileProcessingReport);
	}

	@Test
	public void testPrepareInputFilesWithTerminologyFiles() throws ResourceNotFoundException, IOException, NoSuchAlgorithmException, JAXBException, DecoderException, BusinessServiceException {
		String testManifestFile = getClass().getResource("manifest_with_unprocessed.xml").getFile();
		File testManifest = new File(testManifestFile);
		addTestArchiveFileToSourceDirectory(SRC_TERM_SERVER);
		addEmptyFileToSourceDirectory(SRC_EXERTNALLY_MAINTAINED,"sct2_Concept_Delta_INT_20170731.txt");
		buildDAO.putManifestFile(build, new FileInputStream(testManifest));
		SourceFileProcessingReport report = inputFileService.prepareInputFiles(build, true);
		printReport(report);
		verifyResults();
	}

	@Test
	public void testPrepareFilesForDK_Extension() throws Exception {
		 String testManifestFile = getClass().getResource("manifest_dk.xml").getFile();
		 File testManifest = new File(testManifestFile);
		 addTestArchiveFileToSourceDirectory(SRC_TERM_SERVER);
		 buildDAO.putManifestFile(build, new FileInputStream(testManifest));
		 SourceFileProcessingReport report = inputFileService.prepareInputFiles(build, true);
		 printReport(report);
		 List<String> inputFileList = buildDAO.listInputFileNames(build);
//		 assertNotNull(report.getDetails().get(ReportType.ERROR));
//		 assertEquals(3,report.getDetails().get(ReportType.ERROR).size());
//		 String [] fileNameReportedWithError = {"sct2_Concept_Delta_DK1000005_20170731.txt",
//				 "sct2_Relationship_Delta_DK1000005_20170731.txt",
//				 "sct2_StatedRelationship_Delta_DK1000005_20170731.txt"};
//		 for (FileProcessingReportDetail detail : report.getDetails().get(ReportType.ERROR)) {
//			 assertTrue("must contain" + detail.getFileName(),  Arrays.asList(fileNameReportedWithError).contains(detail.getFileName()));
//			 assertEquals("Required by manifest but not found in any source.", detail.getMessage());
//		 }
		 assertEquals(8, inputFileList.size());
	}


	@Test
	public void testPrepareFilesForSE_Extension() throws Exception {
		 String testManifestFile = getClass().getResource("manifest_se_test.xml").getFile();
		 File testManifest = new File(testManifestFile);
		 addTestArchiveFileToSourceDirectory(SRC_TERM_SERVER);
		 addTestArchiveFileToSourceDirectory(SRC_EXERTNALLY_MAINTAINED);
		 buildDAO.putManifestFile(build, new FileInputStream(testManifest));
		 SourceFileProcessingReport report = inputFileService.prepareInputFiles(build, true);
		 List<String> inputFileList = buildDAO.listInputFileNames(build);
		 printReport(report);
		 assertFileNameExist(inputFileList,"rel2_TextDefinition_Delta-sv_SE1000052_20170531.txt");
		 assertFileNameExist(inputFileList,"rel2_TextDefinition_Delta-en_SE1000052_20170531.txt");
		 assertFileNameExist(inputFileList,"rel2_Description_Delta-sv_SE1000052_20170531.txt");
		 assertFileNameExist(inputFileList,"rel2_Description_Delta-en_SE1000052_20170531.txt");
		 assertFileNameExist(inputFileList,"rel2_cRefset_AttributeValueDelta_SE1000052_20170531.txt");
		 assertFileNameExist(inputFileList,"rel2_cRefset_AssociationReferenceDelta_SE1000052_20170531.txt");
		 assertEquals(14, report.getDetails().get(ReportType.ERROR).size());
		 assertEquals(22, report.getDetails().get(ReportType.WARNING).size());

	}

	@Test
	public void testPrepareInputFilesWithEmptyData() throws ResourceNotFoundException, IOException, XPathExpressionException, ParserConfigurationException, SAXException, JAXBException, DecoderException, NoSuchAlgorithmException, BusinessServiceException {
		String testManifestFile = getClass().getResource("manifest_with_additonal_fields.xml").getFile();
		File testManifest = new File(testManifestFile);
		String testFile = getClass().getResource(EMPTY_DATA_FILE).getFile();
		File testArchive = new File(testFile);
		addTestFileToSourceDirectory(SRC_TERM_SERVER,testArchive);
		buildDAO.putManifestFile(build, new FileInputStream(testManifest));
		SourceFileProcessingReport report =  inputFileService.prepareInputFiles(build,true);
		List<String> inputFileList = buildDAO.listInputFileNames(build);
		printReport(report);
		assertFileNameExist(inputFileList,"rel2_cRefset_AttributeValueDelta_INT_20170731.txt");
		assertFileNameExist(inputFileList,"rel2_cRefset_AssociationReferenceDelta_INT_20170731.txt");
		assertFileNameExist(inputFileList,"rel2_Description_Delta-en_INT_20170731.txt");
		assertFileNameExist(inputFileList,"rel2_TextDefinition_Delta-en_INT_20170731.txt");
		assertFileNameExist(inputFileList,"rel2_Refset_SimpleDelta_INT_20170731.txt");
		assertFileNameExist(inputFileList, "rel2_cRefset_LanguageDelta-en_INT_20170731.txt");
		assertEquals(6, inputFileList.size());
		assertNotNull(report.getDetails().get(ReportType.ERROR));
		assertEquals(10, report.getDetails().get(ReportType.ERROR).size());
		String [] filesReportedAsError = {"doc_Icd10MapTechnicalGuideExemplars_Current-en-US_INT_20170731.xlsx",
				"der2_sRefset_SimpleMapDelta_INT_20170731.txt",
				"der2_iisssccRefset_ExtendedMapDelta_INT_20170731.txt",
				"der2_cciRefset_RefsetDescriptorDelta_INT_20170731.txt",
				"der2_ssRefset_ModuleDependencyDelta_INT_20170731.txt",
				"der2_ciRefset_DescriptionTypeDelta_INT_20170731.txt",
				"der2_sssssssRefset_MRCMDomainDelta_INT_20170731.txt",
				"der2_cissccRefset_MRCMAttributeDomainDelta_INT_20170731.txt",
				"der2_ssccRefset_MRCMAttributeRangeDelta_INT_20170731.txt",
				"der2_cRefset_MRCMModuleScopeDelta_INT_20170731.txt"};
		for (FileProcessingReportDetail reportDetail :report.getDetails().get(ReportType.ERROR)) {
			assertEquals("Required by manifest but not found in any source.", reportDetail.getMessage());
			assertTrue(Arrays.asList(filesReportedAsError).contains(reportDetail.getFileName()), "must contain" + reportDetail.getFileName());
		}
		assertEquals(5, report.getDetails().get(ReportType.WARNING).size());
		assertEquals(20, report.getDetails().get(ReportType.INFO).size());
	}


	@Test
	public void testPrepareInputFileWithMultipleLanguageCodes() throws ResourceNotFoundException, IOException, XPathExpressionException, ParserConfigurationException, SAXException, JAXBException, DecoderException, NoSuchAlgorithmException, BusinessServiceException {
		String testManifestFile = getClass().getResource("manifest_with_multiple_language_codes.xml").getFile();
		File testManifest = new File(testManifestFile);
		String testFile = getClass().getResource(EMPTY_DATA_FILE).getFile();
		File testArchive = new File(testFile);
		addTestFileToSourceDirectory(SRC_TERM_SERVER, testArchive);
		addTestFileToSourceDirectory(SRC_EXERTNALLY_MAINTAINED, new File(getClass().getResource("externally-maintained-empty-delta.zip").getFile()));
		buildDAO.putManifestFile(build, new FileInputStream(testManifest));
		SourceFileProcessingReport report =  inputFileService.prepareInputFiles(build, true);
		List<String> inputFileList = buildDAO.listInputFileNames(build);
		printReport(report);
		String [] fileNames = {"rel2_Concept_Delta_DK1000005_20170731.txt",
				"rel2_Description_Delta_DK1000005_20170731.txt",
				"rel2_Refset_SimpleDelta_DK1000005_20170731.txt",
				"rel2_Relationship_Delta_DK1000005_20170731.txt",
				"rel2_StatedRelationship_Delta_DK1000005_20170731.txt",
				"rel2_TextDefinition_Delta-da_DK1000005_20170731.txt",
				"rel2_TextDefinition_Delta-en_DK1000005_20170731.txt",
				"rel2_cRefset_AssociationReferenceDelta_DK1000005_20170731.txt",
				"rel2_cRefset_AttributeValueDelta_DK1000005_20170731.txt"};
		assertEquals(fileNames.length, inputFileList.size());
		assertFileNameExist(inputFileList,fileNames);
//		assertNotNull(report.getDetails().get(ReportType.ERROR));
//		assertEquals(1,report.getDetails().get(ReportType.ERROR).size());
		assertNotNull(report.getDetails().get(ReportType.WARNING));
		assertEquals(8,report.getDetails().get(ReportType.WARNING).size());
	}


	@Test
	public void testPrepareInputFilesWithEmptyDataInTwoSources() throws ResourceNotFoundException, IOException, XPathExpressionException, ParserConfigurationException, SAXException, JAXBException, DecoderException, NoSuchAlgorithmException, BusinessServiceException {
		String testManifestFile = getClass().getResource("manifest_with_multiple_sources.xml").getFile();
		File testManifest = new File(testManifestFile);
		String testFile = getClass().getResource(EMPTY_DATA_FILE).getFile();
		File testArchive = new File(testFile);
		addTestFileToSourceDirectory(SRC_TERM_SERVER, testArchive);
		addTestFileToSourceDirectory(SRC_EXERTNALLY_MAINTAINED, new File(getClass().getResource("externally-maintained-empty-delta.zip").getFile()));
		buildDAO.putManifestFile(build, new FileInputStream(testManifest));
		SourceFileProcessingReport report =  inputFileService.prepareInputFiles(build,true);
		List<String> inputFileList = buildDAO.listInputFileNames(build);
		printReport(report);
		String [] fileNames = {"rel2_Concept_Delta_INT_20170731.txt",
				"rel2_Description_Delta-en_INT_20170731.txt",
				"rel2_Identifier_Delta_INT_20170731.txt",
				"rel2_Refset_SimpleDelta_INT_20170731.txt",
				"rel2_Relationship_Delta_INT_20170731.txt",
				"rel2_StatedRelationship_Delta_INT_20170731.txt",
				"rel2_TextDefinition_Delta-en_INT_20170731.txt",
				"rel2_cRefset_AssociationReferenceDelta_INT_20170731.txt",
				"rel2_cRefset_AttributeValueDelta_INT_20170731.txt",
				"rel2_cRefset_LanguageDelta-en_INT_20170731.txt",
				"rel2_cRefset_MRCMModuleScopeDelta_INT_20170731.txt",
				"rel2_cciRefset_RefsetDescriptorDelta_INT_20170731.txt",
				"rel2_ciRefset_DescriptionTypeDelta_INT_20170731.txt",
				"rel2_cissccRefset_MRCMAttributeDomainDelta_INT_20170731.txt",
				"rel2_iisssccRefset_ExtendedMapDelta_INT_20170731.txt",
				"rel2_sRefset_SimpleMapDelta_INT_20170731.txt",
				"rel2_ssRefset_ModuleDependencyDelta_INT_20170731.txt",
				"rel2_ssccRefset_MRCMAttributeRangeDelta_INT_20170731.txt",
				"rel2_sssssssRefset_MRCMDomainDelta_INT_20170731.txt"};
		assertEquals(fileNames.length, inputFileList.size());
		assertFileNameExist(inputFileList,fileNames);
		assertNull(report.getDetails().get(ReportType.ERROR));
		assertNotNull(report.getDetails().get(ReportType.WARNING));
		assertEquals(7,report.getDetails().get(ReportType.WARNING).size());
	}

	@Test
	public void testPrepareInputFilesWithInvalidManifest() throws ResourceNotFoundException, IOException, JAXBException, DecoderException, NoSuchAlgorithmException, BusinessServiceException {
		String testManifestFile = getClass().getResource("invalid_manifest.xml").getFile();
		File testManifest = new File(testManifestFile);
		String testFile = getClass().getResource(EMPTY_DATA_FILE).getFile();
		File testArchive = new File(testFile);
		addTestFileToSourceDirectory(SRC_TERM_SERVER, testArchive);
		buildDAO.putManifestFile(build, new FileInputStream(testManifest));
		SourceFileProcessingReport report =  inputFileService.prepareInputFiles(build, true);
		printReport(report);
		assertNotNull(report.getDetails().get(ReportType.ERROR));
		assertNotNull(report.getDetails().get(ReportType.ERROR).get(0).getMessage());
		assertEquals("manifest.xml doesn't conform to the schema definition. The element type \"refset\" must be terminated by the matching end-tag \"</refset>\". The issue lies in the manifest.xml at line 22 and column 9",
				report.getDetails().get(ReportType.ERROR).get(0).getMessage());
	}

	@Test
	public void testPrepareInputFilesWithMultipleSources() throws ResourceNotFoundException, IOException, JAXBException, DecoderException, NoSuchAlgorithmException, BusinessServiceException {
		String testManifestFile = getClass().getResource("manifest_with_multiple_sources.xml").getFile();
		File testManifest = new File(testManifestFile);
		addTestFileToSourceDirectory(SRC_TERM_SERVER, new File(getClass().getResource(TEST_ARCHIVE_FILE).getFile()));
		addTestFileToSourceDirectory(SRC_EXERTNALLY_MAINTAINED, new File(getClass().getResource(TEST_ARCHIVE_FILE).getFile()));
		buildDAO.putManifestFile(build, new FileInputStream(testManifest));
		SourceFileProcessingReport report =  inputFileService.prepareInputFiles(build, true);
		List<String> inputFileList = buildDAO.listInputFileNames(build);
		printReport(report);
		assertEquals(9, inputFileList.size());
		assertFileNameExist(inputFileList,"rel2_cRefset_AttributeValueDelta_INT_20170731.txt");
		assertFileNameExist(inputFileList,"rel2_cRefset_AssociationReferenceDelta_INT_20170731.txt");
		assertFileNameExist(inputFileList,"rel2_Description_Delta-en_INT_20170731.txt");
		assertFileNameExist(inputFileList,"rel2_TextDefinition_Delta-en_INT_20170731.txt");
		assertFileNameExist(inputFileList,"rel2_Refset_SimpleDelta_INT_20170731.txt");
		assertFileNameExist(inputFileList,"rel2_TextDefinition_Delta-en_INT_20170731.txt");
		assertFileNameExist(inputFileList,"rel2_cissccRefset_MRCMAttributeDomainDelta_INT_20170731.txt");

		InputStream inputStream = buildDAO.getInputFileStream(build, "rel2_Concept_Delta_INT_20170731.txt");
		assertNotNull(inputStream);
		inputStream = buildDAO.getInputFileStream(build, "rel2_Relationship_Delta_INT_20170731.txt");
		assertNotNull(inputStream);
		assertEquals(5, IOUtils.readLines(inputStream, "UTF-8").size());
		assertNotNull(report.getDetails().get(ReportType.ERROR));
		assertEquals(22, report.getDetails().get(ReportType.ERROR).size());
		assertNotNull(report.getDetails().get(ReportType.WARNING));
		assertEquals(22, report.getDetails().get(ReportType.WARNING).size());
	}



	private void verifyFileProcessingInAllSources() throws ResourceNotFoundException, IOException {
		InputStream inputStream = buildDAO.getInputFileStream(build, "rel2_cRefset_AssociationReferenceDelta_INT_20140731.txt");
		assertNotNull(inputStream);
		assertEquals(9, IOUtils.readLines(inputStream, Charset.defaultCharset()).size());
		inputStream = buildDAO.getInputFileStream(build, "rel2_cRefset_AttributeValueDelta_INT_20140731.txt");
		assertNotNull(inputStream);
		assertEquals(3, IOUtils.readLines(inputStream, Charset.defaultCharset()).size());
		inputStream = buildDAO.getInputFileStream(build, "rel2_TextDefinition_Delta-en_INT_20140731.txt");
		assertNotNull(inputStream);
		assertEquals(2, IOUtils.readLines(inputStream, Charset.defaultCharset()).size());
		inputStream = buildDAO.getInputFileStream(build, "rel2_Description_Delta-en_INT_20140731.txt");
		assertNotNull(inputStream);
		assertEquals(2, IOUtils.readLines(inputStream, Charset.defaultCharset()).size());
	}

	private void verifyForFileProcessingInRestrictedSources() throws ResourceNotFoundException, IOException {
		InputStream inputStream = buildDAO.getInputFileStream(build, "rel2_cRefset_AssociationReferenceDelta_INT_20180731.txt");
		assertNotNull(inputStream);
		assertEquals(7, IOUtils.readLines(inputStream, Charset.defaultCharset()).size());
		inputStream = buildDAO.getInputFileStream(build, "rel2_cRefset_AttributeValueDelta_INT_20180731.txt");
		assertNotNull(inputStream);
		assertEquals(2, IOUtils.readLines(inputStream, Charset.defaultCharset()).size());
		inputStream = buildDAO.getInputFileStream(build, "rel2_TextDefinition_Delta-en_INT_20180731.txt");
		assertNotNull(inputStream);
		assertEquals(2, IOUtils.readLines(inputStream, Charset.defaultCharset()).size());
		inputStream = buildDAO.getInputFileStream(build, "rel2_Description_Delta-en_INT_20180731.txt");
		assertNotNull(inputStream);
		assertEquals(2, IOUtils.readLines(inputStream, Charset.defaultCharset()).size());
	}

	private void verifyResults() throws ResourceNotFoundException, IOException {
		List<String> inputFileList = buildDAO.listInputFileNames(build);
		assertEquals(7, inputFileList.size());
		InputStream inputStream = buildDAO.getInputFileStream(build, "rel2_cRefset_AssociationReferenceDelta_INT_20170731.txt");
		assertNotNull(inputStream);
		assertEquals(9, IOUtils.readLines(inputStream, Charset.defaultCharset()).size());
		inputStream = buildDAO.getInputFileStream(build, "rel2_cRefset_AttributeValueDelta_INT_20170731.txt");
		assertNotNull(inputStream);
		assertEquals(3, IOUtils.readLines(inputStream, Charset.defaultCharset()).size());
		inputStream = buildDAO.getInputFileStream(build, "rel2_TextDefinition_Delta-en_INT_20170731.txt");
		assertNotNull(inputStream);
		assertEquals(2, IOUtils.readLines(inputStream, Charset.defaultCharset()).size());
		inputStream = buildDAO.getInputFileStream(build, "rel2_Description_Delta-en_INT_20170731.txt");
		assertNotNull(inputStream);
		assertEquals(2, IOUtils.readLines(inputStream, Charset.defaultCharset()).size());

		//Unprocessed files
		inputStream = buildDAO.getInputFileStream(build, "rel2_Concept_Delta_INT_20170731.txt");
		assertNotNull(inputStream);
		inputStream = buildDAO.getInputFileStream(build, "rel2_Relationship_Delta_INT_20170731.txt");
		assertNotNull(inputStream);
		assertEquals(5, IOUtils.readLines(inputStream, Charset.defaultCharset()).size());
		inputStream = buildDAO.getInputFileStream(build, "rel2_Refset_SimpleDelta_INT_20170731.txt");
		assertNotNull(inputStream);
	}


	private void verifyForFileProcessingMissingRefsets(SourceFileProcessingReport fileProcessingReport) {
		String[] missingRefsets = {"900000000000523009","900000000000531004","900000000000489007"};
		int countMissingRefset = 0;
		Map<ReportType,List<FileProcessingReportDetail>> reportDetails = fileProcessingReport.getDetails();
		//for (FileProcessingReportDetail reportDetail : reportDetails) {
			if(reportDetails.containsKey(ReportType.WARNING)) {
				for (FileProcessingReportDetail reportDetail : reportDetails.get(ReportType.WARNING)) {
					String message = reportDetail.getMessage();
					for (String missingRefset : missingRefsets) {
						if (message.contains(missingRefset)) {
							countMissingRefset++;
							break;
						}
					}
				}
			}
		//}
		assertEquals(3, countMissingRefset);
		System.out.println(fileProcessingReport);
	}

	protected void addEmptyFileToSourceDirectory(final String sourceName, final String filename) throws ResourceNotFoundException, IOException, DecoderException {
		final File tempFile = File.createTempFile("testTemp", ".txt");
		try (InputStream inputStream = new FileInputStream(tempFile)) {
			inputFileService.putSourceFile(sourceName,product.getReleaseCenter().getBusinessKey(), product.getBusinessKey(), build.getId(), inputStream, filename, 0L);
		} finally {
			tempFile.delete();
		}
	}

	protected void addTestArchiveFileToSourceDirectory(final String sourceName) throws ResourceNotFoundException, IOException, DecoderException {
		InputStream inputStream = new FileInputStream(testArchive);
		inputFileService.putSourceFile(sourceName, product.getReleaseCenter().getBusinessKey(), product.getBusinessKey(), build.getId(), inputStream, TEST_ARCHIVE_FILE, 0L);
	}

	protected void addTestFileToSourceDirectory(final String sourceName, File zipFile ) throws ResourceNotFoundException, IOException, DecoderException {
		InputStream inputStream = new FileInputStream(zipFile);
		inputFileService.putSourceFile(sourceName, product.getReleaseCenter().getBusinessKey(), product.getBusinessKey(), build.getId(), inputStream, zipFile.getName(), 0L);
	}

	private void assertFileNameExist (List<String> inputFileList, String ...fileNames ) {
		for (String name : fileNames) {
			assertTrue(inputFileList.contains(name), "File name must exist:" + name);
		}
	}

	private void printReport(SourceFileProcessingReport report) {
		if (isDebugRun) {
			 System.out.println(report);
		}
	}


}

package org.ihtsdo.buildcloud.service.inputfile.prepare;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.DecoderException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.ihtsdo.buildcloud.config.DailyBuildResourceConfig;
import org.ihtsdo.buildcloud.config.HibernateTransactionManagerConfiguration;
import org.ihtsdo.buildcloud.config.LocalSessionFactoryBeanConfiguration;
import org.ihtsdo.buildcloud.dao.*;
import org.ihtsdo.buildcloud.dao.helper.BuildS3PathHelper;
import org.ihtsdo.buildcloud.entity.BuildConfiguration;
import org.ihtsdo.buildcloud.entity.Product;
import org.ihtsdo.buildcloud.entity.helper.TestEntityGenerator;
import org.ihtsdo.buildcloud.service.*;
import org.ihtsdo.buildcloud.service.build.RF2Constants;
import org.ihtsdo.buildcloud.service.build.readme.ReadmeGenerator;
import org.ihtsdo.buildcloud.service.build.transform.LegacyIdTransformationService;
import org.ihtsdo.buildcloud.service.build.transform.PesudoUUIDGenerator;
import org.ihtsdo.buildcloud.service.build.transform.TransformationService;
import org.ihtsdo.buildcloud.service.identifier.client.IdServiceRestClientOfflineDemoImpl;
import org.ihtsdo.buildcloud.service.postcondition.PostconditionManager;
import org.ihtsdo.buildcloud.service.precondition.PreconditionManager;
import org.ihtsdo.buildcloud.service.workbenchdatafix.ModuleResolverService;
import org.ihtsdo.otf.dao.s3.OfflineS3ClientImpl;
import org.ihtsdo.otf.dao.s3.helper.S3ClientHelper;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.otf.rest.exception.ResourceNotFoundException;
import org.ihtsdo.snomed.util.rf2.schema.SchemaFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

@EnableConfigurationProperties
@PropertySource(value = "classpath:application.properties", encoding = "UTF-8")
@TestConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {BuildDAOImpl.class, ProductServiceImpl.class, BuildServiceImpl.class,
        PublishServiceImpl.class, ProductDAOImpl.class, OfflineS3ClientImpl.class, S3ClientHelper.class,
        ObjectMapper.class, BuildS3PathHelper.class, ProductInputFileDAOImpl.class, LocalSessionFactoryBeanConfiguration.class,
        HibernateTransactionManagerConfiguration.class, ExtensionConfigDAOImpl.class, ReleaseCenterDAOImpl.class,
        IdServiceRestClientOfflineDemoImpl.class, SchemaFactory.class, PreconditionManager.class, PostconditionManager.class,
        ReadmeGenerator.class, TransformationService.class, PesudoUUIDGenerator.class, ModuleResolverService.class,
        LegacyIdTransformationService.class, DailyBuildResourceConfig.class, TermServerServiceImpl.class, ReleaseCenterServiceImpl.class,
        ProductInputFileServiceImpl.class})
@Transactional
public class ProductInputFileServiceImplTest extends TestEntityGenerator {

    @Autowired
    protected ProductDAO productDAO;
    @Autowired
    private ProductInputFileService productInputFileService;
    @Autowired
    private ProductInputFileDAO productInputFileDAO;

    protected Product product;
    protected File testArchive;
    protected Set<String> subDirectories;
	private boolean isDebugRun = false;

    private static final String JULY_RELEASE = "20140731";
    private static final String SRC_EXERTNALLY_MAINTAINED = "externally-maintained";
    private static final String SRC_MAPPING_TOOL = "mapping-tools";
    private static final String SRC_TERM_SERVER = "terminology-server";
    private static final String SRC_REFSET_TOOL = "reference-set-tool";
    private static final String TEST_ARCHIVE_FILE = "input_source_test_data.zip";
	private static final String EMPTY_DATA_FILE = "emptyDelta.zip";


    @Before
    public void setup() throws Exception {
        String testFile = getClass().getResource(TEST_ARCHIVE_FILE).getFile();
        testArchive = new File(testFile);
//        SecurityHelper.setUser(TestUtils.TEST_USER);
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
        productInputFileService.deleteFilesByPattern(product.getReleaseCenter().getBusinessKey(), product.getBusinessKey(), "*.txt");
        productInputFileService.deleteSourceFilesByPattern(product.getReleaseCenter().getBusinessKey(), product.getBusinessKey(), "*.txt", null);
        productInputFileService.deleteSourceFilesByPattern(product.getReleaseCenter().getBusinessKey(), product.getBusinessKey(), "*.xlsx", null);
    }

    @After
    public void tearDown() {
        productInputFileService.deleteSourceFilesByPattern(product.getReleaseCenter().getBusinessKey(), product.getBusinessKey(), "*.txt", null);
        productInputFileService.deleteSourceFilesByPattern(product.getReleaseCenter().getBusinessKey(), product.getBusinessKey(), "*.xlsx", null);
    }


    @Test
    public void testPutSourceFile() throws IOException, ResourceNotFoundException {
        addTestArchiveFileToSourceDirectory(SRC_EXERTNALLY_MAINTAINED);
        List<String> fileList = productInputFileService.listSourceFilePaths(product.getReleaseCenter().getBusinessKey(), product.getBusinessKey());
        assertTrue(fileList.size() > 0);
    }

    @Test
    public void listSourceFilePaths() throws ResourceNotFoundException, IOException {
        addEmptyFileToSourceDirectory(SRC_EXERTNALLY_MAINTAINED, "test1.txt");
        addEmptyFileToSourceDirectory(SRC_EXERTNALLY_MAINTAINED, "test2.txt");
        addEmptyFileToSourceDirectory(SRC_MAPPING_TOOL, "test3.txt");
        addEmptyFileToSourceDirectory(SRC_TERM_SERVER, "test4.txt");
        addEmptyFileToSourceDirectory(SRC_REFSET_TOOL, "test5.txt");
        List<String> fileList = productInputFileService.listSourceFilePaths(product.getReleaseCenter().getBusinessKey(), product.getBusinessKey());
        assertEquals(5, fileList.size());
    }

    @Test
    public void listSourceFilePathsInSubDirectories() throws ResourceNotFoundException, IOException {
        addEmptyFileToSourceDirectory(SRC_EXERTNALLY_MAINTAINED, "test1.txt");
        addEmptyFileToSourceDirectory(SRC_EXERTNALLY_MAINTAINED, "test2.txt");
        addEmptyFileToSourceDirectory(SRC_MAPPING_TOOL, "test3.txt");
        addEmptyFileToSourceDirectory(SRC_TERM_SERVER, "test4.txt");
        addEmptyFileToSourceDirectory(SRC_REFSET_TOOL, "test5.txt");
        Set<String> subs = new HashSet<>();
        subs.add(SRC_EXERTNALLY_MAINTAINED);
        subs.add(SRC_MAPPING_TOOL);
        List<String> fileList = productInputFileService.listSourceFilePathsFromSubDirectories(product.getReleaseCenter().getBusinessKey(), product.getBusinessKey(), subs);
        assertEquals(3, fileList.size());
    }

    @Test
    public void testPrepareInputFilesInAllSources() throws ResourceNotFoundException, IOException, XPathExpressionException, ParserConfigurationException, SAXException, JAXBException, DecoderException, NoSuchAlgorithmException, BusinessServiceException {
        String testManifestFile = getClass().getResource("manifest_without_sources_specified.xml").getFile();
        File testManifest = new File(testManifestFile);
        addTestArchiveFileToSourceDirectory(SRC_EXERTNALLY_MAINTAINED);
        addTestArchiveFileToSourceDirectory(SRC_REFSET_TOOL);
        addTestArchiveFileToSourceDirectory(SRC_TERM_SERVER);
        productInputFileDAO.putManifestFile(product, new FileInputStream(testManifest), testManifest.getName(), testManifest.length());
        SourceFileProcessingReport report = productInputFileService.prepareInputFiles(product.getReleaseCenter().getBusinessKey(), product.getBusinessKey(), false);
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
        productInputFileDAO.putManifestFile(product, new FileInputStream(testManifest), testManifest.getName(), testManifest.length());
        SourceFileProcessingReport report = productInputFileService.prepareInputFiles(product.getReleaseCenter().getBusinessKey(), product.getBusinessKey(), false);
        printReport(report);
        assertNull(report.getDetails().get(ReportType.ERROR));
        List<String> inputFileList = productInputFileService.listInputFilePaths(product.getReleaseCenter().getBusinessKey(), product.getBusinessKey());
        printReport(report);
        String [] inputFileCreated = {
        "rel2_Description_Delta-en_INT_20140731.txt",
        "rel2_Refset_SimpleDelta_INT_20140731.txt",
        "rel2_TextDefinition_Delta-en_INT_20140731.txt",
        "rel2_cRefset_AssociationReferenceDelta_INT_20140731.txt",
        "rel2_cRefset_AttributeValueDelta_INT_20140731.txt"};
        for (String filename : inputFileList) {
        	assertTrue("must contain " + filename, Arrays.asList(inputFileCreated).contains(filename));
        }
    }

    @Test
    public void testPrepareInputFilesInRestrictedSources() throws ResourceNotFoundException, IOException, XPathExpressionException, ParserConfigurationException, SAXException, JAXBException, DecoderException, NoSuchAlgorithmException, BusinessServiceException {
        String testManifestFile = getClass().getResource("manifest_restricted_sources.xml").getFile();
        File testManifest = new File(testManifestFile);
        addTestArchiveFileToSourceDirectory(SRC_MAPPING_TOOL);
        addTestArchiveFileToSourceDirectory(SRC_TERM_SERVER);
        productInputFileDAO.putManifestFile(product, new FileInputStream(testManifest), testManifest.getName(), testManifest.length());
        SourceFileProcessingReport report = productInputFileService.prepareInputFiles(product.getReleaseCenter().getBusinessKey(), product.getBusinessKey(), false);
        printReport(report);

        assertEquals(2, report.getDetails().get(ReportType.ERROR).size());
        verifyForFileProcessingInRestrictedSources();

    }

    @Test
    public void testPrepareInputFilesMissingRefsets() throws ResourceNotFoundException, IOException, NoSuchAlgorithmException, JAXBException, DecoderException, BusinessServiceException {
        String testManifestFile = getClass().getResource("manifest_missing_refsets.xml").getFile();
        File testManifest = new File(testManifestFile);
        addTestArchiveFileToSourceDirectory(SRC_REFSET_TOOL);
        productInputFileDAO.putManifestFile(product, new FileInputStream(testManifest), testManifest.getName(), testManifest.length());
        SourceFileProcessingReport fileProcessingReport = productInputFileService.prepareInputFiles(product.getReleaseCenter().getBusinessKey(), product.getBusinessKey(), false);
        verifyForFileProcessingMissingRefsets(fileProcessingReport);
    }

    @Test
    public void testPrepareInputFilesWithTerminologyFiles() throws ResourceNotFoundException, IOException, NoSuchAlgorithmException, JAXBException, DecoderException, BusinessServiceException {
        String testManifestFile = getClass().getResource("manifest_with_unprocessed.xml").getFile();
        File testManifest = new File(testManifestFile);
        addTestArchiveFileToSourceDirectory(SRC_TERM_SERVER);
        addEmptyFileToSourceDirectory(SRC_EXERTNALLY_MAINTAINED,"sct2_Concept_Delta_INT_20170731.txt");
        productInputFileDAO.putManifestFile(product, new FileInputStream(testManifest), testManifest.getName(), testManifest.length());
        SourceFileProcessingReport report = productInputFileService.prepareInputFiles(product.getReleaseCenter().getBusinessKey(), product.getBusinessKey(), true);
        printReport(report);
        verifyResults();
    }

    @Test
    public void testPrepareFilesForDK_Extension() throws Exception {
    	 String testManifestFile = getClass().getResource("manifest_dk.xml").getFile();
         File testManifest = new File(testManifestFile);
         addTestArchiveFileToSourceDirectory(SRC_TERM_SERVER);
         productInputFileDAO.putManifestFile(product, new FileInputStream(testManifest), testManifest.getName(), testManifest.length());
         SourceFileProcessingReport report = productInputFileService.prepareInputFiles(product.getReleaseCenter().getBusinessKey(), product.getBusinessKey(), true);
         printReport(report);
         List<String> inputFileList = productInputFileService.listInputFilePaths(product.getReleaseCenter().getBusinessKey(), product.getBusinessKey());
         assertNotNull(report.getDetails().get(ReportType.ERROR));
         assertEquals(3,report.getDetails().get(ReportType.ERROR).size());
         String [] fileNameReportedWithError = {"sct2_Concept_Delta_DK1000005_20170731.txt",
        		 "sct2_Relationship_Delta_DK1000005_20170731.txt",
        		 "sct2_StatedRelationship_Delta_DK1000005_20170731.txt"};
         for (FileProcessingReportDetail detail : report.getDetails().get(ReportType.ERROR)) {
        	 assertTrue("must contain" + detail.getFileName(),  Arrays.asList(fileNameReportedWithError).contains(detail.getFileName()));
             assertEquals("Required by manifest but not found in any source.", detail.getMessage());
         }
         assertEquals(5, inputFileList.size());
    }


    @Test
    public void testPrepareFilesForSE_Extension() throws Exception {
    	 String testManifestFile = getClass().getResource("manifest_se_test.xml").getFile();
         File testManifest = new File(testManifestFile);
         addTestArchiveFileToSourceDirectory(SRC_TERM_SERVER);
         addTestArchiveFileToSourceDirectory(SRC_EXERTNALLY_MAINTAINED);
         productInputFileDAO.putManifestFile(product, new FileInputStream(testManifest), testManifest.getName(), testManifest.length());
         SourceFileProcessingReport report = productInputFileService.prepareInputFiles(product.getReleaseCenter().getBusinessKey(), product.getBusinessKey(), true);
         List<String> inputFileList = productInputFileService.listInputFilePaths(product.getReleaseCenter().getBusinessKey(), product.getBusinessKey());
         printReport(report);
         assertFileNameExist(inputFileList,"rel2_TextDefinition_Delta-sv_SE1000052_20170531.txt");
         assertFileNameExist(inputFileList,"rel2_TextDefinition_Delta-en_SE1000052_20170531.txt");
         assertFileNameExist(inputFileList,"rel2_Description_Delta-sv_SE1000052_20170531.txt");
         assertFileNameExist(inputFileList,"rel2_Description_Delta-en_SE1000052_20170531.txt");
         assertFileNameExist(inputFileList,"rel2_cRefset_AttributeValueDelta_SE1000052_20170531.txt");
         assertFileNameExist(inputFileList,"rel2_cRefset_AssociationReferenceDelta_SE1000052_20170531.txt");
         assertEquals(18, report.getDetails().get(ReportType.ERROR).size());
         assertEquals(16, report.getDetails().get(ReportType.WARNING).size());

    }

    @Test
    public void testPrepareInputFilesWithEmptyData() throws ResourceNotFoundException, IOException, XPathExpressionException, ParserConfigurationException, SAXException, JAXBException, DecoderException, NoSuchAlgorithmException, BusinessServiceException {
        String testManifestFile = getClass().getResource("manifest_with_additonal_fields.xml").getFile();
        File testManifest = new File(testManifestFile);
        String testFile = getClass().getResource(EMPTY_DATA_FILE).getFile();
        File testArchive = new File(testFile);
        addTestFileToSourceDirectory(SRC_TERM_SERVER,testArchive);
        productInputFileDAO.putManifestFile(product, new FileInputStream(testManifest), testManifest.getName(), testManifest.length());
        SourceFileProcessingReport report =  productInputFileService.prepareInputFiles(product.getReleaseCenter().getBusinessKey(), product.getBusinessKey(), true);
        List<String> inputFileList = productInputFileService.listInputFilePaths(product.getReleaseCenter().getBusinessKey(), product.getBusinessKey());
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
        	assertTrue("must contain" + reportDetail.getFileName(),
        			Arrays.asList(filesReportedAsError).contains(reportDetail.getFileName()));
        }
        assertEquals(7, report.getDetails().get(ReportType.WARNING).size());
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
        productInputFileDAO.putManifestFile(product, new FileInputStream(testManifest), testManifest.getName(), testManifest.length());
        SourceFileProcessingReport report =  productInputFileService.prepareInputFiles(product.getReleaseCenter().getBusinessKey(), product.getBusinessKey(), true);
        List<String> inputFileList = productInputFileService.listInputFilePaths(product.getReleaseCenter().getBusinessKey(), product.getBusinessKey());
        printReport(report);
        String [] fileNames = {"rel2_Concept_Delta_DK1000005_20170731.txt",
        		"rel2_Description_Delta_DK1000005_20170731.txt",
        		"rel2_Refset_SimpleDelta_DK1000005_20170731.txt",
        		"rel2_StatedRelationship_Delta_DK1000005_20170731.txt",
        		"rel2_TextDefinition_Delta-da_DK1000005_20170731.txt",
        		"rel2_TextDefinition_Delta-en_DK1000005_20170731.txt",
        		"rel2_cRefset_AssociationReferenceDelta_DK1000005_20170731.txt",
        		"rel2_cRefset_AttributeValueDelta_DK1000005_20170731.txt"};
        assertEquals(fileNames.length, inputFileList.size());
        assertFileNameExist(inputFileList,fileNames);
        assertNotNull(report.getDetails().get(ReportType.ERROR));
        assertEquals(1,report.getDetails().get(ReportType.ERROR).size());
        assertNotNull(report.getDetails().get(ReportType.WARNING));
        assertEquals(7,report.getDetails().get(ReportType.WARNING).size());
    }


    @Test
    public void testPrepareInputFilesWithEmptyDataInTwoSources() throws ResourceNotFoundException, IOException, XPathExpressionException, ParserConfigurationException, SAXException, JAXBException, DecoderException, NoSuchAlgorithmException, BusinessServiceException {
        String testManifestFile = getClass().getResource("manifest_with_multiple_sources.xml").getFile();
        File testManifest = new File(testManifestFile);
        String testFile = getClass().getResource(EMPTY_DATA_FILE).getFile();
        File testArchive = new File(testFile);
        addTestFileToSourceDirectory(SRC_TERM_SERVER, testArchive);
        addTestFileToSourceDirectory(SRC_EXERTNALLY_MAINTAINED, new File(getClass().getResource("externally-maintained-empty-delta.zip").getFile()));
        productInputFileDAO.putManifestFile(product, new FileInputStream(testManifest), testManifest.getName(), testManifest.length());
        SourceFileProcessingReport report =  productInputFileService.prepareInputFiles(product.getReleaseCenter().getBusinessKey(), product.getBusinessKey(), true);
        List<String> inputFileList = productInputFileService.listInputFilePaths(product.getReleaseCenter().getBusinessKey(), product.getBusinessKey());
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
        assertEquals(6,report.getDetails().get(ReportType.WARNING).size());
    }

    @Test
    public void testPrepareInputFilesWithInvalidManifest() throws ResourceNotFoundException, IOException, JAXBException, DecoderException, NoSuchAlgorithmException, BusinessServiceException {
        String testManifestFile = getClass().getResource("invalid_manifest.xml").getFile();
        File testManifest = new File(testManifestFile);
        String testFile = getClass().getResource(EMPTY_DATA_FILE).getFile();
        File testArchive = new File(testFile);
        addTestFileToSourceDirectory(SRC_TERM_SERVER, testArchive);
        productInputFileDAO.putManifestFile(product, new FileInputStream(testManifest), testManifest.getName(), testManifest.length());
        SourceFileProcessingReport report =  productInputFileService.prepareInputFiles(product.getReleaseCenter().getBusinessKey(), product.getBusinessKey(), true);
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
        productInputFileDAO.putManifestFile(product, new FileInputStream(testManifest), testManifest.getName(), testManifest.length());
        SourceFileProcessingReport report =  productInputFileService.prepareInputFiles(product.getReleaseCenter().getBusinessKey(), product.getBusinessKey(), true);
        List<String> inputFileList = productInputFileService.listInputFilePaths(product.getReleaseCenter().getBusinessKey(), product.getBusinessKey());
        printReport(report);
        assertFileNameExist(inputFileList,"rel2_cRefset_AttributeValueDelta_INT_20170731.txt");
        assertFileNameExist(inputFileList,"rel2_cRefset_AssociationReferenceDelta_INT_20170731.txt");
        assertFileNameExist(inputFileList,"rel2_Description_Delta-en_INT_20170731.txt");
        assertFileNameExist(inputFileList,"rel2_TextDefinition_Delta-en_INT_20170731.txt");
        assertFileNameExist(inputFileList,"rel2_Refset_SimpleDelta_INT_20170731.txt");
        assertFileNameExist(inputFileList,"rel2_TextDefinition_Delta-en_INT_20170731.txt");
        assertFileNameExist(inputFileList,"rel2_cissccRefset_MRCMAttributeDomainDelta_INT_20170731.txt");
        assertEquals(7, inputFileList.size());
        InputStream inputStream = productInputFileService.getFileInputStream(product.getReleaseCenter().getBusinessKey(), product.getBusinessKey(), "rel2_Concept_Delta_INT_20170731.txt");
        assertNull(inputStream);
        inputStream = productInputFileService.getFileInputStream(product.getReleaseCenter().getBusinessKey(), product.getBusinessKey(), "rel2_Relationship_Delta_INT_20170731.txt");
        assertNotNull(inputStream);
        assertEquals(5, IOUtils.readLines(inputStream).size());
        assertNotNull(report.getDetails().get(ReportType.ERROR));
        assertEquals(27, report.getDetails().get(ReportType.ERROR).size());
        assertNotNull(report.getDetails().get(ReportType.WARNING));
        assertEquals(2, report.getDetails().get(ReportType.WARNING).size());
    }



    private void verifyFileProcessingInAllSources() throws ResourceNotFoundException, IOException {
        InputStream inputStream = productInputFileService.getFileInputStream(product.getReleaseCenter().getBusinessKey(), product.getBusinessKey(), "rel2_cRefset_AssociationReferenceDelta_INT_20140731.txt");
        assertNotNull(inputStream);
        assertEquals(9, IOUtils.readLines(inputStream).size());
        inputStream = productInputFileService.getFileInputStream(product.getReleaseCenter().getBusinessKey(), product.getBusinessKey(), "rel2_cRefset_AttributeValueDelta_INT_20140731.txt");
        assertNotNull(inputStream);
        assertEquals(3, IOUtils.readLines(inputStream).size());
        inputStream = productInputFileService.getFileInputStream(product.getReleaseCenter().getBusinessKey(), product.getBusinessKey(), "rel2_TextDefinition_Delta-en_INT_20140731.txt");
        assertNotNull(inputStream);
        assertEquals(2, IOUtils.readLines(inputStream).size());
        inputStream = productInputFileService.getFileInputStream(product.getReleaseCenter().getBusinessKey(), product.getBusinessKey(), "rel2_Description_Delta-en_INT_20140731.txt");
        assertNotNull(inputStream);
        assertEquals(2, IOUtils.readLines(inputStream).size());
    }

    private void verifyForFileProcessingInRestrictedSources() throws ResourceNotFoundException, IOException {
        InputStream inputStream = productInputFileService.getFileInputStream(product.getReleaseCenter().getBusinessKey(), product.getBusinessKey(), "rel2_cRefset_AssociationReferenceDelta_INT_20180731.txt");
        assertNotNull(inputStream);
        assertEquals(7, IOUtils.readLines(inputStream).size());
        inputStream = productInputFileService.getFileInputStream(product.getReleaseCenter().getBusinessKey(), product.getBusinessKey(), "rel2_cRefset_AttributeValueDelta_INT_20180731.txt");
        assertNotNull(inputStream);
        assertEquals(2, IOUtils.readLines(inputStream).size());
        inputStream = productInputFileService.getFileInputStream(product.getReleaseCenter().getBusinessKey(), product.getBusinessKey(), "rel2_TextDefinition_Delta-en_INT_20180731.txt");
        assertNotNull(inputStream);
        assertEquals(2, IOUtils.readLines(inputStream).size());
        inputStream = productInputFileService.getFileInputStream(product.getReleaseCenter().getBusinessKey(), product.getBusinessKey(), "rel2_Description_Delta-en_INT_20180731.txt");
        assertNotNull(inputStream);
        assertEquals(2, IOUtils.readLines(inputStream).size());
    }

    private void verifyResults() throws ResourceNotFoundException, IOException {
        List<String> inputFileList = productInputFileService.listInputFilePaths(product.getReleaseCenter().getBusinessKey(), product.getBusinessKey());
        assertEquals(6, inputFileList.size());
        InputStream inputStream = productInputFileService.getFileInputStream(product.getReleaseCenter().getBusinessKey(), product.getBusinessKey(), "rel2_cRefset_AssociationReferenceDelta_INT_20170731.txt");
        assertNotNull(inputStream);
        assertEquals(9, IOUtils.readLines(inputStream).size());
        inputStream = productInputFileService.getFileInputStream(product.getReleaseCenter().getBusinessKey(), product.getBusinessKey(), "rel2_cRefset_AttributeValueDelta_INT_20170731.txt");
        assertNotNull(inputStream);
        assertEquals(3, IOUtils.readLines(inputStream).size());
        inputStream = productInputFileService.getFileInputStream(product.getReleaseCenter().getBusinessKey(), product.getBusinessKey(), "rel2_TextDefinition_Delta-en_INT_20170731.txt");
        assertNotNull(inputStream);
        assertEquals(2, IOUtils.readLines(inputStream).size());
        inputStream = productInputFileService.getFileInputStream(product.getReleaseCenter().getBusinessKey(), product.getBusinessKey(), "rel2_Description_Delta-en_INT_20170731.txt");
        assertNotNull(inputStream);
        assertEquals(2, IOUtils.readLines(inputStream).size());

        //Unprocessed files
        inputStream = productInputFileService.getFileInputStream(product.getReleaseCenter().getBusinessKey(), product.getBusinessKey(), "rel2_Concept_Delta_INT_20170731.txt");
        assertNull(inputStream);
        inputStream = productInputFileService.getFileInputStream(product.getReleaseCenter().getBusinessKey(), product.getBusinessKey(), "rel2_Relationship_Delta_INT_20170731.txt");
        assertNotNull(inputStream);
        assertEquals(5, IOUtils.readLines(inputStream).size());
        inputStream = productInputFileService.getFileInputStream(product.getReleaseCenter().getBusinessKey(), product.getBusinessKey(), "rel2_Refset_SimpleDelta_INT_20170731.txt");
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

    protected void addEmptyFileToSourceDirectory(final String sourceName, final String filename) throws ResourceNotFoundException, IOException {
        final File tempFile = File.createTempFile("testTemp", ".txt");
        try (InputStream inputStream = new FileInputStream(tempFile)) {
            productInputFileService.putSourceFile(sourceName,product.getReleaseCenter().getBusinessKey(), product.getBusinessKey(), inputStream, filename, 0L);
        } finally {
            tempFile.delete();
        }
    }

    protected void addTestArchiveFileToSourceDirectory(final String sourceName) throws ResourceNotFoundException, IOException {
        InputStream inputStream = new FileInputStream(testArchive);
        productInputFileService.putSourceFile(sourceName, product.getReleaseCenter().getBusinessKey(), product.getBusinessKey(), inputStream, TEST_ARCHIVE_FILE, 0L);
    }

    protected void addTestFileToSourceDirectory(final String sourceName, File zipFile ) throws ResourceNotFoundException, IOException {
        InputStream inputStream = new FileInputStream(zipFile);
        productInputFileService.putSourceFile(sourceName, product.getReleaseCenter().getBusinessKey(), product.getBusinessKey(), inputStream, zipFile.getName(), 0L);
    }

    private void assertFileNameExist (List<String> inputFileList, String ...fileNames ) {
    	for (String name : fileNames) {
    		assertTrue("File name must exist:" + name, inputFileList.contains(name));
    	}
    }

    private void printReport(SourceFileProcessingReport report) {
    	if (isDebugRun) {
    		 System.out.println(report);
    	}
    }


}

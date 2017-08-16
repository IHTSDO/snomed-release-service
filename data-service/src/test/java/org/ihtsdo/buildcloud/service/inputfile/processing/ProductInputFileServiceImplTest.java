package org.ihtsdo.buildcloud.service.inputfile.processing;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.io.IOUtils;
import org.ihtsdo.buildcloud.dao.ProductDAO;
import org.ihtsdo.buildcloud.dao.ProductInputFileDAO;
import org.ihtsdo.buildcloud.entity.BuildConfiguration;
import org.ihtsdo.buildcloud.entity.Product;
import org.ihtsdo.buildcloud.entity.helper.TestEntityGenerator;
import org.ihtsdo.buildcloud.service.ProductInputFileService;
import org.ihtsdo.buildcloud.service.build.RF2Constants;
import org.ihtsdo.buildcloud.service.inputfile.prepare.FileProcessingReportDetail;
import org.ihtsdo.buildcloud.service.inputfile.prepare.SourceFileProcessingReport;
import org.ihtsdo.buildcloud.service.inputfile.prepare.ReportType;
import org.ihtsdo.buildcloud.service.security.SecurityHelper;
import org.ihtsdo.buildcloud.test.TestUtils;
import org.ihtsdo.otf.rest.exception.ResourceNotFoundException;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.*;
import java.security.NoSuchAlgorithmException;
import java.util.AbstractCollection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"/test/testDataServiceContext.xml"})
@Transactional
public class ProductInputFileServiceImplTest extends TestEntityGenerator{

    @Autowired
    protected ProductDAO productDAO;
    @Autowired
    private ProductInputFileService productInputFileService;
    @Autowired
    private ProductInputFileDAO productInputFileDAO;

    protected Product product;
    protected File testArchive;
    protected Set<String> subDirectories;

    private static final String JULY_RELEASE = "20140731";
    private static final String SRC_EXERTNALLY_MAINTAINED = "externally-maintained";
    private static final String SRC_MAPPING_TOOL = "mapping-tools";
    private static final String SRC_TERM_SERVER = "terminology-server";
    private static final String SRC_REFSET_TOOL = "reference-set-tool";
    private static final String TEST_ARCHIVE_FILE = "input_source_test_data.zip";
    private static final String TEST_MANIFEST_FILE = "manifest_all_sources.xml";
    private static final String TEST_MANIFEST_FILE_WITH_UNPROCESSED = "manifest_with_unprocessed.xml";
    private static final String TEST_MANIFEST_FILE_RESTRICTED = "manifest_restricted_sources.xml";
    private static final String TEST_MANIFEST_FILE_MISSING_REFSETS = "manifest_missing_refsets.xml";
	private static final String TEST_DK_MANIFEST_FILE = "manifest_dk.xml";
	private static final String TEST_SE_MANIFEST_FILE = "manifest_se_test.xml";
	private static final String EMPTY_DATA_FILE = "emptyDelta.zip";


    @Before
    public void setup() throws Exception {
        String testFile = getClass().getResource(TEST_ARCHIVE_FILE).getFile();
        testArchive = new File(testFile);
        SecurityHelper.setUser(TestUtils.TEST_USER);
        product = productDAO.find(1L, TestUtils.TEST_USER);
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
    public void testPrepareInputFilesInAllSources() throws ResourceNotFoundException, IOException, XPathExpressionException, ParserConfigurationException, SAXException, JAXBException, DecoderException, NoSuchAlgorithmException {
        String testManifestFile = getClass().getResource(TEST_MANIFEST_FILE).getFile();
        File testManifest = new File(testManifestFile);
        addTestArchiveFileToSourceDirectory(SRC_EXERTNALLY_MAINTAINED);
        addTestArchiveFileToSourceDirectory(SRC_REFSET_TOOL);
        addTestArchiveFileToSourceDirectory(SRC_TERM_SERVER);
        productInputFileDAO.putManifestFile(product, new FileInputStream(testManifest), testManifest.getName(), testManifest.length());
        productInputFileService.prepareInputFiles(product.getReleaseCenter().getBusinessKey(), product.getBusinessKey(), false);
        verifyFileProcessingInAllSources();
    }

    @Test
    public void testPrepareInputFilesInRestrictedSources() throws ResourceNotFoundException, IOException, XPathExpressionException, ParserConfigurationException, SAXException, JAXBException, DecoderException, NoSuchAlgorithmException {
        String testManifestFile = getClass().getResource(TEST_MANIFEST_FILE_RESTRICTED).getFile();
        File testManifest = new File(testManifestFile);
        addTestArchiveFileToSourceDirectory(SRC_MAPPING_TOOL);
        addTestArchiveFileToSourceDirectory(SRC_TERM_SERVER);
        productInputFileDAO.putManifestFile(product, new FileInputStream(testManifest), testManifest.getName(), testManifest.length());
        productInputFileService.prepareInputFiles(product.getReleaseCenter().getBusinessKey(), product.getBusinessKey(), false);
        InputStream report = productInputFileDAO.getInputPrepareReport(product);
        StringWriter writer = new StringWriter();
        IOUtils.copy(report, writer, "UTF-8");
        System.out.println(writer.toString());
        verifyForFileProcessingInRestrictedSources();
    }

    @Test
    public void testPrepareInputFilesMissingRefsets() throws ResourceNotFoundException, IOException, NoSuchAlgorithmException, JAXBException, DecoderException {
        String testManifestFile = getClass().getResource(TEST_MANIFEST_FILE_MISSING_REFSETS).getFile();
        File testManifest = new File(testManifestFile);
        addTestArchiveFileToSourceDirectory(SRC_REFSET_TOOL);
        productInputFileDAO.putManifestFile(product, new FileInputStream(testManifest), testManifest.getName(), testManifest.length());
        SourceFileProcessingReport fileProcessingReport = productInputFileService.prepareInputFiles(product.getReleaseCenter().getBusinessKey(), product.getBusinessKey(), false);
        verifyForFileProcessingMissingRefsets(fileProcessingReport);
    }

    @Test
    public void testPrepareInputFilesWithTerminologyFiles() throws ResourceNotFoundException, IOException, NoSuchAlgorithmException, JAXBException, DecoderException {
        String testManifestFile = getClass().getResource(TEST_MANIFEST_FILE_WITH_UNPROCESSED).getFile();
        File testManifest = new File(testManifestFile);
        addTestArchiveFileToSourceDirectory(SRC_TERM_SERVER);
        addEmptyFileToSourceDirectory(SRC_EXERTNALLY_MAINTAINED,"sct2_Concept_Delta_INT_20170731.txt");
        productInputFileDAO.putManifestFile(product, new FileInputStream(testManifest), testManifest.getName(), testManifest.length());
        SourceFileProcessingReport report = productInputFileService.prepareInputFiles(product.getReleaseCenter().getBusinessKey(), product.getBusinessKey(), true);
        System.out.println(report);
        verifyResults();
    }
    
    @Test
    public void testPrepareFilesForDK_Extension() throws Exception {
    	 String testManifestFile = getClass().getResource(TEST_DK_MANIFEST_FILE).getFile();
         File testManifest = new File(testManifestFile);
         addTestArchiveFileToSourceDirectory(SRC_TERM_SERVER);
         productInputFileDAO.putManifestFile(product, new FileInputStream(testManifest), testManifest.getName(), testManifest.length());
         SourceFileProcessingReport report = productInputFileService.prepareInputFiles(product.getReleaseCenter().getBusinessKey(), product.getBusinessKey(), true);
         System.out.println(report);
         List<String> inputFileList = productInputFileService.listInputFilePaths(product.getReleaseCenter().getBusinessKey(), product.getBusinessKey());
         assertEquals(7, inputFileList.size());
    }
    
    
    @Test
    public void testPrepareFilesForSE_Extension() throws Exception {
    	 String testManifestFile = getClass().getResource(TEST_SE_MANIFEST_FILE).getFile();
         File testManifest = new File(testManifestFile);
         addTestArchiveFileToSourceDirectory(SRC_TERM_SERVER);
         addTestArchiveFileToSourceDirectory(SRC_EXERTNALLY_MAINTAINED);
         productInputFileDAO.putManifestFile(product, new FileInputStream(testManifest), testManifest.getName(), testManifest.length());
         SourceFileProcessingReport report = productInputFileService.prepareInputFiles(product.getReleaseCenter().getBusinessKey(), product.getBusinessKey(), true);
         List<String> inputFileList = productInputFileService.listInputFilePaths(product.getReleaseCenter().getBusinessKey(), product.getBusinessKey());
         System.out.println(report);
         assertFileNameExist(inputFileList,"rel2_TextDefinition_Delta-sv_SE1000052_20170531.txt");
         assertFileNameExist(inputFileList,"rel2_TextDefinition_Delta-en_SE1000052_20170531.txt");
         assertFileNameExist(inputFileList,"rel2_Description_Delta-sv_SE1000052_20170531.txt");
         assertFileNameExist(inputFileList,"rel2_Description_Delta-en_SE1000052_20170531.txt");
         assertFileNameExist(inputFileList,"rel2_cRefset_AttributeValueDelta_SE1000052_20170531.txt");
         assertFileNameExist(inputFileList,"rel2_cRefset_AssociationReferenceDelta_SE1000052_20170531.txt");
         assertEquals(23, report.getDetails().get(ReportType.WARNING).size());
         
    }
    
    @Test
    public void testPrepareInputFilesWithEmptyData() throws ResourceNotFoundException, IOException, XPathExpressionException, ParserConfigurationException, SAXException, JAXBException, DecoderException, NoSuchAlgorithmException {
        String testManifestFile = getClass().getResource("manifest_with_additonal_fields.xml").getFile();
        File testManifest = new File(testManifestFile);
        String testFile = getClass().getResource(EMPTY_DATA_FILE).getFile();
        File testArchive = new File(testFile);
        addTestFileToSourceDirectory(SRC_TERM_SERVER,testArchive);
        productInputFileDAO.putManifestFile(product, new FileInputStream(testManifest), testManifest.getName(), testManifest.length());
        SourceFileProcessingReport report =  productInputFileService.prepareInputFiles(product.getReleaseCenter().getBusinessKey(), product.getBusinessKey(), true);
        List<String> inputFileList = productInputFileService.listInputFilePaths(product.getReleaseCenter().getBusinessKey(), product.getBusinessKey());
        System.out.println(report);
        assertFileNameExist(inputFileList,"rel2_cRefset_AttributeValueDelta_INT_20140731.txt");
        assertFileNameExist(inputFileList,"rel2_cRefset_AssociationReferenceDelta_INT_20140731.txt");
        assertFileNameExist(inputFileList,"rel2_Description_Delta-en_INT_20140731.txt");
        assertFileNameExist(inputFileList,"rel2_TextDefinition_Delta-en_INT_20140731.txt");
        assertFileNameExist(inputFileList,"rel2_Refset_SimpleDelta_INT_20140731.txt");
        assertEquals(5, inputFileList.size());
        assertEquals(13, report.getDetails().get(ReportType.WARNING).size());
        assertEquals(8, report.getDetails().get(ReportType.INFO).size());
        assertNull(report.getDetails().get(ReportType.ERROR));
    }
    
    
    
    @Test
    public void testPrepareInputFilesWithEmptyDataInTwoSources() throws ResourceNotFoundException, IOException, XPathExpressionException, ParserConfigurationException, SAXException, JAXBException, DecoderException, NoSuchAlgorithmException {
        String testManifestFile = getClass().getResource("manifest_with_multiple_sources.xml").getFile();
        File testManifest = new File(testManifestFile);
        String testFile = getClass().getResource(EMPTY_DATA_FILE).getFile();
        File testArchive = new File(testFile);
        addTestFileToSourceDirectory(SRC_TERM_SERVER, testArchive);
        addTestFileToSourceDirectory(SRC_EXERTNALLY_MAINTAINED, new File(getClass().getResource("externally-maintained-empty-delta.zip").getFile()));
        productInputFileDAO.putManifestFile(product, new FileInputStream(testManifest), testManifest.getName(), testManifest.length());
        SourceFileProcessingReport report =  productInputFileService.prepareInputFiles(product.getReleaseCenter().getBusinessKey(), product.getBusinessKey(), true);
        List<String> inputFileList = productInputFileService.listInputFilePaths(product.getReleaseCenter().getBusinessKey(), product.getBusinessKey());
        System.out.println(report);
        String [] fileNames = {"rel2_cRefset_AttributeValueDelta_INT_20170731.txt",
        		"rel2_Description_Delta-en_INT_20170731.txt",
        		"rel2_TextDefinition_Delta-en_INT_20170731.txt",
        		"rel2_Concept_Delta_INT_20170731.txt",
        		"rel2_cRefset_AssociationReferenceDelta_INT_20170731.txt",
        		"rel2_Identifier_Delta_INT_20170731.txt",
        		"rel2_TextDefinition_Delta-en_INT_20170731.txt",
        		"rel2_Refset_SimpleDelta_INT_20170731.txt",
        		"rel2_StatedRelationship_Delta_INT_20170731.txt",
        		"rel2_ssRefset_ModuleDependencyDelta_INT_20170731.txt",
        		"rel2_cissccRefset_MRCMAttributeDomainDelta_INT_20170731.txt"};
        assertFileNameExist(inputFileList,fileNames);
        assertNull(report.getDetails().get(ReportType.ERROR));
    }
    
    @Test
    public void testPrepareInputFilesWithMultipleSources() throws ResourceNotFoundException, IOException, XPathExpressionException, ParserConfigurationException, SAXException, JAXBException, DecoderException, NoSuchAlgorithmException {
        String testManifestFile = getClass().getResource("manifest_with_multiple_sources.xml").getFile();
        File testManifest = new File(testManifestFile);
        addTestFileToSourceDirectory(SRC_TERM_SERVER, new File(getClass().getResource(TEST_ARCHIVE_FILE).getFile()));
        addTestFileToSourceDirectory(SRC_EXERTNALLY_MAINTAINED, new File(getClass().getResource(TEST_ARCHIVE_FILE).getFile()));
        productInputFileDAO.putManifestFile(product, new FileInputStream(testManifest), testManifest.getName(), testManifest.length());
        SourceFileProcessingReport report =  productInputFileService.prepareInputFiles(product.getReleaseCenter().getBusinessKey(), product.getBusinessKey(), true);
        List<String> inputFileList = productInputFileService.listInputFilePaths(product.getReleaseCenter().getBusinessKey(), product.getBusinessKey());
        System.out.println(report);
        
        assertFileNameExist(inputFileList,"rel2_cRefset_AttributeValueDelta_INT_20170731.txt");
        assertFileNameExist(inputFileList,"rel2_cRefset_AssociationReferenceDelta_INT_20170731.txt");
        assertFileNameExist(inputFileList,"rel2_Description_Delta-en_INT_20170731.txt");
        assertFileNameExist(inputFileList,"rel2_TextDefinition_Delta-en_INT_20170731.txt");
        assertFileNameExist(inputFileList,"rel2_Concept_Delta_INT_20170731.txt");
        assertFileNameExist(inputFileList,"rel2_Refset_SimpleDelta_INT_20170731.txt");
        assertFileNameExist(inputFileList,"rel2_TextDefinition_Delta-en_INT_20170731.txt");
        assertFileNameExist(inputFileList,"rel2_cissccRefset_MRCMAttributeDomainDelta_INT_20170731.txt");
        assertEquals(8, inputFileList.size());
        InputStream inputStream = productInputFileService.getFileInputStream(product.getReleaseCenter().getBusinessKey(), product.getBusinessKey(), "rel2_Concept_Delta_INT_20170731.txt");
        assertNotNull(inputStream);
        assertEquals(7, IOUtils.readLines(inputStream).size());
        inputStream = productInputFileService.getFileInputStream(product.getReleaseCenter().getBusinessKey(), product.getBusinessKey(), "rel2_Relationship_Delta_INT_20170731.txt");
        assertNotNull(inputStream);
        assertEquals(5, IOUtils.readLines(inputStream).size());
        assertEquals(12, report.getDetails().get(ReportType.WARNING).size());
        assertEquals(34, report.getDetails().get(ReportType.INFO).size());
        assertNull(report.getDetails().get(ReportType.ERROR));
    }
    


    private void verifyFileProcessingInAllSources() throws ResourceNotFoundException, IOException {
        InputStream inputStream = productInputFileService.getFileInputStream(product.getReleaseCenter().getBusinessKey(), product.getBusinessKey(), "rel2_cRefset_AssociationReferenceDelta_INT_20140731.txt");
        assertNotNull(inputStream);
        assertEquals(25, IOUtils.readLines(inputStream).size());
        inputStream = productInputFileService.getFileInputStream(product.getReleaseCenter().getBusinessKey(), product.getBusinessKey(), "rel2_cRefset_AttributeValueDelta_INT_20140731.txt");
        assertNotNull(inputStream);
        assertEquals(7, IOUtils.readLines(inputStream).size());
        inputStream = productInputFileService.getFileInputStream(product.getReleaseCenter().getBusinessKey(), product.getBusinessKey(), "rel2_TextDefinition_Delta-en_INT_20140731.txt");
        assertNotNull(inputStream);
        assertEquals(4, IOUtils.readLines(inputStream).size());
        inputStream = productInputFileService.getFileInputStream(product.getReleaseCenter().getBusinessKey(), product.getBusinessKey(), "rel2_Description_Delta-en_INT_20140731.txt");
        assertNotNull(inputStream);
        assertEquals(4, IOUtils.readLines(inputStream).size());
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
        assertEquals(3, IOUtils.readLines(inputStream).size());
        inputStream = productInputFileService.getFileInputStream(product.getReleaseCenter().getBusinessKey(), product.getBusinessKey(), "rel2_Description_Delta-en_INT_20180731.txt");
        assertNotNull(inputStream);
        assertEquals(3, IOUtils.readLines(inputStream).size());
    }

    private void verifyResults() throws ResourceNotFoundException, IOException {
        List<String> inputFileList = productInputFileService.listInputFilePaths(product.getReleaseCenter().getBusinessKey(), product.getBusinessKey());
        assertEquals(7, inputFileList.size());
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
        assertNotNull(inputStream);
        assertEquals(4, IOUtils.readLines(inputStream).size());
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
}

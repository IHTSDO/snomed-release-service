package org.ihtsdo.buildcloud.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.io.IOUtils;
import org.ihtsdo.buildcloud.dao.ProductDAO;
import org.ihtsdo.buildcloud.dao.ProductInputFileDAO;
import org.ihtsdo.buildcloud.entity.BuildConfiguration;
import org.ihtsdo.buildcloud.entity.Product;
import org.ihtsdo.buildcloud.entity.helper.TestEntityGenerator;
import org.ihtsdo.buildcloud.service.build.RF2Constants;
import org.ihtsdo.buildcloud.service.fileprocessing.FileProcessingReport;
import org.ihtsdo.buildcloud.service.fileprocessing.FileProcessingReportDetail;
import org.ihtsdo.buildcloud.service.fileprocessing.FileProcessingReportType;
import org.ihtsdo.buildcloud.service.security.SecurityHelper;
import org.ihtsdo.buildcloud.test.TestUtils;
import org.ihtsdo.otf.rest.exception.ResourceNotFoundException;
import org.junit.After;
import org.junit.Assert;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * User: huyle
 * Date: 5/25/2017
 * Time: 9:42 AM
 */
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
    private static final String SRC_MANUAL = "manual";
    private static final String SRC_MAPPING_TOOL = "mapping-tools";
    private static final String SRC_TERM_SERVER = "terminology-server";
    private static final String SRC_REFSET_TOOL = "reference-set-tool";
    private static final String TEST_ARCHIVE_FILE = "org/ihtsdo/buildcloud/service/fileprocessing/file_processing.zip";
    private static final String TEST_MANIFEST_FILE = "org/ihtsdo/buildcloud/service/fileprocessing/delta/manifest_all_sources.xml";
    private static final String TEST_MANIFEST_FILE_RESTRICTED = "org/ihtsdo/buildcloud/service/fileprocessing/delta/manifest_restricted_sources.xml";
    private static final String TEST_MANIFEST_FILE_MISSING_REFSETS = "org/ihtsdo/buildcloud/service/fileprocessing/delta/manifest_missing_refsets.xml";


    @Before
    public void setup() throws Exception {
        String testFile = getClass().getResource("/" + TEST_ARCHIVE_FILE).getFile();
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
        subDirectories.add(SRC_MANUAL);
        subDirectories.add(SRC_MAPPING_TOOL);
        subDirectories.add(SRC_TERM_SERVER);
        subDirectories.add(SRC_REFSET_TOOL);
    }

    @After
    public void tearDown() throws ResourceNotFoundException {
        productInputFileService.deleteSourceFilesByPattern(product.getReleaseCenter().getBusinessKey(), product.getBusinessKey(), "*.txt", null);
    }


    @Test
    public void testPutSourceFile() throws IOException, ResourceNotFoundException {
        addTestArchiveFileToSourceDirectory(SRC_MANUAL);
        List<String> fileList = productInputFileService.listSourceFilePaths(product.getReleaseCenter().getBusinessKey(), product.getBusinessKey());
        Assert.assertTrue(fileList.size() > 0);
    }

    @Test
    public void listSourceFilePaths() throws ResourceNotFoundException, IOException {
        addEmptyFileToSourceDirectory(SRC_MANUAL, "test1.txt");
        addEmptyFileToSourceDirectory(SRC_MANUAL, "test2.txt");
        addEmptyFileToSourceDirectory(SRC_MAPPING_TOOL, "test3.txt");
        addEmptyFileToSourceDirectory(SRC_TERM_SERVER, "test4.txt");
        addEmptyFileToSourceDirectory(SRC_REFSET_TOOL, "test5.txt");
        List<String> fileList = productInputFileService.listSourceFilePaths(product.getReleaseCenter().getBusinessKey(), product.getBusinessKey());
        Assert.assertEquals(5, fileList.size());
    }

    @Test
    public void listSourceFilePathsInSubDirectories() throws ResourceNotFoundException, IOException {
        addEmptyFileToSourceDirectory(SRC_MANUAL, "test1.txt");
        addEmptyFileToSourceDirectory(SRC_MANUAL, "test2.txt");
        addEmptyFileToSourceDirectory(SRC_MAPPING_TOOL, "test3.txt");
        addEmptyFileToSourceDirectory(SRC_TERM_SERVER, "test4.txt");
        addEmptyFileToSourceDirectory(SRC_REFSET_TOOL, "test5.txt");
        Set<String> subs = new HashSet<>();
        subs.add(SRC_MANUAL);
        subs.add(SRC_MAPPING_TOOL);
        List<String> fileList = productInputFileService.listSourceFilePathsFromSubDirectories(product.getReleaseCenter().getBusinessKey(), product.getBusinessKey(), subs);
        Assert.assertEquals(3, fileList.size());
    }

    @Test
    public void testPrepareInputFilesInAllSources() throws ResourceNotFoundException, IOException, XPathExpressionException, ParserConfigurationException, SAXException, JAXBException, DecoderException, NoSuchAlgorithmException {
        String testManifestFile = getClass().getResource("/" + TEST_MANIFEST_FILE).getFile();
        File testManifest = new File(testManifestFile);
        addTestArchiveFileToSourceDirectory(SRC_MANUAL);
        addTestArchiveFileToSourceDirectory(SRC_REFSET_TOOL);
        addTestArchiveFileToSourceDirectory(SRC_TERM_SERVER);
        productInputFileDAO.putManifestFile(product, new FileInputStream(testManifest), testManifest.getName(), testManifest.length());
        productInputFileService.prepareInputFiles(product.getReleaseCenter().getBusinessKey(), product.getBusinessKey(), false);
        doAssertionForFileProcessingInAllSources();
    }

    @Test
    public void testPrepareInputFilesInRestrictedSources() throws ResourceNotFoundException, IOException, XPathExpressionException, ParserConfigurationException, SAXException, JAXBException, DecoderException, NoSuchAlgorithmException {
        String testManifestFile = getClass().getResource("/" + TEST_MANIFEST_FILE_RESTRICTED).getFile();
        File testManifest = new File(testManifestFile);
        addTestArchiveFileToSourceDirectory(SRC_MAPPING_TOOL);
        //addTestArchiveFileToSourceDirectory(SRC_REFSET_TOOL);
        addTestArchiveFileToSourceDirectory(SRC_TERM_SERVER);
        productInputFileDAO.putManifestFile(product, new FileInputStream(testManifest), testManifest.getName(), testManifest.length());
        productInputFileService.prepareInputFiles(product.getReleaseCenter().getBusinessKey(), product.getBusinessKey(), false);
        InputStream report = productInputFileDAO.getInputPrepareReport(product);
        StringWriter writer = new StringWriter();
        IOUtils.copy(report, writer, "UTF-8");
        String theString = writer.toString();
        ObjectMapper objectMapper = new ObjectMapper();
        FileProcessingReport fileProcessingReport = objectMapper.readValue(theString, FileProcessingReport.class);
        doAssertionForFileProcessingInRestrictedSources();
    }

    @Test
    public void testPrepareInputFilesMissingRefsets() throws ResourceNotFoundException, IOException, NoSuchAlgorithmException, JAXBException, DecoderException {
        String testManifestFile = getClass().getResource("/" + TEST_MANIFEST_FILE_MISSING_REFSETS).getFile();
        File testManifest = new File(testManifestFile);
        addTestArchiveFileToSourceDirectory(SRC_REFSET_TOOL);
        productInputFileDAO.putManifestFile(product, new FileInputStream(testManifest), testManifest.getName(), testManifest.length());
        FileProcessingReport fileProcessingReport = productInputFileService.prepareInputFiles(product.getReleaseCenter().getBusinessKey(), product.getBusinessKey(), false);
        doAssertionForFileProcessingMissingRefsets(fileProcessingReport);
    }


    private void doAssertionForFileProcessingInAllSources() throws ResourceNotFoundException, IOException {
        InputStream inputStream = productInputFileService.getFileInputStream(product.getReleaseCenter().getBusinessKey(), product.getBusinessKey(), "xder2_cRefset_AssociationReferenceDelta_INT_20170731.txt");
        Assert.assertNotNull(inputStream);
        Assert.assertEquals(25, IOUtils.readLines(inputStream).size());
        inputStream = productInputFileService.getFileInputStream(product.getReleaseCenter().getBusinessKey(), product.getBusinessKey(), "xder2_cRefset_AttributeValueDelta_INT_20170731.txt");
        Assert.assertNotNull(inputStream);
        Assert.assertEquals(7, IOUtils.readLines(inputStream).size());
        inputStream = productInputFileService.getFileInputStream(product.getReleaseCenter().getBusinessKey(), product.getBusinessKey(), "xsct2_TextDefinition_Delta-en_INT_20170731.txt");
        Assert.assertNotNull(inputStream);
        Assert.assertEquals(4, IOUtils.readLines(inputStream).size());
        inputStream = productInputFileService.getFileInputStream(product.getReleaseCenter().getBusinessKey(), product.getBusinessKey(), "xsct2_Description_Delta-en_INT_20170731.txt");
        Assert.assertNotNull(inputStream);
        Assert.assertEquals(4, IOUtils.readLines(inputStream).size());
    }

    private void doAssertionForFileProcessingInRestrictedSources() throws ResourceNotFoundException, IOException {
        InputStream inputStream = productInputFileService.getFileInputStream(product.getReleaseCenter().getBusinessKey(), product.getBusinessKey(), "xder2_cRefset_AssociationReferenceDelta_INT_20180731.txt");
        Assert.assertNotNull(inputStream);
        Assert.assertEquals(7, IOUtils.readLines(inputStream).size());
        inputStream = productInputFileService.getFileInputStream(product.getReleaseCenter().getBusinessKey(), product.getBusinessKey(), "xder2_cRefset_AttributeValueDelta_INT_20180731.txt");
        Assert.assertNotNull(inputStream);
        Assert.assertEquals(2, IOUtils.readLines(inputStream).size());
        inputStream = productInputFileService.getFileInputStream(product.getReleaseCenter().getBusinessKey(), product.getBusinessKey(), "xsct2_TextDefinition_Delta-en_INT_20180731.txt");
        Assert.assertNotNull(inputStream);
        Assert.assertEquals(3, IOUtils.readLines(inputStream).size());
        inputStream = productInputFileService.getFileInputStream(product.getReleaseCenter().getBusinessKey(), product.getBusinessKey(), "xsct2_Description_Delta-en_INT_20180731.txt");
        Assert.assertNotNull(inputStream);
        Assert.assertEquals(3, IOUtils.readLines(inputStream).size());
    }

    private void doAssertionForFileProcessingMissingRefsets(FileProcessingReport fileProcessingReport) {
        String[] missingRefsets = {"900000000000523009","900000000000531004","900000000000489007"};
        int countMissingRefset = 0;
        Map<String,List<FileProcessingReportDetail>> reportDetails = fileProcessingReport.getDetails();
        //for (FileProcessingReportDetail reportDetail : reportDetails) {
            if(reportDetails.containsKey(FileProcessingReportType.WARNING)) {
                for (FileProcessingReportDetail reportDetail : reportDetails.get(FileProcessingReportType.WARNING)) {
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
        Assert.assertEquals(3, countMissingRefset);
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




}

package org.ihtsdo.buildcloud.service.srs;

import com.google.common.io.Files;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.CharEncoding;
import org.ihtsdo.buildcloud.dao.helper.BuildS3PathHelper;
import org.ihtsdo.buildcloud.entity.Product;
import org.ihtsdo.buildcloud.manifest.FileType;
import org.ihtsdo.buildcloud.manifest.FolderType;
import org.ihtsdo.buildcloud.manifest.ListingType;
import org.ihtsdo.buildcloud.manifest.RefsetType;
import org.ihtsdo.buildcloud.service.file.ManifestXmlFileParser;
import org.ihtsdo.otf.dao.s3.helper.FileHelper;
import org.ihtsdo.otf.rest.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.bind.Binder;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: huyle
 * Date: 5/24/2017
 * Time: 10:56 AM
 */
public class FileProcessor {

    private final Logger logger = LoggerFactory.getLogger(FileProcessor.class);

    private InputStream manifestStream;
    private FileHelper fileHelper;
    private BuildS3PathHelper buildS3PathHelper;
    private Product product;
    private File localDir;
    private List<File> processedFiles = new ArrayList<>();
    
    private List<RefsetProcessingConfig> refsetProcessingConfigs;
    private Map<String, List<String>> refsetSourceFileMaps;
    private Map<String, List<String>> descriptionSourceFileMaps;

    private static final String FILE_EXTENSION_TXT = "txt";
    private static final String HEADER_REFSETS = "id\teffectiveTime\tactive\tmoduleId\trefsetId\treferencedComponentId\ttargetComponent";
    private static final String HEADER_TERM_DESCRIPTION = "id\teffectiveTime\tactive\tmoduleId\tconceptId\tlanguageCode\ttypeId\tterm\tcaseSignificanceId";
    private static final int REFSETID_COL = 4;
    private static final int DESCRIPTION_COL = 5;
    private static final String INPUT_FILE_TYPE_REFSET = "Refset";
    private static final String INPUT_FILE_TYPE_DESCRIPTION = "Description";

    static Map<String, InputFileConfig> fileTypesHeaders;
    static {
        fileTypesHeaders = new HashMap<>();
        fileTypesHeaders.put(INPUT_FILE_TYPE_REFSET, new InputFileConfig(INPUT_FILE_TYPE_REFSET, HEADER_REFSETS, REFSETID_COL));
        fileTypesHeaders.put(INPUT_FILE_TYPE_DESCRIPTION, new InputFileConfig(INPUT_FILE_TYPE_DESCRIPTION, HEADER_TERM_DESCRIPTION, DESCRIPTION_COL));
    }

    public FileProcessor(InputStream manifestStream, FileHelper fileHelper, BuildS3PathHelper buildS3PathHelper, Product product) {
        this.manifestStream = manifestStream;
        this.fileHelper = fileHelper;
        this.buildS3PathHelper = buildS3PathHelper;
        this.product = product;
        this.refsetSourceFileMaps = new HashMap<>();
        this.descriptionSourceFileMaps = new HashMap<>();
        this.refsetProcessingConfigs = new ArrayList<>();
        this.processedFiles = new ArrayList<>();
    }


   public void processFiles(List<String> sourceFileLists) throws IOException, JAXBException, ResourceNotFoundException {
        try{
            loadSources(sourceFileLists);
            processManifest();
        } finally {
            if(!FileUtils.deleteQuietly(localDir)) {
                logger.warn("Failed to delete local directory {}", localDir.getAbsolutePath());
            }
        }
   }


   private File loadSources(List<String> sourceFileLists) throws IOException {
       localDir = Files.createTempDir();
       for (String sourceFilePath : sourceFileLists) {
           if(FilenameUtils.getExtension(sourceFilePath).equalsIgnoreCase(FILE_EXTENSION_TXT)) {

               //Copy files from S3 to local for processing
               InputStream sourceFileStream = fileHelper.getFileStream(buildS3PathHelper.getProductSourcesPath(product).append(sourceFilePath).toString());
               String sourceName = sourceFilePath.substring(0,sourceFilePath.indexOf("/"));
               File sourceDir = new File(localDir, sourceName);
               if(!sourceDir.exists()) sourceDir.mkdir();
               String fileName = FilenameUtils.getName(sourceFilePath);
               File outFile = new File(localDir + "/" + sourceName, fileName);
               OutputStream outputStream = new FileOutputStream(outFile);
               IOUtils.copy(sourceFileStream, outputStream);
               IOUtils.closeQuietly(outputStream);
               logger.info("Successfully created temp source file {}", outFile.getAbsolutePath());
               List<String> lines = FileUtils.readLines(outFile, CharEncoding.UTF_8);

               //Check whether current file is Refset or Description as we only want to process those files
               if(lines != null && !lines.isEmpty()) {
                   if(lines.get(0).equalsIgnoreCase(HEADER_REFSETS)) {
                       if(!refsetSourceFileMaps.containsKey(sourceName)) {
                           refsetSourceFileMaps.put(sourceName, new ArrayList<String>());
                       }
                       refsetSourceFileMaps.get(sourceName).add(outFile.getAbsolutePath());
                       logger.info("Found Refset file {}", outFile.getAbsolutePath());
                   } else if(lines.get(0).equalsIgnoreCase(HEADER_TERM_DESCRIPTION)){
                       if(!descriptionSourceFileMaps.containsKey(sourceName)) {
                           descriptionSourceFileMaps.put(sourceName, new ArrayList<String>());
                       }
                       descriptionSourceFileMaps.get(sourceName).add(outFile.getAbsolutePath());
                       logger.info("Found Description file {}", outFile.getAbsolutePath());
                   }
               }
           }
       }
       return localDir;
   }

   private void processRefsetFiles() {
       for (RefsetProcessingConfig refsetProcessingConfig : refsetProcessingConfigs) {
           String targetFileName = refsetProcessingConfig.getTargetFile();
           List<String> filesToProcesses = new ArrayList<>();
           for (RefsetType refsetType : refsetProcessingConfig.getRefsets()) {
               if(refsetType.getSources() != null && refsetType.getSources().getSource() != null && !refsetType.getSources().getSource().isEmpty()) {
                   for (String source : refsetType.getSources().getSource()) {
                       if(refsetSourceFileMaps.containsKey(source)) {
                           filesToProcesses.addAll(refsetSourceFileMaps.get(source));
                       } else {
                           logger.error("Could not find source {} to process refset {}", source, targetFileName);
                       }
                   }
               } else {
                   for (List<String> fileList : refsetSourceFileMaps.values()) {
                       filesToProcesses.addAll(fileList);
                   }
               }
           }

       }
   }

    private void processFiles(File resultDir, String targetFileName, String matchingValue, String processedFileType) {
        if(processedFileType.equalsIgnoreCase(INPUT_FILE_TYPE_REFSET)) {
            
        } else if(processedFileType.equalsIgnoreCase(INPUT_FILE_TYPE_DESCRIPTION)) {
            
        }
        File targetFile = new File(resultDir, targetFileName);


    }

    private void doProcessFiles(File extractDir, String sourceFileName, String matchingValue, String targetFileName, String fileType) throws IOException {
        if(!sourceFileName.equalsIgnoreCase(targetFileName) &&sourceFileName.endsWith(FILE_EXTENSION_TXT)) {
            File sourceFile = new File(extractDir, sourceFileName);
            List<String> lines = FileUtils.readLines(sourceFile, CharEncoding.UTF_8);
            if(lines.get(0).equalsIgnoreCase(fileTypesHeaders.get(fileType).getHeader())) {
                File targetFile = new File(extractDir, targetFileName);
                List<String> copiedLines = new ArrayList<>();
                if(!targetFile.exists()) {
                    boolean createFile = targetFile.createNewFile();
                    if(createFile) {
                        logger.info("Create new {} file {}", fileType, targetFile.getAbsolutePath());
                    } else {
                        logger.error("Failed to create new refset file {}", targetFileName);
                    }
                    copiedLines.add(lines.get(0));
                }
                for (String line : lines) {
                    String[] splits = line.split("\t");
                    if(matchingValue.equals(splits[fileTypesHeaders.get(fileType).getProcessedColumn()])) copiedLines.add(line);
                }
                FileUtils.writeLines(targetFile, copiedLines, CharEncoding.UTF_8);
            }
        }
    }

    public void processManifest() throws JAXBException, ResourceNotFoundException {
        ManifestXmlFileParser manifestXmlFileParser = new ManifestXmlFileParser();
        ListingType listingType = manifestXmlFileParser.parse(manifestStream);
        listAllFiles(listingType);
        System.out.println("test");

    }

    public List<String> listAllFiles(ListingType listingType) {
        FolderType rootFolder = listingType.getFolder();
        List<String> result = new ArrayList<>();
        getFilesFromCurrentAndSubFolders(rootFolder, result);
        return result;
    }

    private void getFilesFromCurrentAndSubFolders(FolderType folder, List<String> filesList) {
        if (folder != null) {
            if (folder.getFile() != null) {
                for (FileType fileType : folder.getFile()) {
                    if(fileType.getContainsReferenceSets() != null) {
                        RefsetProcessingConfig refsetProcessingConfig = new RefsetProcessingConfig(fileType.getName(), fileType.getContainsReferenceSets().getRefset());
                        refsetProcessingConfigs.add(refsetProcessingConfig);
                    }
                    filesList.add(fileType.getName());
                }
            }
            if (folder.getFolder() != null) {
                for (FolderType subFolder : folder.getFolder()) {
                    getFilesFromCurrentAndSubFolders(subFolder, filesList);
                }
            }
        }
    }



    



}

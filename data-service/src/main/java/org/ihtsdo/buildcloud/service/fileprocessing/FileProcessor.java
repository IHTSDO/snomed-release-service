package org.ihtsdo.buildcloud.service.fileprocessing;

import com.google.common.io.Files;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.CharEncoding;
import org.ihtsdo.buildcloud.dao.helper.BuildS3PathHelper;
import org.ihtsdo.buildcloud.entity.Product;
import org.ihtsdo.buildcloud.manifest.FileType;
import org.ihtsdo.buildcloud.manifest.FolderType;
import org.ihtsdo.buildcloud.manifest.ListingType;
import org.ihtsdo.buildcloud.manifest.RefsetType;
import org.ihtsdo.buildcloud.service.build.RF2Constants;
import org.ihtsdo.buildcloud.service.file.ManifestXmlFileParser;
import org.ihtsdo.otf.dao.s3.helper.FileHelper;
import org.ihtsdo.otf.rest.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.util.*;

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
    private File outDir;
    private boolean foundTextDefinitionFile = false;
    private boolean copyFilesDefinedInManifest = false;

    private Set<String> availableSources;
    private Map<String, FileProcessingConfig> refsetFileProcessingConfigs;
    private Map<String, FileProcessingConfig> descriptionFileProcessingConfigs;
    private Map<String, FileProcessingConfig> textDefinitionFileProcessingConfigs;
    private Map<String, String> filesToCopy;
    private Map<String, List<String>> sourceFilesMap;
    private Map<String, Map<String, List<String>>> refSetConfigFromManifest;
    private FileProcessingReport fileProcessingReport;

    private static final String FILE_EXTENSION_TXT = "txt";
    private static final String HEADER_REFSETS = "id\teffectiveTime\tactive\tmoduleId\trefsetId\treferencedComponentId";
    private static final String HEADER_TERM_DESCRIPTION = "id\teffectiveTime\tactive\tmoduleId\tconceptId\tlanguageCode\ttypeId\tterm\tcaseSignificanceId";
    private static final int REFSETID_COL = 4;
    private static final int DESCRIPTION_LANGUAGE_CODE_COL = 5;
    private static final String INPUT_FILE_TYPE_REFSET = "Refset";
    private static final String INPUT_FILE_TYPE_DESCRIPTION = "Description";
    private static final String INPUT_FILE_TYPE_TEXT_DEFINITON = "TextDefinition";
    private static final String OUT_DIR = "out";
    private static final String TEXT_DEFINITION_ALL_LANGUAGE_CODE = "*";
    private static final int DESCRIPTION_TYPE_COL = 6;
    private static final String TEXT_DEFINITION_TYPE_ID = "900000000000550004";

    public FileProcessor(InputStream manifestStream, FileHelper fileHelper, BuildS3PathHelper buildS3PathHelper,
                         Product product, FileProcessingReport fileProcessingReport, boolean copyFilesDefinedInManifest) {
        this.manifestStream = manifestStream;
        this.fileHelper = fileHelper;
        this.buildS3PathHelper = buildS3PathHelper;
        this.product = product;
        this.sourceFilesMap = new HashMap<>();
        this.refsetFileProcessingConfigs = new HashMap<>();
        this.descriptionFileProcessingConfigs = new HashMap<>();
        this.textDefinitionFileProcessingConfigs = new HashMap<>();
        this.availableSources = new HashSet<>();
        this.copyFilesDefinedInManifest = copyFilesDefinedInManifest;
        this.fileProcessingReport = fileProcessingReport;
        this.refSetConfigFromManifest = new HashMap<>();
        if(copyFilesDefinedInManifest) {
            this.filesToCopy = new HashMap<>();
        }
    }

    public FileProcessingReport processFiles(List<String> sourceFileLists) throws IOException, JAXBException, ResourceNotFoundException, DecoderException, NoSuchAlgorithmException {
        try {
            initLocalDirs();
            copySourceFilesToLocal(sourceFileLists);
            loadFileProcessConfigsFromManifest();
            processFiles();
            if(this.copyFilesDefinedInManifest) {
                copyFilesToOutputDir();
            }
            uploadOutFilesToProductInputFiles();
        } catch (Exception e) {
            fileProcessingReport.add(FileProcessingReportType.ERROR, e.getLocalizedMessage());
        } finally {
           if (!FileUtils.deleteQuietly(localDir)) {
                logger.warn("Failed to delete local directory {}", localDir.getAbsolutePath());
            }
        }
        return fileProcessingReport;
    }

    private void initLocalDirs() {
        localDir = Files.createTempDir();
        outDir = new File(localDir, OUT_DIR);
        outDir.mkdir();
    }

    private File copySourceFilesToLocal(List<String> sourceFileLists) throws IOException {

        for (String sourceFilePath : sourceFileLists) {
            if (FilenameUtils.getExtension(sourceFilePath).equalsIgnoreCase(FILE_EXTENSION_TXT)) {
                //Copy files from S3 to local for processing
                InputStream sourceFileStream = fileHelper.getFileStream(buildS3PathHelper.getProductSourcesPath(product).append(sourceFilePath).toString());
                String sourceName = sourceFilePath.substring(0, sourceFilePath.indexOf("/"));
                //Keep track of the sources directories that are used
                availableSources.add(sourceName);
                File sourceDir = new File(localDir, sourceName);
                if (!sourceDir.exists()) sourceDir.mkdir();
                String fileName = FilenameUtils.getName(sourceFilePath);
                File outFile = new File(localDir + "/" + sourceName, fileName);
                FileUtils.copyInputStreamToFile(sourceFileStream, outFile);
                logger.info("Successfully created temp source file {}", outFile.getAbsolutePath());
                if (!sourceFilesMap.containsKey(sourceName)) {
                    sourceFilesMap.put(sourceName, new ArrayList<String>());
                }
                sourceFilesMap.get(sourceName).add(outFile.getAbsolutePath());
            }
        }
        return localDir;
    }

    private void loadFileProcessConfigsFromManifest() throws JAXBException, ResourceNotFoundException {
        ManifestXmlFileParser manifestXmlFileParser = new ManifestXmlFileParser();
        if(manifestStream == null) {
            fileProcessingReport.add(FileProcessingReportType.ERROR, "Failed to load manifest find");
            throw new ResourceNotFoundException("Failed to load manifest find");
        }
        ListingType listingType = manifestXmlFileParser.parse(manifestStream);
        loadProcessConfig(listingType);
    }

    private void loadProcessConfig(ListingType listingType) {
        FolderType rootFolder = listingType.getFolder();
        getFilesFromCurrentAndSubFolders(rootFolder);
    }

    private void getFilesFromCurrentAndSubFolders(FolderType folder) {
        if (folder != null) {
            if (folder.getFile() != null) {
                for (FileType fileType : folder.getFile()) {
                    if (fileType.getName().contains(INPUT_FILE_TYPE_TEXT_DEFINITON)) {
                        foundTextDefinitionFile = true;
                        if (fileType.getContainsLanguageCodes() != null && fileType.getContainsLanguageCodes().getCode() != null) {
                            for (String languageCode : fileType.getContainsLanguageCodes().getCode()) {
                                FileProcessingConfig fileProcessingConfig;
                                if (!textDefinitionFileProcessingConfigs.containsKey(languageCode)) {
                                    fileProcessingConfig = FileProcessingConfig.init(availableSources);
                                    fileProcessingConfig.setFileType(INPUT_FILE_TYPE_TEXT_DEFINITON);
                                    fileProcessingConfig.setValue(languageCode);
                                    textDefinitionFileProcessingConfigs.put(fileProcessingConfig.getValue(), fileProcessingConfig);
                                }
                                fileProcessingConfig = textDefinitionFileProcessingConfigs.get(languageCode);
                                fileProcessingConfig.addTargetFileToAllSources(fileType.getName());
                            }
                        } else {
                            FileProcessingConfig fileProcessingConfig;
                            if (!textDefinitionFileProcessingConfigs.containsKey(TEXT_DEFINITION_ALL_LANGUAGE_CODE)) {
                                fileProcessingConfig = FileProcessingConfig.init(availableSources);
                                fileProcessingConfig.setFileType(INPUT_FILE_TYPE_TEXT_DEFINITON);
                                fileProcessingConfig.setValue(TEXT_DEFINITION_ALL_LANGUAGE_CODE);
                                textDefinitionFileProcessingConfigs.put(fileProcessingConfig.getValue(), fileProcessingConfig);
                            }
                            fileProcessingConfig = textDefinitionFileProcessingConfigs.get(TEXT_DEFINITION_ALL_LANGUAGE_CODE);
                            fileProcessingConfig.addTargetFileToAllSources(fileType.getName());
                        }
                    } else if (fileType.getContainsReferenceSets() != null && fileType.getContainsReferenceSets().getRefset() != null) {
                        for (RefsetType refsetType : fileType.getContainsReferenceSets().getRefset()) {
                            String refsetId = refsetType.getId().toString();
                            FileProcessingConfig fileProcessingConfig;
                            if (!refsetFileProcessingConfigs.containsKey(refsetId)) {
                                fileProcessingConfig = FileProcessingConfig.init(availableSources);
                                fileProcessingConfig.setFileType(INPUT_FILE_TYPE_REFSET);
                                fileProcessingConfig.setValue(refsetType.getId().toString());
                                refsetFileProcessingConfigs.put(fileProcessingConfig.getValue(), fileProcessingConfig);
                            }
                            fileProcessingConfig = refsetFileProcessingConfigs.get(refsetId);
                            Map<String, List<String>> fileNameAndSourceMaps = new HashMap<>();
                            List<String> sourceFromManifest = new ArrayList<>();

                            if (refsetType.getSources() != null && refsetType.getSources().getSource() != null && !refsetType.getSources().getSource().isEmpty()) {
                                fileNameAndSourceMaps.put(fileType.getName(), refsetType.getSources().getSource());
                                refSetConfigFromManifest.put(refsetId, fileNameAndSourceMaps);
                                for (String s : refsetType.getSources().getSource()) {
                                    if (fileProcessingConfig.getTargetFiles().containsKey(s)) {
                                        fileProcessingConfig.addTargetFileToSource(s, fileType.getName());
                                    } else {
                                        logger.error("Failed to find source {}", s);
                                    }
                                }
                            } else {
                                fileProcessingConfig.addTargetFileToAllSources(fileType.getName());
                            }
                        }
                    } else if (fileType.getContainsLanguageCodes() != null && fileType.getContainsLanguageCodes().getCode() != null) {
                        for (String languageCode : fileType.getContainsLanguageCodes().getCode()) {
                            FileProcessingConfig fileProcessingConfig;
                            if (!descriptionFileProcessingConfigs.containsKey(languageCode)) {
                                fileProcessingConfig = FileProcessingConfig.init(availableSources);
                                fileProcessingConfig.setFileType(INPUT_FILE_TYPE_DESCRIPTION);
                                fileProcessingConfig.setValue(languageCode);
                                descriptionFileProcessingConfigs.put(fileProcessingConfig.getValue(), fileProcessingConfig);
                            }
                            fileProcessingConfig = descriptionFileProcessingConfigs.get(languageCode);
                            fileProcessingConfig.addTargetFileToAllSources(fileType.getName());
                        }
                    } else {
                        if(this.copyFilesDefinedInManifest) {
                            this.filesToCopy.put(fileType.getName(), fileType.getName());
                        }
                    }

                }
            }
            if (folder.getFolder() != null) {
                for (FolderType subFolder : folder.getFolder()) {
                    getFilesFromCurrentAndSubFolders(subFolder);
                }
            }
        }
    }

    private void processFiles() throws IOException {
        List<FileProcessingReportDetail> fileProcessingReportDetails = new ArrayList<>();
        for (String source : sourceFilesMap.keySet()) {
            List<String> fileList = sourceFilesMap.get(source);
            for (String fileName : fileList) {
                logger.info("Start processing file {}", fileName);
                File sourceFile = new File(fileName);
                List<String> lines = FileUtils.readLines(sourceFile, CharEncoding.UTF_8);
                if (lines != null && !lines.isEmpty()) {
                    String header = lines.get(0);
                    //remove header before processing
                    lines.remove(0);
                    if (header.startsWith(HEADER_REFSETS)) {
                        processRefsetFiles(fileProcessingReportDetails,lines, source, fileName, outDir, header);
                    } else if (header.startsWith(HEADER_TERM_DESCRIPTION)) {
                        if (foundTextDefinitionFile) {
                            processDescriptionsAndTextDefinitions(lines, source, outDir, header);
                        } else {
                            processFile(lines, source, fileName, outDir, header, DESCRIPTION_LANGUAGE_CODE_COL, descriptionFileProcessingConfigs);
                        }
                    }
                }
                logger.info("Finish processing file {}", fileName);
            }
        }
        if(fileProcessingReportDetails.size() > 0){
            if(fileProcessingReport.getDetails().containsKey(fileProcessingReportDetails.get(0).getType())){
                fileProcessingReport.getDetails().get(fileProcessingReportDetails.get(0).getType()).addAll(fileProcessingReportDetails);
            }else{
                fileProcessingReport.getDetails().put(fileProcessingReportDetails.get(0).getType().name(), fileProcessingReportDetails);
            }
        }
    }

    private void processRefsetFiles(List<FileProcessingReportDetail> fileProcessingReportDetails, List<String> lines, String sourceName, String inFileName, File outDir, String header) throws IOException {
        List<String> warnings = new ArrayList<>();
        if (lines == null || lines.isEmpty()) {
            logger.info("There is no row to process");
        }
         else {
            Map<String, List<String>> rowsGroupedByRefsetId = new HashMap<>();
            for (String line : lines) {
                String[] splits = line.split("\t");
                String refsetId = splits[REFSETID_COL];
                FileProcessingConfig fileProcessingConfig = refsetFileProcessingConfigs.get(refsetId);
                if(fileProcessingConfig == null) {
                    warnings.add(refsetId+":"+sourceName+"/"+inFileName);
                } else {
                    if(!rowsGroupedByRefsetId.containsKey(refsetId)) {
                        rowsGroupedByRefsetId.put(refsetId, new ArrayList<String>());
                    }
                    rowsGroupedByRefsetId.get(refsetId).add(line);
                }
            }
            for (String refsetId : rowsGroupedByRefsetId.keySet()) {
                FileProcessingConfig fileProcessingConfig = refsetFileProcessingConfigs.get(refsetId);
                if(fileProcessingConfig.getTargetFiles() != null){
                    Set<String> targetFiles = fileProcessingConfig.getTargetFiles().get(sourceName);
                    String exactSourceName = "";
                    String outputFileName = "";
                    if(targetFiles != null && targetFiles.size() > 0){
                        writeToFile(outDir, header, sourceName, rowsGroupedByRefsetId.get(refsetId), fileProcessingConfig);
                        String infoMessage = new StringBuilder("Found refset id ").append(refsetId)
                                .append(" in ").append(sourceName+"/"+FilenameUtils.getName(inFileName)).toString();
                        fileProcessingReportDetails.add(new FileProcessingReportDetail(FileProcessingReportType.INFO, FilenameUtils.getName(inFileName) , refsetId, sourceName, infoMessage));
                    }else{
                        Map<String, List<String>> fileNameAccordingSources = refSetConfigFromManifest.get(refsetId);
                        if(fileNameAccordingSources != null ){

                            for(Map.Entry<String, List<String>> entry : fileNameAccordingSources.entrySet()){
                                outputFileName = entry.getKey();
                                exactSourceName = entry.getValue().toString();
                            }
                            String warningMessage = new StringBuilder("The Manifest states that this Reference Set content should come from the following sources: ").append(exactSourceName).toString();
                            fileProcessingReport.add(FileProcessingReportType.WARNING, outputFileName , refsetId, sourceName, warningMessage);
                            logger.warn(warningMessage);
                        }

                    }

                }
            }
            for (String warning : warnings) {
                String[] warningSplits = warning.split(":");
                String warningMessage = new StringBuilder("Found refset id ").append(warningSplits[0])
                        .append(" in ").append(warningSplits[1]).append(" but is not used in manifest configuration").toString();
                fileProcessingReport.add(FileProcessingReportType.WARNING,  FilenameUtils.getName(inFileName) , warningSplits[0], sourceName, warningMessage);
                logger.warn("Found refset id {} in source file {}/{} but is not used in manifest configuration", warningSplits[0], sourceName, inFileName);
            }

        }
    }

    private void processFile(List<String> lines, String sourceName, String inFileName, File outDir, String header, int comparisonColumn, Map<String,
            FileProcessingConfig> fileProcessingConfigs) throws IOException {
        Map<String, List<String>> rows = new HashMap<>();
        if (lines == null || lines.isEmpty())  {
            logger.info("There is no row to process");
        } else {
            for (String line : lines) {
                String[] splits = line.split("\t");
                String comparisonValue = splits[comparisonColumn];
                if (!rows.containsKey(comparisonValue)) {
                    rows.put(comparisonValue, new ArrayList<String>());
                }
                rows.get(comparisonValue).add(line);
            }
            for (String comparisonValue : rows.keySet()) {
                FileProcessingConfig fileProcessingConfig = fileProcessingConfigs.get(comparisonValue);
                writeToFile(outDir, header, sourceName, rows.get(comparisonValue), fileProcessingConfig);
            }
        }

    }

    private void processDescriptionsAndTextDefinitions(List<String> lines, String sourceName, File outDir, String header) throws IOException {
        Map<String, List<String>> descriptionRows = new HashMap<>();
        Map<String, List<String>> textDefinitionRows = new HashMap<>();
        textDefinitionRows.put(TEXT_DEFINITION_ALL_LANGUAGE_CODE, new ArrayList<String>());
        if (lines == null || lines.isEmpty()) {
            logger.info("There is no row to process");
        } else {
            for (String line : lines) {
                String[] splits = line.split("\t");
                String comparisonValue = splits[DESCRIPTION_LANGUAGE_CODE_COL];
                String descriptionTypeValue = splits[DESCRIPTION_TYPE_COL];
                if (TEXT_DEFINITION_TYPE_ID.equals(descriptionTypeValue)) {
                    if (!textDefinitionRows.containsKey(comparisonValue)) {
                        textDefinitionRows.put(comparisonValue, new ArrayList<String>());
                    }
                    textDefinitionRows.get(comparisonValue).add(line);
                    textDefinitionRows.get(TEXT_DEFINITION_ALL_LANGUAGE_CODE).add(line);
                } else {
                    if (!descriptionRows.containsKey(comparisonValue)) {
                        descriptionRows.put(comparisonValue, new ArrayList<String>());
                    }
                    descriptionRows.get(comparisonValue).add(line);
                }

            }
            for (String comparisonValue : descriptionRows.keySet()) {
                FileProcessingConfig fileProcessingConfig = descriptionFileProcessingConfigs.get(comparisonValue);
                writeToFile(outDir, header, sourceName, descriptionRows.get(comparisonValue), fileProcessingConfig);
            }
            // If text definition configuration does not specify languages, copy all text definitions regardless of language code
            FileProcessingConfig allLanguagesConfig = textDefinitionFileProcessingConfigs.get(TEXT_DEFINITION_ALL_LANGUAGE_CODE);
            // If text definition configuration specifies languages, copy all text definitions that have specified language code
            for (String comparisonValue : textDefinitionRows.keySet()) {
                FileProcessingConfig fileProcessingConfig = textDefinitionFileProcessingConfigs.get(comparisonValue);
                if(fileProcessingConfig != null && allLanguagesConfig != null) {
                    fileProcessingConfig.getTargetFiles().get(sourceName).addAll(allLanguagesConfig.getTargetFiles().get(sourceName));
                }
                writeToFile(outDir, header, sourceName, textDefinitionRows.get(comparisonValue), fileProcessingConfig);
            }
        }

    }

    private void writeToFile(File outDir, String header, String sourceName, List<String> lines, FileProcessingConfig fileProcessingConfig) throws IOException {
        if (fileProcessingConfig != null && lines != null && !lines.isEmpty()) {
            Set<String> outFileList = fileProcessingConfig.getTargetFiles().get(sourceName);
            if (outFileList != null) {
                for (String outFileName : outFileList) {
                    File outFile = new File(outDir, outFileName);
                    if (!outFile.exists()) {
                        outFile.createNewFile();
                        List<String> headers = new ArrayList<>();
                        headers.add(header);
                        FileUtils.writeLines(outFile, CharEncoding.UTF_8, headers, RF2Constants.LINE_ENDING);
                    }
                    FileUtils.writeLines(outFile, CharEncoding.UTF_8, lines, RF2Constants.LINE_ENDING, true);
                    logger.info("Copied {} lines to {}", lines.size(), outFile.getAbsolutePath());
                }
            }
        }
    }


    private void copyFilesToOutputDir() throws IOException {
        Map<String, List<String>> fileToCopyWithSourceList = new HashMap<>();
        for (String source : sourceFilesMap.keySet()) {
            List<String> sourceFiles = sourceFilesMap.get(source);
            for (String sourceFilePath : sourceFiles) {
                String sourceFileName = FilenameUtils.getName(sourceFilePath);
                String file = filesToCopy.get(sourceFileName);
                if(file != null) {
                    if(!fileToCopyWithSourceList.containsKey(file)) {
                        List<String> sources = new ArrayList<>();
                        fileToCopyWithSourceList.put(file, sources);
                    }
                    fileToCopyWithSourceList.get(file).add(source + "::" + sourceFilePath);
                }
            }
        }
        for (String fileName : fileToCopyWithSourceList.keySet()) {
            List<String> sources = fileToCopyWithSourceList.get(fileName);
            int fileCount = sources.size();
            if(fileCount > 1) {
                StringBuilder warningMessageBuilder = new StringBuilder("Found file with name ").append(fileName).append( " in multiple sources:");
                for (String sourceName : sources) {
                    String src = sourceName.split("::")[0];
                    warningMessageBuilder.append(" ").append(src).append(",");
                }
                warningMessageBuilder.deleteCharAt(warningMessageBuilder.length()-1);
                warningMessageBuilder.append(". Skip copying file.");
                fileProcessingReport.add(FileProcessingReportType.WARNING, fileName, null, null, warningMessageBuilder.toString());
            } else if (fileCount == 0) {
                logger.warn("Could not find file with name {} in any source. Skip copying file", fileName);
                StringBuilder warningMessageBuilder = new StringBuilder("Could not find file with name ").append(fileName).append(" in any source");
                fileProcessingReport.add(FileProcessingReportType.WARNING, fileName, null, null, warningMessageBuilder.toString());
            } else {
                    String path = sources.get(0).split("::")[1];
                    File inFile = new File(path);
                    File outFile = new File(outDir, fileName);
                    FileUtils.copyFile(inFile, outFile);
                    logger.info("Copied {} to {}", inFile.getAbsolutePath(), outFile.getAbsolutePath());
            }
        }
    }

    private void mergeUnspecfiedFile(String fileName, List<String> sourceFileMap) throws IOException {
        File outFile = new File(outDir, fileName);
        boolean isFirstFile = true;
        for (String sourcePath : sourceFileMap) {
            List<String> lines = FileUtils.readLines(new File(sourcePath), CharEncoding.UTF_8);
            if(isFirstFile) {
                FileUtils.writeLines(outFile, CharEncoding.UTF_8, lines, RF2Constants.LINE_ENDING);
                isFirstFile = false;
            } else {
                lines.remove(0); //Remove header line
                FileUtils.writeLines(outFile, CharEncoding.UTF_8, lines, RF2Constants.LINE_ENDING, true);
            }
        }
        System.out.println(FileUtils.readLines(outFile, CharEncoding.UTF_8).size());

    }

    private void uploadOutFilesToProductInputFiles() throws NoSuchAlgorithmException, IOException, DecoderException {
        File[] files = outDir.listFiles();
        for (File file : files) {
            String filePath =   buildS3PathHelper.getProductInputFilesPath(product) + file.getName();
            fileProcessingReport.add(FileProcessingReportType.INFO, new StringBuilder("Uploaded ").append(file.getName()).append(" to product input files directory").toString());
            fileHelper.putFile(file,filePath);
            logger.info("Uploaded {} to product input files directory", file.getName());
        }
    }

}

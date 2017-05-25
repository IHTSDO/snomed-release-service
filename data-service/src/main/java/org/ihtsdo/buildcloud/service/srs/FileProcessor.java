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

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    private boolean foundTextDefinitionFile = false;

    private Set<String> availableSources;
    private Map<String, FileProcessingConfig> refsetFileProcessingConfigs;
    private Map<String, FileProcessingConfig> descriptionFileProcessingConfigs;
    private Map<String, FileProcessingConfig> textDefinitionFileProcessingConfigs;

    private Map<String, List<String>> sourceFilesMap;

    private static final String FILE_EXTENSION_TXT = "txt";
    private static final String HEADER_REFSETS = "id\teffectiveTime\tactive\tmoduleId\trefsetId\treferencedComponentId";
    private static final String HEADER_TERM_DESCRIPTION = "id\teffectiveTime\tactive\tmoduleId\tconceptId\tlanguageCode\ttypeId\tterm\tcaseSignificanceId";
    private static final int REFSETID_COL = 4;
    private static final int DESCRIPTION_LANGUAGE_CODE_COL = 5;
    private static final String INPUT_FILE_TYPE_REFSET = "Refset";
    private static final String INPUT_FILE_TYPE_DESCRIPTION = "Description";
    private static final String INPUT_FILE_TYPE_TEXT_DEFINITON = "TextDefinition";
    private static final String OUT_DIR = "out";
    private static final String TEXT_DEFINITON = "TextDefinition";
    private static final String TEXT_DEFINITION_ALL_LANGUAGE_CODE = "*";
    private static final int DESCRIPTION_TYPE_COL = 6;
    private static final String TEXT_DEFINITION_TYPE_ID = "900000000000550004";

    public FileProcessor(InputStream manifestStream, FileHelper fileHelper, BuildS3PathHelper buildS3PathHelper, Product product) {
        this.manifestStream = manifestStream;
        this.fileHelper = fileHelper;
        this.buildS3PathHelper = buildS3PathHelper;
        this.product = product;
        this.sourceFilesMap = new HashMap<>();
        this.refsetFileProcessingConfigs = new HashMap<>();
        this.descriptionFileProcessingConfigs = new HashMap<>();
        this.textDefinitionFileProcessingConfigs = new HashMap<>();
        this.availableSources = new HashSet<>();
    }

    public void processFiles(List<String> sourceFileLists) throws IOException, JAXBException, ResourceNotFoundException {
        try {
            copySourceFilesToLocal(sourceFileLists);
            loadFileProcessConfigsFromManifest();
            processFiles();
        } finally {
            if (!FileUtils.deleteQuietly(localDir)) {
                logger.warn("Failed to delete local directory {}", localDir.getAbsolutePath());
            }
        }
    }

    private File copySourceFilesToLocal(List<String> sourceFileLists) throws IOException {
        localDir = Files.createTempDir();
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
                OutputStream outputStream = new FileOutputStream(outFile);
                IOUtils.copy(sourceFileStream, outputStream);
                IOUtils.closeQuietly(outputStream);
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
        ListingType listingType = manifestXmlFileParser.parse(manifestStream);
        loadProcessConfig(listingType);
    }

    private List<String> loadProcessConfig(ListingType listingType) {
        FolderType rootFolder = listingType.getFolder();
        List<String> result = new ArrayList<>();
        getFilesFromCurrentAndSubFolders(rootFolder, result);
        return result;
    }

    private void getFilesFromCurrentAndSubFolders(FolderType folder, List<String> filesList) {
        if (folder != null) {
            if (folder.getFile() != null) {
                for (FileType fileType : folder.getFile()) {
                    if (fileType.getName().contains(TEXT_DEFINITON)) {
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
                            if (refsetType.getSources() != null && refsetType.getSources().getSource() != null && !refsetType.getSources().getSource().isEmpty()) {
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

    private void processFiles() throws IOException {
        for (String source : sourceFilesMap.keySet()) {
            List<String> fileList = sourceFilesMap.get(source);
            for (String fileName : fileList) {
                logger.info("Start processing file {}", fileName);
                File sourceFile = new File(fileName);
                List<String> lines = FileUtils.readLines(sourceFile, CharEncoding.UTF_8);
                if (lines != null && !lines.isEmpty()) {
                    File outDir = new File(localDir, OUT_DIR);
                    if (!outDir.exists()) outDir.mkdirs();
                    String header = lines.get(0);
                    //remove header before processing
                    lines.remove(0);
                    if (header.startsWith(HEADER_REFSETS)) {
                        processFile(lines, source, outDir, header, REFSETID_COL, refsetFileProcessingConfigs);
                    } else if (header.startsWith(HEADER_TERM_DESCRIPTION)) {
                        if (foundTextDefinitionFile) {
                            processDescriptionAndTextDefinition(lines, source, outDir, header);
                        } else {
                            processFile(lines, source, outDir, header, DESCRIPTION_LANGUAGE_CODE_COL, descriptionFileProcessingConfigs);
                        }
                    }
                }
                logger.info("Finish processing file {}", fileName);
            }
        }
    }

    private void processFile(List<String> lines, String sourceName, File outDir, String header, int comparisonColumn, Map<String,
            FileProcessingConfig> fileProcessingConfigs) throws IOException {
        Map<String, List<String>> rows = new HashMap<>();
        if (lines == null || lines.isEmpty()) logger.info("There is no row to process");
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
            /*if (fileProcessingConfig != null) {
                Set<String> outFileList = fileProcessingConfig.getTargetFiles().get(sourceName);
                if (outFileList != null) {
                    for (String outFileName : outFileList) {
                        File outFile = new File(outDir, outFileName);
                        if (!outFile.exists()) {
                            outFile.createNewFile();
                            List<String> headers = new ArrayList<>();
                            headers.add(header);
                            FileUtils.writeLines(outFile, headers);
                        }
                        FileUtils.writeLines(outFile, rows.get(comparisonValue), true);
                        logger.info("Copied {} lines with key value = {} to {}", rows.get(comparisonValue).size(), comparisonValue, outFile.getAbsolutePath());
                    }
                }
            }*/
        }
    }

    private void processDescriptionAndTextDefinition(List<String> lines, String sourceName, File outDir, String header) throws IOException {
        Map<String, List<String>> descriptionRows = new HashMap<>();
        Map<String, List<String>> textDefinitionRows = new HashMap<>();
        textDefinitionRows.put(TEXT_DEFINITION_ALL_LANGUAGE_CODE, new ArrayList<String>());
        if (lines == null || lines.isEmpty()) logger.info("There is no row to process");
        for (String line : lines) {
            String[] splits = line.split("\t");
            String comparisonValue = splits[DESCRIPTION_LANGUAGE_CODE_COL];
            String descriptionTypeValue = splits[DESCRIPTION_TYPE_COL];
            if (descriptionTypeValue.equals(TEXT_DEFINITION_TYPE_ID)) {
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
        FileProcessingConfig allLanguagesConfig = textDefinitionFileProcessingConfigs.get(TEXT_DEFINITION_ALL_LANGUAGE_CODE);
        //writeToFile(outDir, header, sourceName, textDefinitionRows.get(TEXT_DEFINITION_ALL_LANGUAGE_CODE), allLanguagesConfig);
        for (String comparisonValue : textDefinitionRows.keySet()) {
            FileProcessingConfig fileProcessingConfig = descriptionFileProcessingConfigs.get(comparisonValue);
            writeToFile(outDir, header, sourceName, textDefinitionRows.get(comparisonValue), fileProcessingConfig);
        }
    }

    private void writeToFile(File outDir, String header, String sourceName, List<String> lines, FileProcessingConfig fileProcessingConfig) throws IOException {
        if (fileProcessingConfig != null) {
            Set<String> outFileList = fileProcessingConfig.getTargetFiles().get(sourceName);
            if (outFileList != null) {
                for (String outFileName : outFileList) {
                    File outFile = new File(outDir, outFileName);
                    if (!outFile.exists()) {
                        outFile.createNewFile();
                        List<String> headers = new ArrayList<>();
                        headers.add(header);
                        FileUtils.writeLines(outFile, headers);
                    }
                    FileUtils.writeLines(outFile, lines, true);
                    logger.info("Copied {} lines to {}", lines.size(), outFile.getAbsolutePath());
                }
            }
        }
    }

}

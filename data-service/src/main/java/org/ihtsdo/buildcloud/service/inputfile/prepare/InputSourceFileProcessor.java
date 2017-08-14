package org.ihtsdo.buildcloud.service.inputfile.prepare;

import com.google.common.io.Files;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.CharEncoding;
import org.apache.commons.lang3.StringUtils;
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
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import javax.xml.bind.JAXBException;

import static org.ihtsdo.buildcloud.service.inputfile.prepare.ReportType.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class InputSourceFileProcessor {

    private final Logger logger = LoggerFactory.getLogger(InputSourceFileProcessor.class);
    
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
    private MultiValueMap<String, String> filesToCopy;
    private Map<String, List<String>> sourceFilesMap;
    private Map<String, Map<String, List<String>>> refSetConfigFromManifest;
    private SourceFileProcessingReport fileProcessingReport;

    private Map<String,List<String>> skippedSourceFiles;

    public InputSourceFileProcessor(InputStream manifestStream, FileHelper fileHelper, BuildS3PathHelper buildS3PathHelper,
                         Product product, SourceFileProcessingReport fileProcessingReport, boolean copyFilesDefinedInManifest) {
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
        this.skippedSourceFiles = new HashMap<>();
        this.filesToCopy = new LinkedMultiValueMap<>();
      
    }

    public SourceFileProcessingReport processFiles(List<String> sourceFileLists) throws IOException, JAXBException, ResourceNotFoundException, DecoderException, NoSuchAlgorithmException {
        try {
            initLocalDirs();
            copySourceFilesToLocal(sourceFileLists);
            loadFileProcessConfigsFromManifest();
            processFiles();
            if (this.copyFilesDefinedInManifest) {
               fileProcessingReport.addReportDetails(copyFilesToOutputDir());
            }
            uploadOutFilesToProductInputFiles();
        } catch (Exception e) {
            fileProcessingReport.add(ReportType.ERROR, e.getLocalizedMessage());
            logger.error("Error encountered when preparing input files.", e);
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
        fileProcessingReport.setSoureFiles(sourceFilesMap);
        return localDir;
    }

    private void loadFileProcessConfigsFromManifest() throws JAXBException, ResourceNotFoundException {
        ManifestXmlFileParser manifestXmlFileParser = new ManifestXmlFileParser();
        if(manifestStream == null) {
            fileProcessingReport.add(ReportType.ERROR, "Failed to load manifest find");
            throw new ResourceNotFoundException("Failed to load manifest find");
        }
        ListingType listingType = manifestXmlFileParser.parse(manifestStream);
        loadProcessConfig(listingType);
    }

    private void loadProcessConfig(ListingType listingType) {
        FolderType rootFolder = listingType.getFolder();
        getDeltaFilesFromFolder(rootFolder);
    }

    private void getDeltaFilesFromFolder(FolderType folder) {
        if (folder != null) {
            if (folder.getFile() != null ) {
                for (FileType fileType : folder.getFile()) {
                	if (fileType.getName().contains(RF2Constants.SNAPSHOT) || fileType.getName().contains(RF2Constants.FULL)) {
                		continue;
                	}
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
                            if (refsetType.getSources() != null && refsetType.getSources().getSource() != null && !refsetType.getSources().getSource().isEmpty()) {
                                fileNameAndSourceMaps.put(fileType.getName(), refsetType.getSources().getSource());
                                refSetConfigFromManifest.put(refsetId, fileNameAndSourceMaps);
                                for (String s : refsetType.getSources().getSource()) {
                                    if (fileProcessingConfig.getTargetFiles().containsKey(s)) {
                                        fileProcessingConfig.addTargetFileToSource(s, fileType.getName());
                                    } else {
                                        logger.error("Failed to find source {}" + s);
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
                        	String deltaFileName = fileType.getName();
                        	logger.debug("Add file to copy:" + deltaFileName);
                        	deltaFileName = (deltaFileName.startsWith(RF2Constants.BETA_RELEASE_PREFIX)) ? deltaFileName.replaceFirst(RF2Constants.BETA_RELEASE_PREFIX, "") : deltaFileName;
                            if (fileType.getSources() != null) {
                            		this.filesToCopy.put(deltaFileName, fileType.getSources().getSource());
                            } else {
                            		this.filesToCopy.put(deltaFileName, Collections.emptyList());
                            	}
                        }
                    }
                }
            }
            if (folder.getFolder() != null) {
                for (FolderType subFolder : folder.getFolder()) {
                	getDeltaFilesFromFolder(subFolder);
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
                    } else {
                    	addFileToSkippedList(source, fileName);
                    }
                } 
                logger.info("Finish processing file {}", fileName);
            }
        }
        for (FileProcessingReportDetail detail : fileProcessingReportDetails) {
        	fileProcessingReport.addReportDetail(detail);
        	
        }
    }

    private void processRefsetFiles(List<FileProcessingReportDetail> fileProcessingReportDetails, List<String> lines, String sourceName, String inFileName, File outDir, String header) throws IOException {
        if (lines == null || lines.isEmpty()) {
            logger.info("There is no row to process");
        }
         else {
            String[] splits = lines.get(0).split("\t");
            String refsetId = splits[REFSETID_COL];
            FileProcessingConfig fileProcessingConfig = refsetFileProcessingConfigs.get(refsetId);
            //Show warning if refset id is found in sources but not used in manifest configuration
            if (fileProcessingConfig == null) {
                String warningMessage = new StringBuilder("Found refset id ").append(refsetId)
                        .append(" in ").append(sourceName+"/"+FilenameUtils.getName(inFileName)).append(" but is not used in manifest configuration").toString();
                fileProcessingReport.add(ReportType.WARNING,  FilenameUtils.getName(inFileName) , refsetId, sourceName, warningMessage);
                logger.warn("Found refset id {} in source file {}/{} but is not used in manifest configuration", refsetId, sourceName, inFileName);
            } else {
                if(fileProcessingConfig.getTargetFiles() != null){
                    Set<String> targetFiles = fileProcessingConfig.getTargetFiles().get(sourceName);
                    String exactSourceName = "";
                    String outputFileName = "";
                    if(targetFiles != null && targetFiles.size() > 0){
                        writeToFile(outDir, header, sourceName, lines, fileProcessingConfig);
                        String infoMessage = new StringBuilder("Found refset id ").append(refsetId)
                                .append(" in ").append(sourceName+"/"+FilenameUtils.getName(inFileName)).toString();
                        fileProcessingReportDetails.add(new FileProcessingReportDetail(INFO, FilenameUtils.getName(inFileName) , refsetId, sourceName, infoMessage));
                    } else {
                        Map<String, List<String>> fileNameAccordingSources = refSetConfigFromManifest.get(refsetId);
                        if (fileNameAccordingSources != null ){
                            for(Map.Entry<String, List<String>> entry : fileNameAccordingSources.entrySet()){
                                    outputFileName = entry.getKey();
                                    exactSourceName = entry.getValue().toString();
                            }
                            String warningMessage = new StringBuilder("The manifest.xml states that this Reference Set content should come from the following sources: ").append(exactSourceName).toString();
                            fileProcessingReport.add(WARNING, outputFileName , refsetId, sourceName, warningMessage);
                            logger.warn(warningMessage);
                        }

                    }
                }
            }
        }
    }

    private void addFileToSkippedList(String sourceName, String filename) {
    	if (skippedSourceFiles.get(sourceName) == null) {
    		List<String> files = new ArrayList<String>();
    		files.add(filename);
    		skippedSourceFiles.put(sourceName, files );
    	} else {
    		skippedSourceFiles.get(sourceName).add(filename);
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


    private List<FileProcessingReportDetail> copyFilesToOutputDir() throws IOException {
    	//check files specified in the manifest file but not present in the out dir.
    	List<FileProcessingReportDetail> reportDetails = new ArrayList<>();
    	List<String> copyFileList = new ArrayList<>(filesToCopy.keySet());
        Map<String, Integer> fileCountMap = new HashMap<>();
        for (String source : skippedSourceFiles.keySet()) {
            List<String> sourceFiles = skippedSourceFiles.get(source);
            for (String sourceFilePath : sourceFiles) {
                String sourceFileName = FilenameUtils.getName(sourceFilePath);
                if (copyFileList.contains(sourceFileName)) {
                    if(!fileCountMap.containsKey(sourceFileName)) {
                        fileCountMap.put(sourceFileName, 0);
                    }
                    fileCountMap.put(sourceFileName, fileCountMap.get(sourceFileName) + 1);
                }
            }
        }
        Map<String,String> fileNameMap = getFileNameMapWithoutNamespaceToken();
        for (String source : skippedSourceFiles.keySet()) {
        	List<String> filesProcessed = new ArrayList<>();
            for (String sourceFilePath : skippedSourceFiles.get(source)) {
                String sourceFileName = FilenameUtils.getName(sourceFilePath);
                sourceFileName = sourceFileName.startsWith(RF2Constants.BETA_RELEASE_PREFIX) ? sourceFileName.replaceFirst(RF2Constants.BETA_RELEASE_PREFIX, "") : sourceFileName;
                filesProcessed.add(sourceFilePath);
                Integer fileCount = fileCountMap.get(sourceFileName);
                if (fileCount != null) {
                    if (fileCount > 1) { 
                    	//If we find duplication in multiple sources, skip file copying since we don't know which one to copy
                    	String msg = "Found file with name " + sourceFileName + "in multiple sources. Skip copying source file to output directory";
                        logger.warn(msg);
                        reportDetails.add(new FileProcessingReportDetail(WARNING, msg));
                    } else if (fileCount <=0 ){ 
                    	String msg = "Could not find any file matched exactly in any source with name:" + sourceFileName;
                        logger.warn(msg);
                        reportDetails.add(new FileProcessingReportDetail(WARNING, sourceFileName, source, null, msg));
                    } else {
                        File inFile = new File(sourceFilePath);
                        File outFile = new File(outDir, sourceFileName);
                        FileUtils.copyFile(inFile, outFile);
                        logger.info("Copied {} to {}", inFile.getAbsolutePath(), outFile.getAbsolutePath());
                        reportDetails.add(new FileProcessingReportDetail(INFO, sourceFileName, source, null, " file name matched exactly and copied"));
                    }
                } else {
                	String fileNameSpecifiedByManifest = fileNameMap.get(getFileNameWithoutCountryNameSpaceToken(sourceFileName));
                    if (fileNameSpecifiedByManifest != null) {
                    	if (!filesToCopy.containsKey(fileNameSpecifiedByManifest) || (!filesToCopy.get(fileNameSpecifiedByManifest).isEmpty() && !filesToCopy.get(fileNameSpecifiedByManifest).contains(source))) {
                        	//source file is not required by the manifest
                        	String msg = "File is skipped as not required by the manifest:" +  sourceFileName;
                        	logger.info(msg);
                        	reportDetails.add(new FileProcessingReportDetail(INFO, sourceFileName, source, null, msg));
                        } else {
                        	File inFile = new File(sourceFilePath);
                            File outFile = new File(outDir, fileNameSpecifiedByManifest);
                            FileUtils.copyFile(inFile, outFile);
                            logger.info("Renamed {} to {}", inFile.getAbsolutePath(), outFile.getAbsolutePath());
                            reportDetails.add(new FileProcessingReportDetail(INFO, sourceFileName, source, null, "Renamed to:" + fileNameSpecifiedByManifest));
                        }
                    	 
                    } else {
                    	String msg = "File is skipped as can't find any match in the manifest. Please check the file name is specified in the manifest and has the same release date as the source file.";
                    	logger.warn(msg);
                    	reportDetails.add(new FileProcessingReportDetail(WARNING, sourceFileName, source, null, msg));
                    }
                }
            }
            skippedSourceFiles.get(source).removeAll(filesProcessed);
        }
        return reportDetails;
    }
    
    private String getFileNameWithoutCountryNameSpaceToken(String rf2FileName) {
    	rf2FileName = rf2FileName.startsWith(RF2Constants.BETA_RELEASE_PREFIX) ? rf2FileName.replaceFirst(RF2Constants.BETA_RELEASE_PREFIX, "") : rf2FileName;
    	String[] splits = rf2FileName.split(RF2Constants.FILE_NAME_SEPARATOR);
		StringBuilder key = new StringBuilder();
		for (int i=0; i < splits.length ;i++) {
			if (i == 3) {
				continue;
			}
			if ( i > 0) {
				key.append(RF2Constants.FILE_NAME_SEPARATOR);
			}
			key.append(splits[i]);
		}
		return key.toString();
    }
    
    private Map<String, String> getFileNameMapWithoutNamespaceToken() {
    	Map<String, String> result = new HashMap<>();
    	for (String fileName : filesToCopy.keySet()) {
    		result.put(getFileNameWithoutCountryNameSpaceToken(fileName), fileName);
    	}
    	return result;
    }
    
    private void uploadOutFilesToProductInputFiles() throws NoSuchAlgorithmException, IOException, DecoderException {
        File[] files = outDir.listFiles();
        List<String> filesPrepared = new ArrayList<>();
    	logger.debug("Found {} prepared files in directory {} to upload to the input-files folder in S3", files.length, outDir.getAbsolutePath());
        for (File file : files) {
        	String inputFileName = file.getName();
        	filesPrepared.add(inputFileName);
        	if (file.getName().startsWith(RF2Constants.BETA_RELEASE_PREFIX)) {
        		inputFileName = file.getName().replaceFirst(RF2Constants.BETA_RELEASE_PREFIX, "");
        	}
        	inputFileName = inputFileName.replace("der2", "rel2").replace("sct2", "rel2");
            String filePath =   buildS3PathHelper.getProductInputFilesPath(product) + inputFileName;
            fileProcessingReport.add(ReportType.INFO, new StringBuilder("Uploaded ").append(inputFileName).append(" to product input files directory").toString());
            fileHelper.putFile(file,filePath);
            logger.info("Uploaded {} to product input files directory with name {}", file.getName(), inputFileName);
        }
        List<String> requiredFileList = new ArrayList<String>(filesToCopy.keySet());
        for (String filename : requiredFileList) {
        	if (!filesPrepared.contains(filename)) {
        		 String message = "Required by manifest but not found in source:" + filename;
            	 logger.warn(message);
    			fileProcessingReport.add(WARNING, message);
        	}
        	
        }
    }

	public Map<String, List<String>> getSkippedSourceFiles() {
		return skippedSourceFiles;
	}
    
}

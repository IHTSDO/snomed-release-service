package org.ihtsdo.buildcloud.service.inputfile.prepare;

import static org.ihtsdo.buildcloud.service.inputfile.prepare.ReportType.ERROR;
import static org.ihtsdo.buildcloud.service.inputfile.prepare.ReportType.INFO;
import static org.ihtsdo.buildcloud.service.inputfile.prepare.ReportType.WARNING;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBException;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.CharEncoding;
import org.ihtsdo.buildcloud.dao.helper.BuildS3PathHelper;
import org.ihtsdo.buildcloud.entity.Product;
import org.ihtsdo.buildcloud.manifest.FieldType;
import org.ihtsdo.buildcloud.manifest.FileType;
import org.ihtsdo.buildcloud.manifest.FolderType;
import org.ihtsdo.buildcloud.manifest.ListingType;
import org.ihtsdo.buildcloud.manifest.RefsetType;
import org.ihtsdo.buildcloud.service.build.RF2Constants;
import org.ihtsdo.buildcloud.service.file.ManifestXmlFileParser;
import org.ihtsdo.otf.dao.s3.helper.FileHelper;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.otf.rest.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.google.common.io.Files;

public class InputSourceFileProcessor {

	private static final String TAB = "\t";

	private static final String NO_LANGUAGE_CODE_IS_CONFIGURED_MSG = "No language code is configured in the manifest.xml.";

	private static final String NO_DATA_FOUND = "No data found apart from header line.";

	private static final String README_HEADER_FILE_NAME = "readme-header.txt";

	private static final String UNPROCESSABLE_MSG = "Can't be processed as the %s appears in multiple sources and no source is configured in the manifest.xml.";

	private final Logger logger = LoggerFactory.getLogger(InputSourceFileProcessor.class);

	private static final String HEADER_REFSETS = "id\teffectiveTime\tactive\tmoduleId\trefsetId\treferencedComponentId";
	private static final String HEADER_TERM_DESCRIPTION = "id\teffectiveTime\tactive\tmoduleId\tconceptId\tlanguageCode\ttypeId\tterm\tcaseSignificanceId";
	private static final int REFSETID_COL = 4;
	private static final int DESCRIPTION_LANGUAGE_CODE_COL = 5;
	private static final String INPUT_FILE_TYPE_REFSET = "Refset";
	private static final String INPUT_FILE_TYPE_DESCRIPTION = "Description_";
	private static final String INPUT_FILE_TYPE_TEXT_DEFINITON = "TextDefinition";
	private static final String OUT_DIR = "out";
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
	private Map<String, List<String>> sourceFilesMap;
	private SourceFileProcessingReport fileProcessingReport;
	private Map<String,List<String>> skippedSourceFiles;
	private MultiValueMap<String, String> fileOrKeyWithMultipleSources;
	//processing instructions from the manifest.xml
	private Map<String, FileProcessingConfig> refsetFileProcessingConfigs;
	private Map<String, FileProcessingConfig> descriptionFileProcessingConfigs;
	private Map<String, FileProcessingConfig> textDefinitionFileProcessingConfigs;
	private MultiValueMap<String, String> filesToCopyFromSource;
	private MultiValueMap<String, String> refsetWithAdditionalFields;

	public InputSourceFileProcessor(InputStream manifestStream, FileHelper fileHelper, BuildS3PathHelper buildS3PathHelper,
						 Product product, boolean copyFilesDefinedInManifest) {
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
		this.skippedSourceFiles = new HashMap<>();
		this.filesToCopyFromSource = new LinkedMultiValueMap<>();
		this.refsetWithAdditionalFields = new LinkedMultiValueMap<>();
		this.fileProcessingReport = new SourceFileProcessingReport();
		this.fileOrKeyWithMultipleSources = new LinkedMultiValueMap<>();
	}

	public SourceFileProcessingReport processFiles(List<String> sourceFileLists) {
		try {
			initLocalDirs();
			copySourceFilesToLocal(sourceFileLists);
			loadFileProcessConfigsFromManifest();
			prepareSourceFiles();
			if (copyFilesDefinedInManifest) {
				try {
					fileProcessingReport.addReportDetails(copyFilesToOutputDir());
				} catch (Exception e) {
					String errorMsg = "Failed to copy files defined in manifest locally for preparing input files.";
					logger.error(errorMsg, e);
					fileProcessingReport.add(ReportType.ERROR, errorMsg);
				}
			}
			verifyRefsetFiles();
			uploadOutFilesToProductInputFiles();
		} finally {
			if (!FileUtils.deleteQuietly(localDir)) {
				logger.warn("Failed to delete local directory {}", localDir.getAbsolutePath());
			}
		}
		return fileProcessingReport;
	}

	private void verifyRefsetFiles() {
		File[] filesPrepared = outDir.listFiles();
		Collection<FileProcessingConfig> configs = refsetFileProcessingConfigs.values();
		Set<String> refSetFilesToCreate = new HashSet<>();
		for (FileProcessingConfig config : configs) {
			refSetFilesToCreate.add(config.getTargetFileName());
		}
		for (String refsetFileName : refSetFilesToCreate) {
			refsetFileName = refsetFileName.startsWith(RF2Constants.BETA_RELEASE_PREFIX) ? refsetFileName.substring(1) : refsetFileName;
			boolean isRefsetFilePrepared = false;
			StringBuilder headerLine = new StringBuilder();
			headerLine.append(HEADER_REFSETS);
			boolean checkAdditionalFields = false;
			if (refsetWithAdditionalFields.get(refsetFileName) != null
					&& !refsetWithAdditionalFields.get(refsetFileName).isEmpty()) {
				checkAdditionalFields = true;
				for (String fieldName : refsetWithAdditionalFields.get(refsetFileName)) {
					 headerLine.append(TAB);
					 headerLine.append(fieldName);
				}
			}
			for (File file : filesPrepared) {
				String fileName = file.getName();
				fileName = fileName.startsWith(RF2Constants.BETA_RELEASE_PREFIX) ? fileName.substring(1) : fileName;
				if (fileName.equals(refsetFileName)) {
					isRefsetFilePrepared = true;
					//check refset contains additional fields
					if (checkAdditionalFields) {
						try {
							String header = getHeaderLine(file);
							if (header == null || !header.equals(headerLine.toString())) {
								fileProcessingReport.add(ReportType.ERROR, refsetFileName, null, null,
										"Refset file does not contain a valid header according to the manifest. Actual:" + header + " while expecting:" + headerLine.toString());
							}
						} catch (IOException e) {
							String msg = "Failed to get header line for file " + file.getName();
							logger.error(msg, e);
							fileProcessingReport.add(ReportType.ERROR, msg);
						}
					}
					break;
				}
			}
			if (!isRefsetFilePrepared) {
				//create empty delta and add warning message
				File refsetDeltFile = new File(outDir,refsetFileName);
				if (!refsetDeltFile .exists()) {
					try {
						refsetDeltFile.createNewFile();
						FileUtils.writeLines(refsetDeltFile, CharEncoding.UTF_8, Arrays.asList(headerLine.toString()), RF2Constants.LINE_ENDING);
						fileProcessingReport.add(ReportType.WARNING, refsetFileName, null, null,
								"No refset data found in any source therefore an empty delta file with header line only is created instead.");
					} catch (IOException e) {
						String msg = "Failed to create empty delta file " + refsetDeltFile.getName();
						logger.error(msg, e);
						fileProcessingReport.add(ReportType.ERROR, msg);
					}
				}
			}
		}
	}

	private String getHeaderLine(File preparedFile) throws IOException {
		try ( BufferedReader reader = new BufferedReader(new FileReader(preparedFile))) {
			 return reader.readLine();
		 }
	}

	private void initLocalDirs() {
		localDir = Files.createTempDir();
		outDir = new File(localDir, OUT_DIR);
		outDir.mkdir();
	}

	private File copySourceFilesToLocal(List<String> sourceFileLists) {
		for (String sourceFilePath : sourceFileLists) {
			 //Copy files from S3 to local for processing
			String s3FilePath = buildS3PathHelper.getProductSourcesPath(product).append(sourceFilePath).toString();
			try (InputStream sourceFileStream = fileHelper.getFileStream(s3FilePath)) {
				if (sourceFileStream == null) {
					fileProcessingReport.add(ReportType.ERROR, String.format("Source file not found in S3 %s", s3FilePath));
				}
				String sourceName = sourceFilePath.substring(0, sourceFilePath.indexOf("/"));
				//Keep track of the sources directories that are used
				availableSources.add(sourceName);
				File sourceDir = new File(localDir, sourceName);
				if (!sourceDir.exists()) {
					sourceDir.mkdir();
				}
				String fileName = FilenameUtils.getName(sourceFilePath);
				fileName = fileName.startsWith(RF2Constants.BETA_RELEASE_PREFIX) ? fileName.substring(1) : fileName;
				File outFile = new File(localDir + "/" + sourceName, fileName);
				FileUtils.copyInputStreamToFile(sourceFileStream, outFile);
				logger.info("Successfully created temp source file {}", outFile.getAbsolutePath());
				if (!sourceFilesMap.containsKey(sourceName)) {
					sourceFilesMap.put(sourceName, new ArrayList<>());
				}
				sourceFilesMap.get(sourceName).add(outFile.getAbsolutePath());
				fileOrKeyWithMultipleSources.add(fileName, sourceName);
			} catch (IOException e) {
				String errorMsg = String.format("Failed to copy source file %s to local disk", sourceFilePath);
				fileProcessingReport.add(ReportType.ERROR, errorMsg);
			}
		}
		for (String sourceName : sourceFilesMap.keySet()) {
			 fileProcessingReport.addSoureFiles(sourceName, sourceFilesMap.get(sourceName));
		}
		return localDir;
	}

	 void loadFileProcessConfigsFromManifest() {
		if (manifestStream == null) {
			fileProcessingReport.add(ReportType.ERROR, "Failed to load manifest");
		}
		try {
			ManifestXmlFileParser manifestXmlFileParser = new ManifestXmlFileParser();
			ListingType listingType = manifestXmlFileParser.parse(manifestStream);
			loadProcessConfig(listingType);
		} catch (Exception e) {
			String errorMsg = "Failed to parse manifest file.";
			logger.error(errorMsg, e);
			fileProcessingReport.add(ReportType.ERROR, errorMsg);
		}
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
						initTextDefinitionProcessingConfig(fileType);
					} else if (fileType.getContainsReferenceSets() != null && fileType.getContainsReferenceSets().getRefset() != null) {
						initRefsetProcessingConfig(fileType);
					} else if (fileType.getName().contains(INPUT_FILE_TYPE_DESCRIPTION)) {
						initDescripionProcessingConfig(fileType);
					} else {
						if(this.copyFilesDefinedInManifest) {
							String deltaFileName = fileType.getName();
							logger.debug("Add file to copy:" + deltaFileName);
							deltaFileName = (deltaFileName.startsWith(RF2Constants.BETA_RELEASE_PREFIX)) ? deltaFileName.replaceFirst(RF2Constants.BETA_RELEASE_PREFIX, "") : deltaFileName;
							if (fileType.getSources() != null) {
									this.filesToCopyFromSource.put(deltaFileName, fileType.getSources().getSource());
							} else {
									this.filesToCopyFromSource.put(deltaFileName, Collections.emptyList());
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

	void initDescripionProcessingConfig(FileType fileType) {
		if (fileType.getContainsLanguageCodes() != null && fileType.getContainsLanguageCodes().getCode() != null) {
			for (String languageCode : fileType.getContainsLanguageCodes().getCode()) {
				FileProcessingConfig config = new FileProcessingConfig(INPUT_FILE_TYPE_DESCRIPTION, languageCode, fileType.getName());
				if (!descriptionFileProcessingConfigs.containsKey(languageCode)) {
					descriptionFileProcessingConfigs.put(config.getKey(), config);
				}
				if (fileType.getSources() != null && !fileType.getSources().getSource().isEmpty()) {
					config.setSpecificSources(new HashSet<>(fileType.getSources().getSource()));
				}
			}
		} else {
			//add error reporting manifest.xml is not configured properly
			fileProcessingReport.add(ReportType.ERROR, fileType.getName(), null, null, NO_LANGUAGE_CODE_IS_CONFIGURED_MSG);
		}
	}

	void initRefsetProcessingConfig(FileType fileType) {
		if (fileType.getContainsAdditionalFields() != null && fileType.getContainsAdditionalFields().getField() != null ) {
			for (FieldType field : fileType.getContainsAdditionalFields().getField()) {
				String refsetFileName = fileType.getName();
				refsetFileName = (refsetFileName.startsWith(RF2Constants.BETA_RELEASE_PREFIX)) ?
						refsetFileName.replaceFirst(RF2Constants.BETA_RELEASE_PREFIX, "") : refsetFileName;
				refsetWithAdditionalFields.add(refsetFileName, field.getName());
			}
		}
		for (RefsetType refsetType : fileType.getContainsReferenceSets().getRefset()) {
			String refsetId = refsetType.getId().toString();
			FileProcessingConfig fileProcessingConfig = new FileProcessingConfig(INPUT_FILE_TYPE_REFSET, refsetType.getId().toString(), fileType.getName());
			if (!refsetFileProcessingConfigs.containsKey(refsetId)) {
				refsetFileProcessingConfigs.put(fileProcessingConfig.getKey(), fileProcessingConfig);
			}
			if (refsetType.getSources() != null && refsetType.getSources().getSource() != null && !refsetType.getSources().getSource().isEmpty()) {
				fileProcessingConfig.setSpecificSources(new HashSet<String>(refsetType.getSources().getSource()));
			} else {
				if (fileType.getSources() != null && !fileType.getSources().getSource().isEmpty()) {
					fileProcessingConfig.setSpecificSources(new HashSet<String>(fileType.getSources().getSource()));
				}
			}
		}
	}

	 void initTextDefinitionProcessingConfig(FileType fileType) {
		foundTextDefinitionFile = true;
		if (fileType.getContainsLanguageCodes() != null && fileType.getContainsLanguageCodes().getCode() != null) {
			for (String languageCode : fileType.getContainsLanguageCodes().getCode()) {
				FileProcessingConfig config = new FileProcessingConfig(INPUT_FILE_TYPE_TEXT_DEFINITON, languageCode, fileType.getName());
				if (fileType.getSources() != null && !fileType.getSources().getSource().isEmpty()) {
					config.setSpecificSources(new HashSet<>(fileType.getSources().getSource()));
				}
				if (!textDefinitionFileProcessingConfigs.containsKey(languageCode)) {
					textDefinitionFileProcessingConfigs.put(config.getKey(), config);
				}
			}
		} else {
			//add error reporting manifest.xml is not configured properly
			fileProcessingReport.add(ReportType.ERROR, fileType.getName(), null, null, NO_LANGUAGE_CODE_IS_CONFIGURED_MSG);
		}
	}

	private void prepareSourceFiles() {
		List<FileProcessingReportDetail> fileProcessingReportDetails = new ArrayList<>();
		for (String source : sourceFilesMap.keySet()) {
			List<String> fileList = sourceFilesMap.get(source);
			for (String fileName : fileList) {
				logger.info("Start processing file {}", fileName);
				File sourceFile = new File(fileName);
				try {
					List<String> lines = FileUtils.readLines(sourceFile, CharEncoding.UTF_8);
					if (lines != null && !lines.isEmpty()) {
						String header = lines.get(0);
						//remove header before processing
						lines.remove(0);
						if (header.startsWith(HEADER_REFSETS)) {
							processRefsetFiles(fileProcessingReportDetails,lines, source, fileName, outDir, header);
						} else if (header.startsWith(HEADER_TERM_DESCRIPTION)) {
							//create delta file with header
							writeHeaderToFile(outDir,header, descriptionFileProcessingConfigs.values());
							if (foundTextDefinitionFile) {
								writeHeaderToFile(outDir,header, textDefinitionFileProcessingConfigs.values());
							}
							processDescriptionsAndTextDefinitions(lines, source, fileName, outDir, header);
						} else {
							addFileToSkippedList(source, fileName);
						}
					}
					logger.info("Finish processing file {}", fileName);
				} catch (IOException e) {
					String msg = "Failed to prepare source file " + fileName;
					logger.error(msg, e);
					fileProcessingReport.add(ReportType.ERROR, msg);
				}
			}
		}
		fileProcessingReport.addReportDetails(fileProcessingReportDetails);
	}

	private void processRefsetFiles(List<FileProcessingReportDetail> fileProcessingReportDetails, List<String> lines, String sourceName,
			String inFileName, File outDir, String header) {

		 String inputFilename = FilenameUtils.getName(inFileName);
		if (lines == null || lines.isEmpty()) {
			fileProcessingReport.add(INFO, inputFilename, null, sourceName, NO_DATA_FOUND);
		}
		try {
			if (filesToCopyFromSource.containsKey(inputFilename) && (filesToCopyFromSource.get(inputFilename).isEmpty()
					|| filesToCopyFromSource.get(inputFilename).contains(sourceName))) {
				writeToFile(outDir, header, lines, inputFilename);
			} else {
				//map lines by refset id
				MultiValueMap<String, String> linesByRefsetId = new LinkedMultiValueMap<>();
				for (String line : lines) {
					String[] splits = line.split(TAB);
					String refsetId = splits[REFSETID_COL];
					linesByRefsetId.add(refsetId, line);
				}

				for (String refsetId : linesByRefsetId.keySet()) {
					fileOrKeyWithMultipleSources.add(refsetId, sourceName);
					FileProcessingConfig fileProcessingConfig = refsetFileProcessingConfigs.get(refsetId);
					if (fileProcessingConfig == null) {
						if (!filesToCopyFromSource.containsKey(inputFilename)) {
							String warningMsg = String.format("Found lines %d with refset id %s in source file "
									+ "but is not used by the manifest configuration", linesByRefsetId.get(refsetId).size(), refsetId);
							fileProcessingReport.add(ReportType.WARNING, inputFilename, refsetId, sourceName, warningMsg);
						}
					} else {
						if (fileProcessingConfig.getSpecificSources().contains(sourceName) ||
								(fileProcessingConfig.getSpecificSources().isEmpty() && fileOrKeyWithMultipleSources.get(refsetId).size() == 1)) {
							writeToFile(outDir, header, linesByRefsetId.get(refsetId), fileProcessingConfig.getTargetFileName());
							String infoMessage = String.format("Added source %s/%s", sourceName, FilenameUtils.getName(inFileName));
							fileProcessingReportDetails.add(new FileProcessingReportDetail(INFO, fileProcessingConfig.getTargetFileName(),
									refsetId, sourceName, infoMessage));
						} else if (fileProcessingConfig.getSpecificSources().isEmpty() && fileOrKeyWithMultipleSources.get(refsetId).size() > 1) {
							String errorMsg = String.format(UNPROCESSABLE_MSG, "refset id");
							fileProcessingReport.add(ERROR, inputFilename, refsetId, sourceName, errorMsg);
						} else {
							String warningMessage = String.format("Source %s is not specified in the manifest.xml therefore is skipped.", sourceName);
							fileProcessingReport.add(WARNING, inputFilename, refsetId, sourceName, warningMessage);
						}
					}
				}
			}
		} catch (IOException e) {
			String msg = "Failed to process input file " + inFileName;
			logger.error(msg, e);
			fileProcessingReport.add(ReportType.ERROR, msg);
		}
	}

	private void addFileToSkippedList(String sourceName, String filename) {
		if (skippedSourceFiles.get(sourceName) == null) {
			List<String> files = new ArrayList<>();
			files.add(filename);
			skippedSourceFiles.put(sourceName, files );
		} else {
			skippedSourceFiles.get(sourceName).add(filename);
		}
	}


	private void processDataByLanguageCode(Map<String,FileProcessingConfig> fileProcessingConfigs, String sourceName, Map<String, List<String>> rows,
			String inputFilename, String header, boolean isTextDefinition) throws IOException {
		for (String languageCode : rows.keySet()) {
			FileProcessingConfig fileProcessingConfig = fileProcessingConfigs.get(languageCode);
			if (fileProcessingConfig != null) {
				String key = isTextDefinition ? INPUT_FILE_TYPE_TEXT_DEFINITON + languageCode : INPUT_FILE_TYPE_DESCRIPTION + languageCode;
				if (fileProcessingConfig.getSpecificSources().contains(sourceName) ||
						(fileProcessingConfig.getSpecificSources().isEmpty() && fileOrKeyWithMultipleSources.get(key).size() == 1)) {
					writeToFile(outDir, header, rows.get(languageCode), fileProcessingConfig.getTargetFileName());
				} else if (fileProcessingConfig.getSpecificSources().isEmpty() && fileOrKeyWithMultipleSources.get(key).size() > 1) {
					String errorMsg = String.format(UNPROCESSABLE_MSG, "language code " + languageCode);
					fileProcessingReport.add(ERROR, inputFilename, null, sourceName, errorMsg);
				} else {
					String warningMsg = String.format("Source %s is not specified in the manifest.xml therefore is skipped.", sourceName);
					fileProcessingReport.add(WARNING, inputFilename , null, sourceName, warningMsg);
				}
			} else {
				String msg = String.format("Found language code: %s in source file but not specified in the manifest.xml", languageCode);
				fileProcessingReport.add(ERROR, inputFilename , null, sourceName, msg);
			}
		}
	}

	private void processDescriptionsAndTextDefinitions(List<String> lines, String sourceName, String inFileName, File outDir, String header) throws IOException {
		Map<String, List<String>> descriptionRows = new HashMap<>();
		Map<String, List<String>> textDefinitionRows = new HashMap<>();
		String inputFilename = FilenameUtils.getName(inFileName);
		if (lines == null || lines.isEmpty()) {
			fileProcessingReport.add(WARNING, inputFilename, null, sourceName, NO_DATA_FOUND);
		} else {
			for (String line : lines) {
				String[] splits = line.split(TAB);
				String languageCode = splits[DESCRIPTION_LANGUAGE_CODE_COL];
				String descriptionTypeValue = splits[DESCRIPTION_TYPE_COL];
				boolean isTextDefinition = false;
				if (foundTextDefinitionFile && TEXT_DEFINITION_TYPE_ID.equals(descriptionTypeValue)) {
					isTextDefinition = true;
					if (!textDefinitionRows.containsKey(languageCode)) {
						textDefinitionRows.put(languageCode, new ArrayList<String>());
					}
					textDefinitionRows.get(languageCode).add(line);
				} else {
					if (!descriptionRows.containsKey(languageCode)) {
						descriptionRows.put(languageCode, new ArrayList<String>());
					}
					descriptionRows.get(languageCode).add(line);
				}
				String key = isTextDefinition ? INPUT_FILE_TYPE_TEXT_DEFINITON + languageCode : INPUT_FILE_TYPE_DESCRIPTION + languageCode;
				if (!fileOrKeyWithMultipleSources.containsKey(key) || !fileOrKeyWithMultipleSources.get(key).contains(sourceName)) {
					fileOrKeyWithMultipleSources.add(key, sourceName);
				}
			}
			processDataByLanguageCode(descriptionFileProcessingConfigs, sourceName, descriptionRows, inputFilename, header, false);
			if (foundTextDefinitionFile) {
				 processDataByLanguageCode(textDefinitionFileProcessingConfigs, sourceName, textDefinitionRows, inputFilename, header, true);
			}
		}
	}

	private void writeHeaderToFile(File outDir, String headerLine, Collection<FileProcessingConfig> configs) throws IOException {
		if (configs != null) {
			Set<String> fileNamesToCreate = new HashSet<>();
			for (FileProcessingConfig config : configs) {
				fileNamesToCreate.add(config.getTargetFileName());
			}
			for (String fileName : fileNamesToCreate) {
				File file = new File(outDir, fileName);
				if (!file.exists()) {
					file.createNewFile();
					FileUtils.writeLines(file, CharEncoding.UTF_8, Arrays.asList(headerLine), RF2Constants.LINE_ENDING);
				}
			}
		}

	}

	private void writeToFile(File outDir, String header, List<String> lines, String targetOutputFileName) throws IOException {
		if (targetOutputFileName != null && header != null) {
			 File outFile = new File(outDir, targetOutputFileName);
			 if (!outFile.exists()) {
				 outFile.createNewFile();
				 List<String> headers = new ArrayList<>();
				 headers.add(header);
				 FileUtils.writeLines(outFile, CharEncoding.UTF_8, headers, RF2Constants.LINE_ENDING);
			 }
			 if (lines != null && !lines.isEmpty()) {
				 FileUtils.writeLines(outFile, CharEncoding.UTF_8, lines, RF2Constants.LINE_ENDING, true);
				 logger.debug("Copied {} lines to {}", lines.size(), outFile.getAbsolutePath());
			 }
		}
	}

	private FileProcessingReportDetail copyFilesWhenIgnoringCountryAndNamespace(String sourceFileName, String source,
			String sourceFilePath, Map<String,String>  fileNameMapWithoutNamespace) throws IOException {
		// ignores country and name space token check
		FileProcessingReportDetail reportDetail = null;
		String fileNameSpecifiedByManifest = fileNameMapWithoutNamespace.get(getFileNameWithoutCountryNameSpaceToken(sourceFileName));
		if (fileNameSpecifiedByManifest != null) {
			if (filesToCopyFromSource.containsKey(fileNameSpecifiedByManifest) && filesToCopyFromSource.get(fileNameSpecifiedByManifest).contains(source)) {
				File inFile = new File(sourceFilePath);
				File outFile = new File(outDir, fileNameSpecifiedByManifest);
				copyOrAppend(inFile, outFile);
				logger.info("Renamed {} to {}", inFile.getAbsolutePath(), outFile.getAbsolutePath());
				reportDetail = new FileProcessingReportDetail(INFO, sourceFileName, source, null, "Copied to:" + fileNameSpecifiedByManifest);
			} else {
				//source file is not required by the manifest
				String msg = "Skipped as not required by the manifest:" +  sourceFileName;
				logger.info(msg);
				reportDetail = new FileProcessingReportDetail(INFO, sourceFileName, source, null, msg);
			}
		} else {
			if (!sourceFileName.equals(README_HEADER_FILE_NAME)) {
				String msg = "Skipped as can't find any match in the manifest. "
						+ "Please check the file name is specified in the manifest and has the same release date as the source file.";
				reportDetail = new FileProcessingReportDetail(ReportType.WARNING, sourceFileName, null, source, msg);
			}
		}
		return reportDetail;
	}
	private List<FileProcessingReportDetail> copyFilesToOutputDir() throws IOException {
		List<FileProcessingReportDetail> reportDetails = new ArrayList<>();
		Map<String,String> fileNameMapWithoutNamespace = getFileNameMapWithoutNamespaceToken();
		for (String source : skippedSourceFiles.keySet()) {
			for (String sourceFilePath : skippedSourceFiles.get(source)) {
				String sourceFileName = FilenameUtils.getName(sourceFilePath);
				sourceFileName = sourceFileName.startsWith(RF2Constants.BETA_RELEASE_PREFIX) ? sourceFileName.substring(1) : sourceFileName;
				if (!filesToCopyFromSource.containsKey(sourceFileName)) {
					FileProcessingReportDetail reportDetail = copyFilesWhenIgnoringCountryAndNamespace(sourceFileName, source, sourceFilePath, fileNameMapWithoutNamespace);
					if (reportDetail != null) {
						reportDetails.add(reportDetail);
					}
				} else {
					 boolean toCopy = false;
					if (filesToCopyFromSource.get(sourceFileName).contains(source)) {
						//copy as specified by manifest
						reportDetails.add(new FileProcessingReportDetail(INFO, sourceFileName, null, source,
								"Copied as file name and source matched with the manifest exactly."));
						toCopy = true;
					} else {
						if (filesToCopyFromSource.get(sourceFileName).isEmpty()) {
							if (fileOrKeyWithMultipleSources.containsKey(sourceFileName) && fileOrKeyWithMultipleSources.get(sourceFileName).size() > 1) {
								//source is not specified. To check whether the same file exists in multiple sources.
								String errorMsg = String.format(UNPROCESSABLE_MSG, "file name");
								reportDetails.add(new FileProcessingReportDetail(ERROR, sourceFileName, null, source, errorMsg ));
							} else {
								//copy only one source
								toCopy = true;
								reportDetails.add(new FileProcessingReportDetail(INFO, sourceFileName, null, source,
										"Copied as the file name matched even though no source is specfied in the manifest.xml as it appears only in one source."));
							}
						} else {
							// skip it as is not from the given source specified by the manifest
							reportDetails.add(new FileProcessingReportDetail(INFO, sourceFileName, null, source ,
									"Skipped as only the file name matched with the manifest but not the source."));
						}
					}
					if (toCopy) {
						File inFile = new File(sourceFilePath);
						File outFile = new File(outDir, sourceFileName);
						try {
							copyOrAppend(inFile, outFile);
						} catch (IOException e) {
							String msg = String.format("Failed to copy file %s to %s", inFile.getAbsolutePath(), outFile.getAbsolutePath());
							logger.error(msg, e);
							throw e;
						}
						logger.info("Copied {} to {}", inFile.getAbsolutePath(), outFile.getAbsolutePath());
					}
				}
			}
			skippedSourceFiles.get(source).clear();
		}
		return reportDetails;
	}

	private void copyOrAppend(File sourceFile, File destinationFile) throws IOException {
	  if (destinationFile.exists()) {
			//remove header line and append
			List<String> lines = FileUtils.readLines(sourceFile, CharEncoding.UTF_8);
			if (!lines.isEmpty() && lines.size() > 0) {
				lines.remove(0);
				FileUtils.writeLines(destinationFile, CharEncoding.UTF_8, lines, RF2Constants.LINE_ENDING, true);
				logger.debug("Appending " + sourceFile.getName() + " to " + destinationFile.getAbsolutePath());
			}
		} else {
			destinationFile.createNewFile();
			FileUtils.copyFile(sourceFile, destinationFile);
			logger.debug("Copying " + sourceFile.getName() + " to " + destinationFile.getAbsolutePath());
		}
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
		for (String fileName : filesToCopyFromSource.keySet()) {
			result.put(getFileNameWithoutCountryNameSpaceToken(fileName), fileName);
		}
		return result;
	}

	private void uploadOutFilesToProductInputFiles() {
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
			if (!Normalizer.isNormalized(inputFileName, Form.NFC)) {
				inputFileName = Normalizer.normalize(inputFileName, Form.NFC);
			}
			String filePath =   buildS3PathHelper.getProductInputFilesPath(product) + inputFileName;
			fileProcessingReport.add(ReportType.INFO,inputFileName, null, null, "Uploaded to product input files directory");
			try {
				fileHelper.putFile(file,filePath);
			} catch (Exception e) {
				String msg = String.format("Failed to upload input file %s to S3 %s", inputFileName, filePath);
				logger.error(msg, e);
				fileProcessingReport.add(ReportType.ERROR, msg);
			}
			logger.info("Uploaded {} to product input files directory with name {}", file.getName(), inputFileName);
		}
		for (String filename : filesToCopyFromSource.keySet()) {
			if (!filesPrepared.contains(filename) && !filename.startsWith("Readme") && !filename.startsWith("release")) {
				String message = null;
				 if (filesToCopyFromSource.get(filename).isEmpty()) {
					 message = String.format("Required by manifest but not found in any source.");
				 } else {
					 message = String.format("Required by manifest but not found in source %s", filesToCopyFromSource.get(filename));
				 }
				fileProcessingReport.add(ERROR, filename, null, null, message);
			}
		}
	}

	public Map<String, List<String>> getSkippedSourceFiles() {
		return skippedSourceFiles;
	}

	public Set<String> getAvailableSources() {
		return availableSources;
	}

	public Map<String, FileProcessingConfig> getRefsetFileProcessingConfigs() {
		return refsetFileProcessingConfigs;
	}

	public Map<String, FileProcessingConfig> getDescriptionFileProcessingConfigs() {
		return descriptionFileProcessingConfigs;
	}

	public Map<String, FileProcessingConfig> getTextDefinitionFileProcessingConfigs() {
		return textDefinitionFileProcessingConfigs;
	}

	public MultiValueMap<String, String> getFilesToCopyFromSource() {
		return filesToCopyFromSource;
	}

	public Map<String, List<String>> getSourceFilesMap() {
		return sourceFilesMap;
	}

	public MultiValueMap<String, String> getRefsetWithAdditionalFields() {
		return refsetWithAdditionalFields;
	}

}

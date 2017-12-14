package org.ihtsdo.buildcloud.service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.ihtsdo.buildcloud.dao.BuildDAO;
import org.ihtsdo.buildcloud.dao.io.AsyncPipedStreamBean;
import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.BuildConfiguration;
import org.ihtsdo.buildcloud.entity.ExtensionConfig;
import org.ihtsdo.buildcloud.entity.ReleaseCenter;
import org.ihtsdo.buildcloud.service.RF2ClassifierService;
import org.ihtsdo.buildcloud.service.build.RF2BuildUtils;
import org.ihtsdo.buildcloud.service.build.RF2Constants;
import org.ihtsdo.buildcloud.service.build.transform.TransformationService;
import org.ihtsdo.buildcloud.service.classifier.ClassificationResult;
import org.ihtsdo.buildcloud.service.classifier.ExternalRF2ClassifierRestClient;
import org.ihtsdo.classifier.ClassificationException;
import org.ihtsdo.classifier.ClassificationRunner;
import org.ihtsdo.classifier.CycleCheck;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.otf.rest.exception.ProcessingException;
import org.ihtsdo.otf.utils.ZipFileUtils;
import org.ihtsdo.snomed.util.rf2.schema.ComponentType;
import org.ihtsdo.snomed.util.rf2.schema.SchemaFactory;
import org.ihtsdo.snomed.util.rf2.schema.TableSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StreamUtils;

import com.google.common.io.Files;

public  class RF2ClassifierService {

	private static final String CLASSIFIER_RESULT = "_classifier_result.txt";

	private static final String RECONCILED = "_reconciled.txt";

	@Autowired
	private BuildDAO buildDAO;

	@Autowired
	private String coreModuleSctid;

	@Autowired
	private TransformationService transformationService;

	@Autowired 
	private ExternalRF2ClassifierRestClient externalClassifierRestClient;
	
	private final Logger logger = LoggerFactory.getLogger(getClass());

	public enum Relationship {
		STATED, INFERRED
	};
	
	/**
	 * Checks for required files, performs cycle check then generates inferred relationships.
	 * @throws IOException 
	 */
	public ClassificationResult classify(final Build build, final Map<String, TableSchema> inputFileSchemaMap) throws BusinessServiceException {
		ClassifierFilesPojo classifierFiles = constructClassifierFilesPojo(inputFileSchemaMap);
		BuildConfiguration configuration = build.getConfiguration();
		if (!classifierFiles.isSufficientToClassify()) {
			logger.info("Stated relationship and concept files not present. Skipping classification.");
			return null;
		}
		File tempDir = Files.createTempDir();
		performStatedRelationshipCycleCheck(build, classifierFiles, tempDir);
		logger.info("No cycles in stated relationship snapshot. Performing classification...");
		Map<String,String> uuidToSctidMap = prepareFilesForClassifier(build,classifierFiles, tempDir);
		 File equivalencyReportOutputFile = new File(tempDir, RF2Constants.EQUIVALENCY_REPORT_TXT);
		 File classifierResultOutputFile = null;
		 boolean isSnapshot = true;
		if (!configuration.useExternalClassifier()) {
			classifierResultOutputFile = runInternalClassifier(configuration, equivalencyReportOutputFile, classifierFiles, tempDir);
		} else {
			isSnapshot = false;
			classifierResultOutputFile = runExternalClassifier(build, equivalencyReportOutputFile, classifierFiles, tempDir);
			if (classifierResultOutputFile == null) {
				throw new BusinessServiceException("No inferred relationship delta file found in the classificaiton result.");
			}
		}
		try {
		logger.info("Classification finished.");
		uploadLog(build, equivalencyReportOutputFile, RF2Constants.EQUIVALENCY_REPORT_TXT);
		
		// Upload classification results into S3 
		buildDAO.putTransformedFile(build, classifierResultOutputFile);
		String transformedInferredFile = getInferredFilename(classifierFiles, configuration.useExternalClassifier());
		transformationService.transformInferredRelationshipFile(build, new FileInputStream(classifierResultOutputFile), transformedInferredFile, uuidToSctidMap);
		ClassificationResult result = new ClassificationResult(transformedInferredFile, isSnapshot);
		return result;

	} catch (BusinessServiceException | IOException e) {
		String msg = "Failed to generate inferred relationship snapshot due to: ";
		msg += e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
		throw new ProcessingException(msg, e);
			} finally {
				FileUtils.deleteQuietly(tempDir);
			}
	}

	private ClassifierFilesPojo constructClassifierFilesPojo(Map<String, TableSchema> inputFileSchemaMap) {
		ClassifierFilesPojo classifierFiles = new ClassifierFilesPojo();
		for (final String inputFilename : inputFileSchemaMap.keySet()) {
			final TableSchema inputFileSchema = inputFileSchemaMap.get(inputFilename);
			if (inputFileSchema == null) {
				logger.warn("Failed to recover schema mapped to {}.", inputFilename);
				continue;
			}
			if (inputFileSchema.getComponentType() == ComponentType.CONCEPT) {
				classifierFiles.getConceptSnapshotFilenames().add(inputFilename.replace(SchemaFactory.REL_2, SchemaFactory.SCT_2).replace(RF2Constants.DELTA, RF2Constants.SNAPSHOT));
			} else if (inputFileSchema.getComponentType() == ComponentType.STATED_RELATIONSHIP) {
				classifierFiles.getStatedRelationshipSnapshotFilenames().add(inputFilename.replace(SchemaFactory.REL_2, SchemaFactory.SCT_2).replace(RF2Constants.DELTA, RF2Constants.SNAPSHOT));
			}
		}
		return classifierFiles;
	}

	private String getInferredFilename(ClassifierFilesPojo classifierFiles, boolean isExternal) {
		String localStated = classifierFiles.getLocalStatedRelationshipFilePaths().iterator().next();
		if (!isExternal) {
			String inferredRelationshipSnapshotFilename = localStated.substring(localStated.lastIndexOf("/") + 1)
					.replace(ComponentType.STATED_RELATIONSHIP.toString(), ComponentType.RELATIONSHIP.toString())
					.replace(RF2Constants.DELTA, RF2Constants.SNAPSHOT);
			return inferredRelationshipSnapshotFilename;
		} else {
			String inferredRelationshipDeltaFilename = localStated.substring(localStated.lastIndexOf("/") + 1)
					.replace(ComponentType.STATED_RELATIONSHIP.toString(), ComponentType.RELATIONSHIP.toString())
					.replace(RF2Constants.SNAPSHOT, RF2Constants.DELTA);
			return inferredRelationshipDeltaFilename;
		}
	}

	private File runExternalClassifier(Build build, File equivalencyReportOutputFile, ClassifierFilesPojo classifierFiles, File tempDir) throws BusinessServiceException {
		String previousPublished = build.getConfiguration().getPreviousPublishedPackage();
		File rf2DeltaZipFile = createRf2DeltaArchiveForClassifier(build, classifierFiles, tempDir);
		File inferredResult = null;
		try {
			File resultZipFile = externalClassifierRestClient.classify(rf2DeltaZipFile, previousPublished);
			File classifierResult = new File(tempDir, "result");
			if (!classifierResult.exists()) {
				classifierResult.mkdir();
			}
			if (classifierResult.exists() && classifierResult.isDirectory()) {
				ZipFileUtils.extractFilesFromZipToOneFolder(resultZipFile, classifierResult.getAbsolutePath().toString());
			} else {
				throw new BusinessServiceException("Failed to create folder to extract classification results:" + classifierResult);
			}
			for (File file : classifierResult.listFiles()) {
				if (file.getName().endsWith(".txt")) {
					if (file.getName().startsWith("sct2_Relationship_Delta")) {
						inferredResult = file;
					}
					if (file.getName().startsWith("der2_sRefset_EquivalentConceptSimpleMapDelta")) {
						FileUtils.copyFile(file, equivalencyReportOutputFile);
					}
				}
			}
			
		} catch (Exception e) {
			String errorMsg = "Error coccurred when running external classification due to:";
			if (e.getCause() != null) {
				errorMsg += e.getCause().getMessage();
			} else {
				errorMsg += e.getMessage();
			}
			throw new BusinessServiceException(errorMsg, e);
		}
		return inferredResult;
	}

	private File createRf2DeltaArchiveForClassifier(Build build, ClassifierFilesPojo classifierFiles, File tempDir) throws ProcessingException {
		//external classifier expects the delta files for concept, stated relationship, empty relationship and option MRCM attribute domain refset
		File rf2DeltaZipFile = new File(tempDir, "rf2Delta_" + build.getId() + ".zip");
		List<String> rf2DeltaFileList = new ArrayList<>();
		for ( String filename : classifierFiles.getStatedRelationshipSnapshotFilenames()) {
			rf2DeltaFileList.add(filename.replace(RF2Constants.SNAPSHOT, RF2Constants.DELTA));
		}
		
		for ( String filename : classifierFiles.getConceptSnapshotFilenames()) {
			rf2DeltaFileList.add(filename.replace(RF2Constants.SNAPSHOT, RF2Constants.DELTA));
		}
		
		File deltaTempDir = Files.createTempDir();
		downloadFiles(build, deltaTempDir, rf2DeltaFileList);
		//add empty relationship delta as the external classifier currently requires it to be present
		String inferredDeltaFile = null;
		try {
			inferredDeltaFile = createEmptyRelationshipDelta(deltaTempDir, rf2DeltaFileList);
		} catch (IOException e) {
			throw new ProcessingException("Error occured when creating empty relaitonship delta file.", e);
		}
		if (inferredDeltaFile == null) {
			throw new ProcessingException("Failed to create empty relaitonship delta file.");
		}
		try {
			ZipFileUtils.zip(deltaTempDir.getAbsolutePath(), rf2DeltaZipFile.getAbsolutePath());
		} catch (IOException e) {
			throw new ProcessingException("Failed to zip RF2 delta files.", e);
		}
		return rf2DeltaZipFile;
		
	}

	private String createEmptyRelationshipDelta(File deltaTempDir, List<String> rf2DeltaFileList) throws IOException {
		String inferredDelta = null;
		for (String filename : rf2DeltaFileList) {
			if (filename.contains(RF2Constants.STATED)) {
				inferredDelta = filename.replace(ComponentType.STATED_RELATIONSHIP.toString(), ComponentType.RELATIONSHIP.toString());
				File statedFile = new File(deltaTempDir, filename);
				File inferredDeltaFile = new File(deltaTempDir, inferredDelta);
				try (BufferedWriter writer = new BufferedWriter(new FileWriter(inferredDeltaFile));
					 BufferedReader reader = new BufferedReader(new FileReader(statedFile))) {
					String header = reader.readLine();
					writer.write(header);
					writer.write(RF2Constants.LINE_ENDING);
				} 
				break;
			}
		}
		return inferredDelta;
	}

	private File runInternalClassifier(BuildConfiguration config, File equivalencyReportOutputFile, ClassifierFilesPojo classifierFiles, File tempDir) throws BusinessServiceException {
		String effectiveTimeSnomedFormat = config.getEffectiveTimeSnomedFormat();
		List<String> localConceptFilePaths = new ArrayList<>(classifierFiles.getLocalConceptFilePaths());
		List<String> localStatedRelationshipFilePaths = new ArrayList<>(classifierFiles.getLocalStatedRelationshipFilePaths());
		List<String> previousInferredRelationshipFilePaths = classifierFiles.getLocalPreviousInferredRelationshipFilePaths();
		//Save the classifier result before transforming for debugging purpose.
		final String statedRelationshipDeltaPath = localStatedRelationshipFilePaths.iterator().next();
		final String inferredRelationshipSnapshotFilename = statedRelationshipDeltaPath.substring(statedRelationshipDeltaPath.lastIndexOf("/") + 1)
				.replace(ComponentType.STATED_RELATIONSHIP.toString(), ComponentType.RELATIONSHIP.toString())
				.replace(RF2Constants.DELTA, RF2Constants.SNAPSHOT);
		 File classifierResultOutputFile = new File(tempDir, inferredRelationshipSnapshotFilename.replace(RF2Constants.TXT_FILE_EXTENSION,  CLASSIFIER_RESULT));
		 String moduleId = coreModuleSctid;
		 ExtensionConfig extConfig = config.getExtensionConfig();
		 if (extConfig != null) {
			 moduleId = extConfig.getModuleId();
		 }
		ClassificationRunner classificationRunner = new ClassificationRunner(moduleId, effectiveTimeSnomedFormat,
					localConceptFilePaths, localStatedRelationshipFilePaths, previousInferredRelationshipFilePaths,
					classifierResultOutputFile.getAbsolutePath(), equivalencyReportOutputFile.getAbsolutePath());
			try {
				classificationRunner.execute();
			} catch (IOException | ClassificationException e) {
				throw new BusinessServiceException("Error when executing internal classifier.", e);
			}
			
		return classifierResultOutputFile;
		}

	private Map<String, String> prepareFilesForClassifier(Build build, ClassifierFilesPojo classifierFiles, File tempDir) throws BusinessServiceException {
		BuildConfiguration configuration = build.getConfiguration();
		final List<String> previousInferredRelationshipFilePaths = new ArrayList<>();
		classifierFiles.setLocalPreviousInferredRelationshipFilePaths(previousInferredRelationshipFilePaths);
		// Generate inferred relationship ids using transform looking up previous IDs where available
		Map<String, String> uuidToSctidMap = null;
		ExtensionConfig extConfig = build.getConfiguration().getExtensionConfig();
		if (extConfig != null) {
			String dependencyReleaseInferredSnapshot = downloadDependencyInferredRelationshipSnapshot(tempDir,build);
			previousInferredRelationshipFilePaths.add(dependencyReleaseInferredSnapshot);
			uuidToSctidMap = RelationshipHelper
					.buildUuidSctidMapFromPreviousRelationshipFile(dependencyReleaseInferredSnapshot,
							RF2Constants.RelationshipFileType.INFERRED);
		}
		String previousInferredRelationshipFilePath = null;
		if (!configuration.isFirstTimeRelease()) {
			previousInferredRelationshipFilePath = getPreviousRelationshipFilePath(build,classifierFiles.getStatedRelationshipSnapshotFilenames().get(0),
					tempDir,
					Relationship.INFERRED);
			if (previousInferredRelationshipFilePath != null) {
				previousInferredRelationshipFilePaths.add(previousInferredRelationshipFilePath);
				if (uuidToSctidMap == null) {
					uuidToSctidMap = RelationshipHelper
							.buildUuidSctidMapFromPreviousRelationshipFile(previousInferredRelationshipFilePath,
									RF2Constants.RelationshipFileType.INFERRED);
				} else {
					uuidToSctidMap.putAll(RelationshipHelper
							.buildUuidSctidMapFromPreviousRelationshipFile(previousInferredRelationshipFilePath,
									RF2Constants.RelationshipFileType.INFERRED));
				}
				
				logger.debug("Successfully build map of previously allocated inferred relationship SCTIDs");
			} else {
				logger.error(RF2Constants.DATA_PROBLEM + "No previous inferred relationship file found - unable to reconcile prior allocated SCTIDs.");
			}
		}

		List<String> localStatedRelationshipFilePaths = classifierFiles.getLocalStatedRelationshipFilePaths();
		//Add relationship id reconciliation fixes when extension overwrites international relationships
		if (extConfig != null) {
			logger.info("Reconcile extension relationship ids with the dependency international release.");
			reconcileRelationships(localStatedRelationshipFilePaths, previousInferredRelationshipFilePaths);
			if (!extConfig.isReleaseAsAnEdition()) {
				//reconcile concept ids
				reconcileConcepts(classifierFiles.getLocalConceptFilePaths());
			}
		}
		return uuidToSctidMap;
	}


	private void performStatedRelationshipCycleCheck(Build build, ClassifierFilesPojo classifierFiles, File tempDir) throws BusinessServiceException {
		//download concepts and stated relationships locally to perform stated relationship cycle check
		// Download snapshot files
		logger.info("Sufficient files for relationship classification. Downloading local copy...");
		final List<String> localConceptFilePaths = downloadFiles(build, tempDir, classifierFiles.getConceptSnapshotFilenames());
		classifierFiles.setLocalConceptFilePaths(localConceptFilePaths);
		logger.debug("Concept snapshot file downloaded:" + classifierFiles.getConceptSnapshotFilenames().get(0));
		final List<String> localStatedRelationshipFilePaths = downloadFiles(build, tempDir, classifierFiles.getStatedRelationshipSnapshotFilenames());
		classifierFiles.setLocalStatedRelationshipFilePaths(localStatedRelationshipFilePaths);
		logger.debug("Stated relationship snapshot file downloaded:" + classifierFiles.getStatedRelationshipSnapshotFilenames().get(0));
		ExtensionConfig extConfig = build.getConfiguration().getExtensionConfig();
		if (extConfig != null) {
			if (!extConfig.isReleaseAsAnEdition()) {
				// add extension dependency concept snapshot file
				logger.info("Downloading concepts and stated relationships from the dependency release for extension release.");
				String dependencyConceptSnapshotFileName = downloadDependencyConceptSnapshot(tempDir,build);
				localConceptFilePaths.add(dependencyConceptSnapshotFileName);
				String dependencyStatedRelationshipFilename = downloadDependencyStatedRelationshipSnapshot(tempDir,build);
				localStatedRelationshipFilePaths.add(dependencyStatedRelationshipFilename);
			}
		}
		final File cycleFile = new File(tempDir, RF2Constants.CONCEPTS_WITH_CYCLES_TXT);
		if (!checkNoStatedRelationshipCycles(build, localConceptFilePaths, localStatedRelationshipFilePaths, cycleFile)) {
			String msg = RF2Constants.DATA_PROBLEM + "Cycles detected in stated relationship snapshot file. "  
					+  "See " + RF2Constants.CONCEPTS_WITH_CYCLES_TXT + " in build package logs for more details.";
			logger.error(msg);
			throw new BusinessServiceException(msg);
		} 
	}
		

	private void reconcileConcepts(List<String> localConceptFilePaths) throws BusinessServiceException {
		if (localConceptFilePaths.size() == 2) {
			String reconciledConceptSnapshot = localConceptFilePaths.get(0).replace(RF2Constants.TXT_FILE_EXTENSION, RECONCILED);
			try {
				reconcileSnapshotFilesById(localConceptFilePaths.get(1), localConceptFilePaths.get(0), reconciledConceptSnapshot);
			} catch (IOException | ParseException e) {
				throw new BusinessServiceException("Error during concept snapshot files reconciliation", e);
			}
			localConceptFilePaths.clear();
			localConceptFilePaths.add(reconciledConceptSnapshot);
		}
	}

	/** Fix relationship ids are re-used in the extension
	 * @param localStatedRelationshipFilePaths
	 * @param previousInferredRelationshipFilePaths
	 * @throws BusinessServiceException 
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 * @throws ParseException 
	 */
	private void reconcileRelationships(List<String> localStatedRelationshipFilePaths, List<String> previousInferredRelationshipFilePaths) throws BusinessServiceException {
		//only fix when there are at least two stated relationships
		if (localStatedRelationshipFilePaths.size() > 1) {
			String reconciledStatedSnapshot = localStatedRelationshipFilePaths.get(1).replace(RF2Constants.TXT_FILE_EXTENSION, RECONCILED);
			try {
				reconcileSnapshotFilesById(localStatedRelationshipFilePaths.get(0), localStatedRelationshipFilePaths.get(1),reconciledStatedSnapshot);
			} catch (IOException | ParseException e) {
				throw new BusinessServiceException("Error during stated relationships reconciliation", e);
			}
			localStatedRelationshipFilePaths.clear();
			localStatedRelationshipFilePaths.add(reconciledStatedSnapshot);
		}
		
		if (previousInferredRelationshipFilePaths.size() > 1) {
			String reconciledInferredSnapshot = previousInferredRelationshipFilePaths.get(1).replace(RF2Constants.TXT_FILE_EXTENSION, RECONCILED);
			try {
				reconcileSnapshotFilesById(previousInferredRelationshipFilePaths.get(0), previousInferredRelationshipFilePaths.get(1), reconciledInferredSnapshot);
				logger.info("Previous inferred relationships reconciled and saved in the temp file:" + reconciledInferredSnapshot);
			} catch (IOException | ParseException e) {
				throw new BusinessServiceException("Error during inferred relationships reconciliation", e);
			}
			previousInferredRelationshipFilePaths.clear();
			previousInferredRelationshipFilePaths.add(reconciledInferredSnapshot);
		}
	}

	/** Reconcile snapshot files with the most recent effective time for the same id.
	 * @param internationalSnapshot
	 * @param extensionSnapshot
	 * @param reconciledSnapshot
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws ParseException
	 */
	private void reconcileSnapshotFilesById(String internationalSnapshot, String extensionSnapshot, String reconciledSnapshot) throws FileNotFoundException, IOException, ParseException {
		//load the extension file into map as it is smaller
		Map<String,String> extensionSnapshotFileInMap = loadSnapshotFileIntoMap(new File(extensionSnapshot));
		FastDateFormat formater = RF2Constants.DATE_FORMAT;
		try (BufferedReader reader = new BufferedReader(new FileReader(new File(internationalSnapshot)));
				BufferedWriter writer = new BufferedWriter(new FileWriter(new File(reconciledSnapshot)))) {
			String line = reader.readLine();
			writer.append(line);
			writer.append(RF2Constants.LINE_ENDING);
			String key = null;
			while ((line = reader.readLine()) != null ) {
				key = line.split(RF2Constants.COLUMN_SEPARATOR)[0];
				if (extensionSnapshotFileInMap.containsKey(key)) {
					String lineFromExtension = extensionSnapshotFileInMap.get(key);
					String effectTimeStrExt = lineFromExtension.split(RF2Constants.COLUMN_SEPARATOR)[1];
					String effectTimeStrInt = line.split(RF2Constants.COLUMN_SEPARATOR) [1];
					if (formater.parse(effectTimeStrExt).after(formater.parse(effectTimeStrInt))) {
						writer.append(lineFromExtension);
						writer.append(RF2Constants.LINE_ENDING);
					} else {
						writer.append(line);
						writer.append(RF2Constants.LINE_ENDING);
					}
					extensionSnapshotFileInMap.remove(key);
				} else {
					writer.append(line);
					writer.append(RF2Constants.LINE_ENDING);
				}
				key = null;
			}
			for (String extensionOnly : extensionSnapshotFileInMap.values()) {
				writer.append(extensionOnly);
				writer.append(RF2Constants.LINE_ENDING);
			}
		}
	} 
	
	private Map<String, String> loadSnapshotFileIntoMap(File file) throws FileNotFoundException, IOException{
		Map<String,String> resultMap = new HashMap<>();
		try ( BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), RF2Constants.UTF_8))) {
			String line = reader.readLine();
			while ( (line = reader.readLine() ) != null ) {
				if (!line.isEmpty()) {
					String[] splits = line.split(RF2Constants.COLUMN_SEPARATOR, -1);
					resultMap.put(splits[0],line);
				}
			}
		}
		return resultMap;
	}

	private String downloadDependencyInferredRelationshipSnapshot(File tempDir, Build build) throws BusinessServiceException {
		String dependencyReleasePackage = build.getConfiguration().getExtensionConfig().getDependencyRelease();
		//SnomedCT_Release_INT_20160131.zip
		String releaseDate = RF2BuildUtils.getReleaseDateFromReleasePackage(dependencyReleasePackage);
		logger.debug("Extension dependency release date:" + releaseDate);
		if (releaseDate != null) {
			String inferredRelationshipSnapshot = RF2Constants.INT_RELATIONSHIP_SNAPSHOT_PREFIX + releaseDate + RF2Constants.TXT_FILE_EXTENSION;
			logger.debug("dependency inferred relationship snapshot file name:" + inferredRelationshipSnapshot);
			return downloadDependencySnapshot(tempDir, dependencyReleasePackage, inferredRelationshipSnapshot);
		}
		return null;
	}



	private String downloadDependencyConceptSnapshot(File tempDir, Build build) throws BusinessServiceException {
		String dependencyReleasePackage = build.getConfiguration().getExtensionConfig().getDependencyRelease();
		String releaseDate = RF2BuildUtils.getReleaseDateFromReleasePackage(dependencyReleasePackage);
		if (releaseDate != null) {
			return downloadDependencySnapshot(tempDir, dependencyReleasePackage, RF2Constants.CONCEPT_SNAPSHOT_PREFIX + releaseDate + RF2Constants.TXT_FILE_EXTENSION);
		}
		return null;
	}



	private String downloadDependencySnapshot( File tempDir,String dependencyReleasePackage, String dependencySnapshotFilename) throws BusinessServiceException {
		final File localFile = new File(tempDir, dependencySnapshotFilename);
		try (InputStream publishedFileArchiveEntry = buildDAO.getPublishedFileArchiveEntry(RF2Constants.INT_RELEASE_CENTER ,
				dependencySnapshotFilename, dependencyReleasePackage);
			 FileOutputStream out = new FileOutputStream(localFile)) {
			if (publishedFileArchiveEntry != null) {
				StreamUtils.copy(publishedFileArchiveEntry, out);
				return localFile.getAbsolutePath();
			}
		} catch (IOException e) {
			throw new BusinessServiceException("Error found when downloading dependency file:" + dependencySnapshotFilename, e);
		}
		return null;
		
	}



	private String downloadDependencyStatedRelationshipSnapshot(File tempDir, Build build) throws BusinessServiceException {
		String dependencyReleasePackage = build.getConfiguration().getExtensionConfig().getDependencyRelease();
		//SnomedCT_Release_INT_20160131.zip
		String releaseDate = RF2BuildUtils.getReleaseDateFromReleasePackage(dependencyReleasePackage);
		if (releaseDate != null) {
			return downloadDependencySnapshot(tempDir, dependencyReleasePackage, RF2Constants.INT_STATED_RELATIONSHIP_SNAPSHOT_PREFIX + releaseDate + RF2Constants.TXT_FILE_EXTENSION);
		}
		return null;
	}



	public boolean checkNoStatedRelationshipCycles(final Build build, final List<String> localConceptFilePaths,
			final List<String> localStatedRelationshipFilePaths, final File cycleFile) throws ProcessingException {
		try {
			logger.info("Performing stated relationship cycle check...");
			final CycleCheck cycleCheck = new CycleCheck(localConceptFilePaths, localStatedRelationshipFilePaths, cycleFile.getAbsolutePath());
			final boolean cycleDetected = cycleCheck.cycleDetected();
			if (cycleDetected) {
				// Upload cycles file
				uploadLog(build, cycleFile, RF2Constants.CONCEPTS_WITH_CYCLES_TXT);
			}
			return !cycleDetected;
		} catch (IOException | ClassificationException e) {
			final String message = e.getMessage();
			throw new ProcessingException("Error during stated relationship cycle check: " +
					e.getClass().getSimpleName() + (message != null ? " - " + message : ""), e);
		}
	}

	public void uploadLog(final Build build, final File logFile, final String targetFilename) throws ProcessingException {
		try (FileInputStream in = new FileInputStream(logFile);
			 AsyncPipedStreamBean logFileOutputStream = buildDAO.getLogFileOutputStream(build, targetFilename)) {
			final OutputStream outputStream = logFileOutputStream.getOutputStream();
			StreamUtils.copy(in, outputStream);
			outputStream.close();
		} catch (final IOException e) {
			throw new ProcessingException("Failed to upload file " + targetFilename + ".", e);
		}
	}

	private String getPreviousRelationshipFilePath(final Build build, String relationshipFilename, final File tempDir, final Relationship relationshipType) throws BusinessServiceException {
		final String previousPublishedPackage = build.getConfiguration().getPreviousPublishedPackage();
		if (relationshipType == Relationship.INFERRED) {
			relationshipFilename = relationshipFilename.replace(RF2Constants.STATED, "");
		}
		final File localFile = new File(tempDir, relationshipFilename + ".previous_published");
		try (InputStream publishedFileArchiveEntry = buildDAO.getPublishedFileArchiveEntry(build.getProduct().getReleaseCenter(),
				relationshipFilename, previousPublishedPackage);
			 FileOutputStream out = new FileOutputStream(localFile)) {
			if (publishedFileArchiveEntry != null) {
				StreamUtils.copy(publishedFileArchiveEntry, out);
				return localFile.getAbsolutePath();
			}
		} catch (IOException e) {
			throw new BusinessServiceException("Error when downloading previous relationship file:" + relationshipFilename, e);
		}
		return null;
	}
	

	private List<String> downloadFiles(final Build build, final File tempDir, final List<String> filenameLists) throws ProcessingException {
		final List<String> localFilePaths = new ArrayList<>();
		boolean isBeta = build.getConfiguration().isBetaRelease();
		for (String downloadFilename : filenameLists) {
			if (isBeta) {
				downloadFilename = RF2Constants.BETA_RELEASE_PREFIX + downloadFilename;
			}
			final File localFile = new File(tempDir, downloadFilename);
			try (InputStream inputFileStream = buildDAO.getOutputFileInputStream(build, downloadFilename);
				 FileOutputStream out = new FileOutputStream(localFile)) {
				if (inputFileStream != null) {
					StreamUtils.copy(inputFileStream, out);
					localFilePaths.add(localFile.getAbsolutePath());
				} else {
					throw new ProcessingException("Didn't find output file:" + downloadFilename);
				}
			} catch (final IOException e) {
				throw new ProcessingException("Failed to download files for classification.", e);
			}
		}
		return localFilePaths;
	}	
	
	private static class ClassifierFilesPojo {
	
		private final List<String> conceptSnapshotFilenames;
		private final List<String> statedRelationshipSnapshotFilenames;
		private List<String> localPreviousInferredRelationshipFilePaths;
		private List<String> localConceptFilePaths;
		private List<String> localStatedRelationshipFilePaths;

		ClassifierFilesPojo() {
			conceptSnapshotFilenames = new ArrayList<>();
			statedRelationshipSnapshotFilenames = new ArrayList<>();
			localConceptFilePaths = new ArrayList<>();
			localPreviousInferredRelationshipFilePaths = new ArrayList<>();
			localStatedRelationshipFilePaths = new ArrayList<>();
		}

		public List<String> getLocalPreviousInferredRelationshipFilePaths() {
			return this.localPreviousInferredRelationshipFilePaths;
		}

		public void setLocalPreviousInferredRelationshipFilePaths(List<String> previousInferredRelationshipFilePaths) {
			this.localPreviousInferredRelationshipFilePaths = previousInferredRelationshipFilePaths;
		}

		public List<String> getLocalConceptFilePaths() {
			return this.localConceptFilePaths;
		}

		public List<String> getLocalStatedRelationshipFilePaths() {
			return this.localStatedRelationshipFilePaths;
		}

		public void setLocalStatedRelationshipFilePaths(List<String> localStatedRelationshipFilePaths) {
			this.localStatedRelationshipFilePaths = localStatedRelationshipFilePaths;
		}

		public void setLocalConceptFilePaths(List<String> localConceptFilePaths) {
			this.localConceptFilePaths = localConceptFilePaths;
		}

		public boolean isSufficientToClassify() {
			return !conceptSnapshotFilenames.isEmpty() && !statedRelationshipSnapshotFilenames.isEmpty();
		}

		public List<String> getConceptSnapshotFilenames() {
			return conceptSnapshotFilenames;
		}

		public List<String> getStatedRelationshipSnapshotFilenames() {
			return statedRelationshipSnapshotFilenames;
		}
	}
}

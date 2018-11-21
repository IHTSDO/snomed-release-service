package org.ihtsdo.buildcloud.service.classifier;

import static org.ihtsdo.buildcloud.service.build.RF2Constants.BETA_RELEASE_PREFIX;
import static org.ihtsdo.buildcloud.service.build.RF2Constants.COLUMN_SEPARATOR;
import static org.ihtsdo.buildcloud.service.build.RF2Constants.CONCEPTS_WITH_CYCLES_TXT;
import static org.ihtsdo.buildcloud.service.build.RF2Constants.CONCEPT_SNAPSHOT_PREFIX;
import static org.ihtsdo.buildcloud.service.build.RF2Constants.DATA_PROBLEM;
import static org.ihtsdo.buildcloud.service.build.RF2Constants.DATE_FORMAT;
import static org.ihtsdo.buildcloud.service.build.RF2Constants.DELTA;
import static org.ihtsdo.buildcloud.service.build.RF2Constants.INT_RELATIONSHIP_SNAPSHOT_PREFIX;
import static org.ihtsdo.buildcloud.service.build.RF2Constants.INT_RELEASE_CENTER;
import static org.ihtsdo.buildcloud.service.build.RF2Constants.INT_STATED_RELATIONSHIP_SNAPSHOT_PREFIX;
import static org.ihtsdo.buildcloud.service.build.RF2Constants.LINE_ENDING;
import static org.ihtsdo.buildcloud.service.build.RF2Constants.SNAPSHOT;
import static org.ihtsdo.buildcloud.service.build.RF2Constants.STATED;
import static org.ihtsdo.buildcloud.service.build.RF2Constants.TXT_FILE_EXTENSION;
import static org.ihtsdo.buildcloud.service.build.RF2Constants.UTF_8;

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

import org.apache.commons.lang3.time.FastDateFormat;
import org.ihtsdo.buildcloud.dao.BuildDAO;
import org.ihtsdo.buildcloud.dao.io.AsyncPipedStreamBean;
import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.BuildConfiguration;
import org.ihtsdo.buildcloud.entity.ExtensionConfig;
import org.ihtsdo.buildcloud.service.RF2ClassifierService.RelationshipType;
import org.ihtsdo.buildcloud.service.build.RF2BuildUtils;
import org.ihtsdo.classifier.ClassificationException;
import org.ihtsdo.classifier.ClassificationRunner;
import org.ihtsdo.classifier.CycleCheck;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.otf.rest.exception.ProcessingException;
import org.ihtsdo.snomed.util.rf2.schema.ComponentType;
import org.ihtsdo.snomed.util.rf2.schema.TableSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StreamUtils;

public class InternalRF2Classifier extends RF2Classifier{

	private static final String CLASSIFIER_RESULT = "_classifier_result.txt";
	private static final String RECONCILED = "_reconciled.txt";
	
	@Autowired
	private BuildDAO buildDAO;

	@Autowired
	private String coreModuleSctid;
	
	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	@Override
	public File run(Build build, File equivalencyReportOutputFile, ClassificationInputInfo classificationInputIno, File resultDir) throws BusinessServiceException {
		performStatedRelationshipCycleCheck(build, classificationInputIno, resultDir);
		logger.info("No cycles in stated relationship snapshot. Performing classification...");
		prepareFilesForClassifier(build, classificationInputIno, resultDir);
		return runInternalClassifier(build.getConfiguration(), equivalencyReportOutputFile, classificationInputIno, resultDir);
		
	}
	
	private File runInternalClassifier(BuildConfiguration config, File equivalencyReportOutputFile,
			ClassificationInputInfo classificationInputIno, File resultDir) throws BusinessServiceException {
		String effectiveTimeSnomedFormat = config.getEffectiveTimeSnomedFormat();
		List<String> localConceptFilePaths = new ArrayList<>(classificationInputIno.getLocalConceptFilePaths());
		List<String> localStatedRelationshipFilePaths = new ArrayList<>(classificationInputIno.getLocalStatedRelationshipFilePaths());
		List<String> previousInferredRelationshipFilePaths = classificationInputIno.getLocalPreviousInferredRelationshipFilePaths();
		//Save the classifier result before transforming for debugging purpose.
		String statedSnapshot = classificationInputIno.getStatedRelationshipFileNames().get(0);
		String inferredSnapshotFilename = statedSnapshot.replace(ComponentType.STATED_RELATIONSHIP.toString(), ComponentType.RELATIONSHIP.toString());
		File classifierResultOutputFile = new File(resultDir, inferredSnapshotFilename.replace(TXT_FILE_EXTENSION,  CLASSIFIER_RESULT));
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


	@Override
	public ClassificationInputInfo constructClassificationInputInfo(Map<String, TableSchema> inputFileSchemaMap) {
		ClassificationInputInfo pojo = new ClassificationInputInfo(false);
		for (final String inputFilename : inputFileSchemaMap.keySet()) {
			final TableSchema inputFileSchema = inputFileSchemaMap.get(inputFilename);
			if (inputFileSchema == null) {
				logger.warn("Failed to recover schema mapped to {}.", inputFilename);
				continue;
			}
			String updatedFilename = inputFileSchema.getFilename();
			if (inputFileSchema.getComponentType() == ComponentType.CONCEPT) {
				pojo.getConceptFileNames().add(updatedFilename.replace(DELTA, SNAPSHOT));
			} else if (inputFileSchema.getComponentType() == ComponentType.STATED_RELATIONSHIP) {
				pojo.getStatedRelationshipFileNames().add(updatedFilename.replace(DELTA, SNAPSHOT));
			}
		}
		return pojo;
	}
	
	private void prepareFilesForClassifier(Build build, ClassificationInputInfo classifierFiles, File tempDir) throws BusinessServiceException {
		BuildConfiguration configuration = build.getConfiguration();
		List<String> previousInferredRelationshipFilePaths = new ArrayList<>();
		// Generate inferred relationship ids using transform looking up previous IDs where available
		String previousInferredFileLocalPath = null;
		if (!configuration.isFirstTimeRelease()) {
			previousInferredFileLocalPath = downloadPreviousRelationshipFileLocally(build,
					classifierFiles.getStatedRelationshipFileNames().get(0), tempDir, RelationshipType.INFERRED);
			if (previousInferredFileLocalPath != null) {
				previousInferredRelationshipFilePaths.add(previousInferredFileLocalPath);
				logger.debug("Successfully build map of previously allocated inferred relationship SCTIDs");
			} else {
				throw new BusinessServiceException(DATA_PROBLEM + " No previous inferred relationship file found - unable to reconcile prior allocated SCTIDs.");
			}
		}
		
		ExtensionConfig extConfig = build.getConfiguration().getExtensionConfig();
		if (extConfig != null) {
			//need for all extension and edition release
			String dependencyReleaseInferredSnapshot = downloadDependencyInferredRelationshipSnapshot(tempDir,build);
			previousInferredRelationshipFilePaths.add(dependencyReleaseInferredSnapshot);
		}
		classifierFiles.setLocalPreviousInferredRelationshipFilePaths(previousInferredRelationshipFilePaths);
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
	}


	private void performStatedRelationshipCycleCheck(Build build, ClassificationInputInfo classifierFiles, File tempDir) throws BusinessServiceException {
		//download concepts and stated relationships locally to perform stated relationship cycle check
		// Download snapshot files
		logger.info("Sufficient files for relationship classification. Downloading local copy...");
		final List<String> localConceptFilePaths = downloadFiles(build, tempDir, classifierFiles.getConceptFileNames(), buildDAO);
		logger.debug("Concept snapshot file downloaded:" + localConceptFilePaths.get(0));
		final List<String> localStatedRelationshipFilePaths = downloadFiles(build, tempDir, classifierFiles.getStatedRelationshipFileNames(), buildDAO);
		logger.debug("Stated relationship snapshot file downloaded:" + localStatedRelationshipFilePaths.get(0));
		ExtensionConfig extConfig = build.getConfiguration().getExtensionConfig();
		if (extConfig != null && (!extConfig.isReleaseAsAnEdition())) {
			// add extension dependency concept snapshot file
			logger.info("Downloading concepts and stated relationships from the dependency release for extension release.");
			String dependencyConceptSnapshotFileName = downloadDependencyConceptSnapshot(tempDir,build);
			String dependencyStatedRelationshipFilename = downloadDependencyStatedRelationshipSnapshot(tempDir,build);
			if (dependencyConceptSnapshotFileName == null || dependencyStatedRelationshipFilename == null) {
				throw new BusinessServiceException("Concept and stated relationship snapshot file not found in the published dependent international release");
			}
			localConceptFilePaths.add(dependencyConceptSnapshotFileName);
			localStatedRelationshipFilePaths.add(dependencyStatedRelationshipFilename);
		}
		classifierFiles.setLocalConceptFilePaths(localConceptFilePaths);
		classifierFiles.setLocalStatedRelationshipFilePaths(localStatedRelationshipFilePaths);
		
		final File cycleFile = new File(tempDir, CONCEPTS_WITH_CYCLES_TXT);
		if (!checkNoStatedRelationshipCycles(build, localConceptFilePaths, localStatedRelationshipFilePaths, cycleFile)) {
			String msg = DATA_PROBLEM + "Cycles detected in stated relationship snapshot file. "  
					+  "See " + CONCEPTS_WITH_CYCLES_TXT + " in build package logs for more details.";
			logger.error(msg);
			throw new BusinessServiceException(msg);
		} 
	}
	
	
	private String downloadDependencyInferredRelationshipSnapshot(File tempDir, Build build) throws BusinessServiceException {
		String dependencyReleasePackage = build.getConfiguration().getExtensionConfig().getDependencyRelease();
		//SnomedCT_Release_INT_20160131.zip
		String releaseDate = RF2BuildUtils.getReleaseDateFromReleasePackage(dependencyReleasePackage);
		logger.debug("Extension dependency release date:" + releaseDate);
		if (releaseDate != null) {
			String inferredRelationshipSnapshot = INT_RELATIONSHIP_SNAPSHOT_PREFIX + releaseDate + TXT_FILE_EXTENSION;
			logger.debug("dependency inferred relationship snapshot file name:" + inferredRelationshipSnapshot);
			return downloadDependencySnapshot(tempDir, dependencyReleasePackage, inferredRelationshipSnapshot);
		}
		return null;
	}

	private String downloadDependencyConceptSnapshot(File tempDir, Build build) throws BusinessServiceException {
		String dependencyReleasePackage = build.getConfiguration().getExtensionConfig().getDependencyRelease();
		String releaseDate = RF2BuildUtils.getReleaseDateFromReleasePackage(dependencyReleasePackage);
		if (releaseDate != null) {
			return downloadDependencySnapshot(tempDir, dependencyReleasePackage, CONCEPT_SNAPSHOT_PREFIX + releaseDate + TXT_FILE_EXTENSION);
		}
		return null;
	}
	
	
	private String downloadPreviousRelationshipFileLocally(final Build build, String relationshipFilename, final File tempDir, final RelationshipType relationshipType) throws BusinessServiceException {
		final String previousPublishedPackage = build.getConfiguration().getPreviousPublishedPackage();
		if (relationshipType == RelationshipType.INFERRED) {
			relationshipFilename = relationshipFilename.replace(STATED, "");
		}
		if (build.getConfiguration().isBetaRelease() && relationshipFilename.startsWith(BETA_RELEASE_PREFIX)) {
			relationshipFilename = relationshipFilename.replaceFirst(BETA_RELEASE_PREFIX, "");
		}
		final File localFile = new File(tempDir, relationshipFilename);
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
	
	private String downloadDependencyStatedRelationshipSnapshot(File tempDir, Build build) throws BusinessServiceException {
		String dependencyReleasePackage = build.getConfiguration().getExtensionConfig().getDependencyRelease();
		//SnomedCT_Release_INT_20160131.zip
		String releaseDate = RF2BuildUtils.getReleaseDateFromReleasePackage(dependencyReleasePackage);
		if (releaseDate != null) {
			return downloadDependencySnapshot(tempDir, dependencyReleasePackage, INT_STATED_RELATIONSHIP_SNAPSHOT_PREFIX + releaseDate + TXT_FILE_EXTENSION);
		}
		return null;
	}
	

	private String downloadDependencySnapshot( File tempDir,String dependencyReleasePackage, String dependencySnapshotFilename) throws BusinessServiceException {
		final File localFile = new File(tempDir, dependencySnapshotFilename);
		try (InputStream publishedFileArchiveEntry = buildDAO.getPublishedFileArchiveEntry(INT_RELEASE_CENTER ,
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
	
	public boolean checkNoStatedRelationshipCycles(final Build build, final List<String> localConceptFilePaths,
			final List<String> localStatedRelationshipFilePaths, final File cycleFile) throws ProcessingException {
		try {
			logger.info("Performing stated relationship cycle check...");
			final CycleCheck cycleCheck = new CycleCheck(localConceptFilePaths, localStatedRelationshipFilePaths, cycleFile.getAbsolutePath());
			final boolean cycleDetected = cycleCheck.cycleDetected();
			if (cycleDetected) {
				// Upload cycles file
				uploadLog(build, cycleFile, CONCEPTS_WITH_CYCLES_TXT);
			}
			return !cycleDetected;
		} catch (IOException | ClassificationException e) {
			final String message = e.getMessage();
			throw new ProcessingException("Error during stated relationship cycle check: " +
					e.getClass().getSimpleName() + (message != null ? " - " + message : ""), e);
		}
	}


	private void reconcileConcepts(List<String> localConceptFilePaths) throws BusinessServiceException {
		if (localConceptFilePaths.size() == 2) {
			String reconciledConceptSnapshot = localConceptFilePaths.get(0).replace(TXT_FILE_EXTENSION, RECONCILED);
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
			String reconciledStatedSnapshot = localStatedRelationshipFilePaths.get(0).replace(TXT_FILE_EXTENSION, RECONCILED);
			try {
				reconcileSnapshotFilesById(localStatedRelationshipFilePaths.get(1), localStatedRelationshipFilePaths.get(0), reconciledStatedSnapshot);
			} catch (IOException | ParseException e) {
				throw new BusinessServiceException("Error during stated relationships reconciliation", e);
			}
			localStatedRelationshipFilePaths.clear();
			localStatedRelationshipFilePaths.add(reconciledStatedSnapshot);
		}
		
		if (previousInferredRelationshipFilePaths.size() > 1) {
			String reconciledInferredSnapshot = previousInferredRelationshipFilePaths.get(0).replace(TXT_FILE_EXTENSION, RECONCILED);
			try {
				reconcileSnapshotFilesById(previousInferredRelationshipFilePaths.get(1), previousInferredRelationshipFilePaths.get(0), reconciledInferredSnapshot);
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
		FastDateFormat formater = DATE_FORMAT;
		try (BufferedReader reader = new BufferedReader(new FileReader(new File(internationalSnapshot)));
				BufferedWriter writer = new BufferedWriter(new FileWriter(new File(reconciledSnapshot)))) {
			String line = reader.readLine();
			writer.append(line);
			writer.append(LINE_ENDING);
			String key = null;
			while ((line = reader.readLine()) != null ) {
				key = line.split(COLUMN_SEPARATOR)[0];
				if (extensionSnapshotFileInMap.containsKey(key)) {
					String lineFromExtension = extensionSnapshotFileInMap.get(key);
					String effectTimeStrExt = lineFromExtension.split(COLUMN_SEPARATOR)[1];
					String effectTimeStrInt = line.split(COLUMN_SEPARATOR) [1];
					if (formater.parse(effectTimeStrExt).after(formater.parse(effectTimeStrInt))) {
						writer.append(lineFromExtension);
						writer.append(LINE_ENDING);
					} else {
						writer.append(line);
						writer.append(LINE_ENDING);
					}
					extensionSnapshotFileInMap.remove(key);
				} else {
					writer.append(line);
					writer.append(LINE_ENDING);
				}
				key = null;
			}
			for (String extensionOnly : extensionSnapshotFileInMap.values()) {
				writer.append(extensionOnly);
				writer.append(LINE_ENDING);
			}
		}
	} 
	
	private Map<String, String> loadSnapshotFileIntoMap(File file) throws FileNotFoundException, IOException{
		Map<String,String> resultMap = new HashMap<>();
		try ( BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), UTF_8))) {
			String line = reader.readLine();
			while ( (line = reader.readLine() ) != null ) {
				if (!line.isEmpty()) {
					String[] splits = line.split(COLUMN_SEPARATOR, -1);
					resultMap.put(splits[0],line);
				}
			}
		}
		return resultMap;
	}
	
	
	private void uploadLog(final Build build, final File logFile, final String targetFilename) throws ProcessingException {
		try (FileInputStream in = new FileInputStream(logFile);
			 AsyncPipedStreamBean logFileOutputStream = buildDAO.getLogFileOutputStream(build, targetFilename)) {
			final OutputStream outputStream = logFileOutputStream.getOutputStream();
			StreamUtils.copy(in, outputStream);
			outputStream.close();
		} catch (final IOException e) {
			throw new ProcessingException("Failed to upload file " + targetFilename + ".", e);
		}
	}
}

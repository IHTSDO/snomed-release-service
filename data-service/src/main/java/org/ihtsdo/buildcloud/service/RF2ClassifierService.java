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

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.ihtsdo.buildcloud.dao.BuildDAO;
import org.ihtsdo.buildcloud.dao.io.AsyncPipedStreamBean;
import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.BuildConfiguration;
import org.ihtsdo.buildcloud.entity.ExtensionConfig;
import org.ihtsdo.buildcloud.entity.ReleaseCenter;
import org.ihtsdo.buildcloud.service.build.RF2BuildUtils;
import org.ihtsdo.buildcloud.service.build.RF2Constants;
import org.ihtsdo.buildcloud.service.build.transform.TransformationService;
import org.ihtsdo.classifier.ClassificationException;
import org.ihtsdo.classifier.ClassificationRunner;
import org.ihtsdo.classifier.CycleCheck;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.otf.rest.exception.ProcessingException;
import org.ihtsdo.snomed.util.rf2.schema.ComponentType;
import org.ihtsdo.snomed.util.rf2.schema.SchemaFactory;
import org.ihtsdo.snomed.util.rf2.schema.TableSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StreamUtils;

import com.google.common.io.Files;

public class RF2ClassifierService {

	private static final String RECONCILED = "_reconciled.txt";

	@Autowired
	private BuildDAO buildDAO;

	@Autowired
	private String coreModuleSctid;

	@Autowired
	private TransformationService transformationService;
	
	private final Logger logger = LoggerFactory.getLogger(getClass());

	public enum Relationship {
		STATED, INFERRED
	};

	/**
	 * Checks for required files, performs cycle check then generates inferred relationships.
	 */
	public String generateInferredRelationshipSnapshot(final Build build, final Map<String, TableSchema> inputFileSchemaMap) throws BusinessServiceException {
		final ClassifierFilesPojo classifierFiles = new ClassifierFilesPojo();
		final BuildConfiguration configuration = build.getConfiguration();
		// Collect names of concept and relationship output files
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
		if (!classifierFiles.isSufficientToClassify()) {
			logger.info("Stated relationship and concept files not present. Skipping classification.");
			return null;
		}
			File tempDir = null;
			try {
				// Download snapshot files
				tempDir = Files.createTempDir();
				logger.info("Sufficient files for relationship classification. Downloading local copy...");
				final List<String> localConceptFilePaths = downloadFiles(build, tempDir, classifierFiles.getConceptSnapshotFilenames());
				logger.debug("Concept snapshot file downloaded:" + classifierFiles.getConceptSnapshotFilenames().get(0));
				final List<String> localStatedRelationshipFilePaths = downloadFiles(build, tempDir, classifierFiles.getStatedRelationshipSnapshotFilenames());
				logger.debug("Stated relationship snapshot file downloaded:" + classifierFiles.getStatedRelationshipSnapshotFilenames().get(0));
				ExtensionConfig extConfig = build.getConfiguration().getExtensionConfig();
				String moduleId = coreModuleSctid;
				if (extConfig != null) {
					moduleId = extConfig.getModuleId();
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
				if (checkNoStatedRelationshipCycles(build, localConceptFilePaths, localStatedRelationshipFilePaths,
						cycleFile)) {

					logger.info("No cycles in stated relationship snapshot. Performing classification...");

					final String effectiveTimeSnomedFormat = configuration.getEffectiveTimeSnomedFormat();
					final List<String> previousInferredRelationshipFilePaths = new ArrayList<>();
					// Generate inferred relationship ids using transform looking up previous IDs where available
					Map<String, String> uuidToSctidMap = null;
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

					final String statedRelationshipDeltaPath = localStatedRelationshipFilePaths.iterator().next();
					final String inferredRelationshipSnapshotFilename = statedRelationshipDeltaPath.substring(statedRelationshipDeltaPath.lastIndexOf("/") + 1)
							.replace(ComponentType.STATED_RELATIONSHIP.toString(), ComponentType.RELATIONSHIP.toString())
							.replace(RF2Constants.DELTA, RF2Constants.SNAPSHOT);
					
					//Save the classifier result before transforming for debugging purpose.
					final File classifierResultOutputFile = new File(tempDir, inferredRelationshipSnapshotFilename.replace(RF2Constants.TXT_FILE_EXTENSION,  "_classifier_result.txt"));
					final File equivalencyReportOutputFile = new File(tempDir, RF2Constants.EQUIVALENCY_REPORT_TXT);
					
					//Add relationship id reconciliation fixes when extension overwrites international relationships
					if (extConfig != null) {
						logger.info("Reconcile extension relationship ids with the dependency international release.");
						reconcileRelationships(localStatedRelationshipFilePaths, previousInferredRelationshipFilePaths);
					}
					final ClassificationRunner classificationRunner = new ClassificationRunner(moduleId, effectiveTimeSnomedFormat,
							localConceptFilePaths, localStatedRelationshipFilePaths, previousInferredRelationshipFilePaths,
							classifierResultOutputFile.getAbsolutePath(), equivalencyReportOutputFile.getAbsolutePath());
					classificationRunner.execute();

					logger.info("Classification finished.");

					uploadLog(build, equivalencyReportOutputFile, RF2Constants.EQUIVALENCY_REPORT_TXT);
					
					// Upload inferred relationships file with null ids
					buildDAO.putTransformedFile(build, classifierResultOutputFile);

					transformationService.transformInferredRelationshipFile(build, new FileInputStream(classifierResultOutputFile), inferredRelationshipSnapshotFilename, uuidToSctidMap);

					return inferredRelationshipSnapshotFilename;
				} else {
					logger.error(RF2Constants.DATA_PROBLEM + "Cycles detected in stated relationship snapshot file. " +
							"See " + RF2Constants.CONCEPTS_WITH_CYCLES_TXT + " in build package logs for more details.");
					return null;
				}
			} catch (ClassificationException | IOException e) {
				throw new ProcessingException("Failed to generate inferred relationship snapshot.", e);
			} finally {
				FileUtils.deleteQuietly(tempDir);
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
				reconcileSnapshotFilesByRelationshipId(localStatedRelationshipFilePaths.get(0), localStatedRelationshipFilePaths.get(1),reconciledStatedSnapshot);
			} catch (IOException | ParseException e) {
				throw new BusinessServiceException("Error during stated relationships reconciliation", e);
			}
			localStatedRelationshipFilePaths.clear();
			localStatedRelationshipFilePaths.add(reconciledStatedSnapshot);
		}
		
		if (previousInferredRelationshipFilePaths.size() > 1) {
			String reconciledInferredSnapshot = previousInferredRelationshipFilePaths.get(1).replace(RF2Constants.TXT_FILE_EXTENSION, RECONCILED);
			try {
				reconcileSnapshotFilesByRelationshipId(previousInferredRelationshipFilePaths.get(0), previousInferredRelationshipFilePaths.get(1), reconciledInferredSnapshot);
				logger.info("Previous inferred relationships reconciled and saved in the temp file:" + reconciledInferredSnapshot);
			} catch (IOException | ParseException e) {
				throw new BusinessServiceException("Error during inferred relationships reconciliation", e);
			}
			previousInferredRelationshipFilePaths.clear();
			previousInferredRelationshipFilePaths.add(reconciledInferredSnapshot);
		}
	}

	private void reconcileSnapshotFilesByRelationshipId(String internationalSnapshot, String extensionSnapshot, String reconciledSnapshot) throws FileNotFoundException, IOException, ParseException {
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

	private String downloadDependencyInferredRelationshipSnapshot(File tempDir, Build build) throws IOException {
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



	private String downloadDependencyConceptSnapshot(File tempDir, Build build) throws IOException {
		String dependencyReleasePackage = build.getConfiguration().getExtensionConfig().getDependencyRelease();
		String releaseDate = RF2BuildUtils.getReleaseDateFromReleasePackage(dependencyReleasePackage);
		if (releaseDate != null) {
			return downloadDependencySnapshot(tempDir, dependencyReleasePackage, RF2Constants.CONCEPT_SNAPSHOT_PREFIX + releaseDate + RF2Constants.TXT_FILE_EXTENSION);
		}
		return null;
	}



	private String downloadDependencySnapshot( File tempDir,String dependencyReleasePackage, String dependencySnapshotFilename) throws IOException {
		final File localFile = new File(tempDir, dependencySnapshotFilename);
		try (InputStream publishedFileArchiveEntry = buildDAO.getPublishedFileArchiveEntry(RF2Constants.INT_RELEASE_CENTER ,
				dependencySnapshotFilename, dependencyReleasePackage);
			 FileOutputStream out = new FileOutputStream(localFile)) {
			if (publishedFileArchiveEntry != null) {
				StreamUtils.copy(publishedFileArchiveEntry, out);
				return localFile.getAbsolutePath();
			}
		}
		return null;
		
	}



	private String downloadDependencyStatedRelationshipSnapshot(File tempDir, Build build) throws IOException {
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

	private String getPreviousRelationshipFilePath(final Build build, String relationshipFilename, final File tempDir, final Relationship relationshipType) throws IOException {
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
		}

		return null;
	}
	

	private List<String> downloadFiles(final Build build, final File tempDir, final List<String> filenameLists) throws ProcessingException {
		final List<String> localFilePaths = new ArrayList<>();
		boolean isBeta = build.getConfiguration().isBetaRelease();
		for (String downloadFilename : filenameLists) {
			if (isBeta) {
				downloadFilename = "x" + downloadFilename;
			}
			final File localFile = new File(tempDir, downloadFilename);
			try (InputStream inputFileStream = buildDAO.getOutputFileInputStream(build, downloadFilename);
				 FileOutputStream out = new FileOutputStream(localFile)) {
				if (inputFileStream != null) {
					StreamUtils.copy(inputFileStream, out);
					localFilePaths.add(localFile.getAbsolutePath());
				} else {
					throw new ProcessingException("Didn't find output file " + downloadFilename);
				}
			} catch (final IOException e) {
				throw new ProcessingException("Failed to download snapshot file for classifier cycle check.", e);
			}
		}
		return localFilePaths;
	}

	private static class ClassifierFilesPojo {

		private final List<String> conceptSnapshotFilenames;
		private final List<String> statedRelationshipSnapshotFilenames;

		ClassifierFilesPojo() {
			conceptSnapshotFilenames = new ArrayList<>();
			statedRelationshipSnapshotFilenames = new ArrayList<>();
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

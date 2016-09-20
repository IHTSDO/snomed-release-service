package org.ihtsdo.buildcloud.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.ihtsdo.buildcloud.dao.BuildDAO;
import org.ihtsdo.buildcloud.dao.io.AsyncPipedStreamBean;
import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.BuildConfiguration;
import org.ihtsdo.buildcloud.entity.ExtensionConfig;
import org.ihtsdo.buildcloud.entity.ReleaseCenter;
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
				final List<String> localStatedRelationshipFilePaths = downloadFiles(build, tempDir, classifierFiles.getStatedRelationshipSnapshotFilenames());
				ExtensionConfig extConfig = build.getConfiguration().getExtensionConfig();
				String moduleId = coreModuleSctid;
				if (extConfig != null) {
					moduleId = extConfig.getModuleId();
					// add extension dependency concept snapshot file
					String dependencyConceptSnapshotFileName = downloadDependencyConceptSnapshot(tempDir,build);
					localConceptFilePaths.add(dependencyConceptSnapshotFileName);
					String dependencyStatedRelationshipFilename = downloadDependencyStatedRelationshipSnapshot(tempDir,build);
					localStatedRelationshipFilePaths.add(dependencyStatedRelationshipFilename);
					
				}

				final File cycleFile = new File(tempDir, RF2Constants.CONCEPTS_WITH_CYCLES_TXT);
				if (checkNoStatedRelationshipCycles(build, localConceptFilePaths, localStatedRelationshipFilePaths,
						cycleFile)) {

					logger.info("No cycles in stated relationship snapshot. Performing classification...");

					final String effectiveTimeSnomedFormat = configuration.getEffectiveTimeSnomedFormat();
					final List<String> previousInferredRelationshipFilePaths = new ArrayList<>();
					if (extConfig != null) {
						previousInferredRelationshipFilePaths.add(downloadDependencyInferredRelationshipSnapshot(tempDir,build));
					}
					String previousInferredRelationshipFilePath = null;

					// Generate inferred relationship ids using transform looking up previous IDs where available
					Map<String, String> uuidToSctidMap = null;
					if (!configuration.isFirstTimeRelease()) {
						final String currentRelationshipFilename = classifierFiles.getStatedRelationshipSnapshotFilenames().get(0);
						previousInferredRelationshipFilePath = getPreviousRelationshipFilePath(build, currentRelationshipFilename,
								tempDir,
								Relationship.INFERRED);
						if (previousInferredRelationshipFilePath != null) {
							previousInferredRelationshipFilePaths.add(previousInferredRelationshipFilePath);
							uuidToSctidMap = RelationshipHelper
									.buildUuidSctidMapFromPreviousRelationshipFile(previousInferredRelationshipFilePath,
											RF2Constants.RelationshipFileType.INFERRED);
							logger.debug("Successfully build map of previously allocated inferred relationship SCTIDs");
						} else {
							logger.info(RF2Constants.DATA_PROBLEM + "No previous inferred relationship file found - unable to reconcile prior allocated SCTIDs.");
						}
					}

					final String statedRelationshipDeltaPath = localStatedRelationshipFilePaths.iterator().next();
					final String inferredRelationshipSnapshotFilename = statedRelationshipDeltaPath.substring(statedRelationshipDeltaPath.lastIndexOf("/") + 1)
							.replace(ComponentType.STATED_RELATIONSHIP.toString(), ComponentType.RELATIONSHIP.toString())
							.replace(RF2Constants.DELTA, RF2Constants.SNAPSHOT);

					final File classifierInferredRelationshipResultOutputFile = new File(tempDir, inferredRelationshipSnapshotFilename.replace(RF2Constants.TXT_FILE_EXTENSION,  "_classifier_result.txt"));
					final File equivalencyReportOutputFile = new File(tempDir, RF2Constants.EQUIVALENCY_REPORT_TXT);
				
					final ClassificationRunner classificationRunner = new ClassificationRunner(moduleId, effectiveTimeSnomedFormat,
							localConceptFilePaths, localStatedRelationshipFilePaths, previousInferredRelationshipFilePaths,
							classifierInferredRelationshipResultOutputFile.getAbsolutePath(), equivalencyReportOutputFile.getAbsolutePath());
					classificationRunner.execute();

					logger.info("Classification finished.");

					uploadLog(build, equivalencyReportOutputFile, RF2Constants.EQUIVALENCY_REPORT_TXT);

					// Upload inferred relationships file with null ids
					buildDAO.putTransformedFile(build, classifierInferredRelationshipResultOutputFile);

					transformationService.transformInferredRelationshipFile(build, new FileInputStream(classifierInferredRelationshipResultOutputFile), inferredRelationshipSnapshotFilename, uuidToSctidMap);

					return inferredRelationshipSnapshotFilename;
				} else {
					logger.info(RF2Constants.DATA_PROBLEM + "Cycles detected in stated relationship snapshot file. " +
							"See " + RF2Constants.CONCEPTS_WITH_CYCLES_TXT + " in build package logs for more details.");
					return null;
				}
			} catch (ClassificationException | IOException e) {
				throw new ProcessingException("Failed to generate inferred relationship snapshot.", e);
			} finally {
				FileUtils.deleteQuietly(tempDir);
			}
	}


	private String getReleaseDateFromReleasePackage(String dependencyReleasePackage) {
		if (dependencyReleasePackage != null && dependencyReleasePackage.endsWith(RF2Constants.ZIP_FILE_EXTENSION)) {
			String [] splits = dependencyReleasePackage.replace(RF2Constants.ZIP_FILE_EXTENSION, "").split(RF2Constants.FILE_NAME_SEPARATOR);
			String releaseDate = splits[splits.length - 1];
			return releaseDate;
		}
		return null;
	}

	private String downloadDependencyInferredRelationshipSnapshot(File tempDir, Build build) throws IOException {
		String dependencyReleasePackage = build.getConfiguration().getExtensionConfig().getDependencyRelease();
		//SnomedCT_Release_INT_20160131.zip
		String releaseDate = getReleaseDateFromReleasePackage(dependencyReleasePackage);
		logger.debug("Extension dependency release date:" + releaseDate);
		if (releaseDate != null) {
			String inferredRelationshipSnapshot = RF2Constants.RELATIONSHIP_SNAPSHOT_PREFIX + releaseDate + RF2Constants.TXT_FILE_EXTENSION;
			logger.debug("dependency inferred relationship snapshot file name:" + inferredRelationshipSnapshot);
			return downloadDependencySnapshot(tempDir, dependencyReleasePackage, inferredRelationshipSnapshot);
		}
		return null;
	}



	private String downloadDependencyConceptSnapshot(File tempDir, Build build) throws IOException {
		String dependencyReleasePackage = build.getConfiguration().getExtensionConfig().getDependencyRelease();
		String releaseDate = getReleaseDateFromReleasePackage(dependencyReleasePackage);
		if (releaseDate != null) {
			return downloadDependencySnapshot(tempDir, dependencyReleasePackage, RF2Constants.CONCEPT_SNAPSHOT_PREFIX + releaseDate + RF2Constants.TXT_FILE_EXTENSION);
		}
		return null;
	}



	private String downloadDependencySnapshot( File tempDir,String dependencyReleasePackage, String dependencySnapshotFilename) throws IOException {
		final File localFile = new File(tempDir, dependencySnapshotFilename);
		ReleaseCenter intReleaseCenter = new ReleaseCenter("International Release Center", "International");
		try (InputStream publishedFileArchiveEntry = buildDAO.getPublishedFileArchiveEntry(intReleaseCenter ,
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
		String releaseDate = getReleaseDateFromReleasePackage(dependencyReleasePackage);
		if (releaseDate != null) {
			return downloadDependencySnapshot(tempDir, dependencyReleasePackage, RF2Constants.STATED_RELATIONSHIP_SNAPSHOT_PREFIX + releaseDate + RF2Constants.TXT_FILE_EXTENSION);
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

	public String getPreviousRelationshipFilePath(final Build build, String relationshipFilename, final File tempDir,
			final Relationship relationshipType) throws IOException {
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

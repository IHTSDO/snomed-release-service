package org.ihtsdo.buildcloud.service;

import com.google.common.io.Files;
import org.ihtsdo.buildcloud.dao.ExecutionDAO;
import org.ihtsdo.buildcloud.dao.io.AsyncPipedStreamBean;
import org.ihtsdo.buildcloud.entity.Execution;
import org.ihtsdo.buildcloud.entity.Package;
import org.ihtsdo.buildcloud.service.exception.ProcessingException;
import org.ihtsdo.buildcloud.service.execution.RF2Constants;
import org.ihtsdo.buildcloud.service.execution.transform.TransformationService;
import org.ihtsdo.classifier.ClassificationException;
import org.ihtsdo.classifier.ClassificationRunner;
import org.ihtsdo.classifier.CycleCheck;
import org.ihtsdo.snomed.util.rf2.schema.ComponentType;
import org.ihtsdo.snomed.util.rf2.schema.SchemaFactory;
import org.ihtsdo.snomed.util.rf2.schema.TableSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StreamUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RF2ClassifierService {

	@Autowired
	private ExecutionDAO executionDAO;

	@Autowired
	private String coreModuleSctid;

	@Autowired
	private TransformationService transformationService;

	private Logger logger = LoggerFactory.getLogger(getClass());

	/**
	 * Checks for required files, performs cycle check then generates inferred relationships.
	 */
	public String generateInferredRelationshipSnapshot(Execution execution, Package pkg, Map<String, TableSchema> inputFileSchemaMap) throws ProcessingException, IOException, ClassificationException {
		String packageBusinessKey = pkg.getBusinessKey();
		ClassifierFilesPojo classifierFiles = new ClassifierFilesPojo();

		// Collect names of concept and relationship output files
		for (String inputFilename : inputFileSchemaMap.keySet()) {
			TableSchema inputFileSchema = inputFileSchemaMap.get(inputFilename);

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

		if (classifierFiles.isSufficientToClassify()) {
			// Download snapshot files
			logger.info("Sufficient files for relationship classification. Downloading local copy...");
			File tempDir = Files.createTempDir();
			List<String> localConceptFilePaths = downloadFiles(execution, packageBusinessKey, tempDir, classifierFiles.getConceptSnapshotFilenames());
			List<String> localStatedRelationshipFilePaths = downloadFiles(execution, packageBusinessKey, tempDir, classifierFiles.getStatedRelationshipSnapshotFilenames());
			File cycleFile = new File(tempDir, RF2Constants.CONCEPTS_WITH_CYCLES_TXT);
			if (checkNoStatedRelationshipCycles(execution, packageBusinessKey, localConceptFilePaths, localStatedRelationshipFilePaths,
					cycleFile)) {

				logger.info("No cycles in stated relationship snapshot. Performing classification...");

				String effectiveTimeSnomedFormat = pkg.getBuild().getEffectiveTimeSnomedFormat();
				List<String> previousInferredRelationshipFilePaths = new ArrayList<>();
				if (!pkg.isFirstTimeRelease()) {
					String previousInferredRelationshipFilePath = getPreviousInferredRelationshipFilePath(execution, pkg, classifierFiles, tempDir);
					if (previousInferredRelationshipFilePath != null) {
						previousInferredRelationshipFilePaths.add(previousInferredRelationshipFilePath);
					} else {
						logger.info(RF2Constants.DATA_PROBLEM + "No previous inferred relationship file found.");
					}
				}

				String statedRelationshipDeltaPath = localStatedRelationshipFilePaths.iterator().next();
				String inferredRelationshipSnapshotFilename = statedRelationshipDeltaPath.substring(statedRelationshipDeltaPath.lastIndexOf("/") + 1)
						.replace(ComponentType.STATED_RELATIONSHIP.toString(), ComponentType.RELATIONSHIP.toString())
						.replace(RF2Constants.DELTA, RF2Constants.SNAPSHOT);

				File inferredRelationshipsOutputFile = new File(tempDir, inferredRelationshipSnapshotFilename);
				File equivalencyReportOutputFile = new File(tempDir, RF2Constants.EQUIVALENCY_REPORT_TXT);

				ClassificationRunner classificationRunner = new ClassificationRunner(coreModuleSctid, effectiveTimeSnomedFormat,
						localConceptFilePaths, localStatedRelationshipFilePaths, previousInferredRelationshipFilePaths,
						inferredRelationshipsOutputFile.getAbsolutePath(), equivalencyReportOutputFile.getAbsolutePath());
				classificationRunner.execute();

				logger.info("Classification finished.");

				uploadLog(execution, packageBusinessKey, equivalencyReportOutputFile, RF2Constants.EQUIVALENCY_REPORT_TXT);

				// Upload inferred relationships file with null ids
				executionDAO.putTransformedFile(execution, pkg, inferredRelationshipsOutputFile);

				// Generate inferred relationship ids using transform
				transformationService.transformInferredRelationshipFile(execution, pkg, inferredRelationshipSnapshotFilename);

				return inferredRelationshipSnapshotFilename;
			} else {
				logger.info(RF2Constants.DATA_PROBLEM + "Cycles detected in stated relationship snapshot file. " +
						"See " + RF2Constants.CONCEPTS_WITH_CYCLES_TXT + " in execution package logs for more details.");
			}
		} else {
			logger.info("Stated relationship and concept files not present. Skipping classification.");
		}
		return null;
	}

	public boolean checkNoStatedRelationshipCycles(Execution execution, String packageBusinessKey, List<String> localConceptFilePaths,
			List<String> localStatedRelationshipFilePaths, File cycleFile) throws ProcessingException {
		try {
			logger.info("Performing stated relationship cycle check...");
			CycleCheck cycleCheck = new CycleCheck(localConceptFilePaths, localStatedRelationshipFilePaths, cycleFile.getAbsolutePath());
			boolean cycleDetected = cycleCheck.cycleDetected();
			if (cycleDetected) {
				// Upload cycles file
				uploadLog(execution, packageBusinessKey, cycleFile, RF2Constants.CONCEPTS_WITH_CYCLES_TXT);
			}
			return !cycleDetected;
		} catch (IOException | ClassificationException e) {
			String message = e.getMessage();
			throw new ProcessingException("Error during stated relationship cycle check: " +
					e.getClass().getSimpleName() + (message != null ? " - " + message : ""), e);
		}
	}

	public void uploadLog(Execution execution, String packageBusinessKey, File logFile, String targetFilename) throws ProcessingException {
		try (FileInputStream in = new FileInputStream(logFile);
			 AsyncPipedStreamBean logFileOutputStream = executionDAO.getLogFileOutputStream(execution, packageBusinessKey, targetFilename)) {
			OutputStream outputStream = logFileOutputStream.getOutputStream();
			StreamUtils.copy(in, outputStream);
			outputStream.close();
		} catch (IOException e) {
			throw new ProcessingException("Failed to upload file " + targetFilename + ".", e);
		}
	}

	private String getPreviousInferredRelationshipFilePath(Execution execution, Package pkg, ClassifierFilesPojo classifierFiles, File tempDir) throws IOException {
		String previousPublishedPackage = pkg.getPreviousPublishedPackage();
		String inferredRelationshipFilename = classifierFiles.getStatedRelationshipSnapshotFilenames().get(0);
		String previousInferredRelationshipFilename = inferredRelationshipFilename + ".previous_published";

		File localFile = new File(tempDir, previousInferredRelationshipFilename);
		try (InputStream publishedFileArchiveEntry = executionDAO.getPublishedFileArchiveEntry(execution.getBuild().getProduct(), inferredRelationshipFilename, previousPublishedPackage);
			 FileOutputStream out = new FileOutputStream(localFile)) {
			if (publishedFileArchiveEntry != null) {
				StreamUtils.copy(publishedFileArchiveEntry, out);
				return localFile.getAbsolutePath();
			}
		}

		return null;
	}

	private List<String> downloadFiles(Execution execution, String packageBusinessKey, File tempDir, List<String> filenameLists) throws ProcessingException {
		List<String> localFilePaths = new ArrayList<>();
		for (String downloadFilename : filenameLists) {

			File localFile = new File(tempDir, downloadFilename);
			try (InputStream inputFileStream = executionDAO.getOutputFileInputStream(execution, packageBusinessKey, downloadFilename);
				 FileOutputStream out = new FileOutputStream(localFile)) {
				if (inputFileStream != null) {
					StreamUtils.copy(inputFileStream, out);
					localFilePaths.add(localFile.getAbsolutePath());
				} else {
					throw new ProcessingException("Didn't find output file " + downloadFilename);
				}
			} catch (IOException e) {
				throw new ProcessingException("Failed to download snapshot file for classifier cycle check.", e);
			}
		}
		return localFilePaths;
	}

	private static class ClassifierFilesPojo {

		private List<String> conceptSnapshotFilenames;
		private List<String> statedRelationshipSnapshotFilenames;

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

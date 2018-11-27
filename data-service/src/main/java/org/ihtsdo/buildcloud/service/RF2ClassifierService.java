package org.ihtsdo.buildcloud.service;

import static org.ihtsdo.buildcloud.service.build.RF2Constants.BETA_RELEASE_PREFIX;
import static org.ihtsdo.buildcloud.service.build.RF2Constants.DELTA;
import static org.ihtsdo.buildcloud.service.build.RF2Constants.EQUIVALENCY_REPORT_TXT;
import static org.ihtsdo.buildcloud.service.build.RF2Constants.SNAPSHOT;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.ihtsdo.buildcloud.dao.BuildDAO;
import org.ihtsdo.buildcloud.dao.io.AsyncPipedStreamBean;
import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.BuildConfiguration;
import org.ihtsdo.buildcloud.service.build.RF2Constants.RelationshipFileType;
import org.ihtsdo.buildcloud.service.build.transform.TransformationService;
import org.ihtsdo.buildcloud.service.classifier.ClassificationResult;
import org.ihtsdo.buildcloud.service.classifier.ExternalRF2Classifier;
import org.ihtsdo.buildcloud.service.classifier.RF2Classifier;
import org.ihtsdo.buildcloud.service.classifier.RF2Classifier.ClassificationInputInfo;
import org.ihtsdo.buildcloud.service.helper.RelationshipHelper;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.otf.rest.exception.ProcessingException;
import org.ihtsdo.snomed.util.rf2.schema.ComponentType;
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
	private TransformationService transformationService;
	
	@Autowired
	private ExternalRF2Classifier externalRF2Classifier;

	private final Logger logger = LoggerFactory.getLogger(getClass());

	public enum RelationshipType {
		STATED, INFERRED
	};
	
	/**
	 * Checks for required files, performs cycle check then generates inferred relationships.
	 * @throws IOException 
	 */
	public ClassificationResult classify(final Build build, final Map<String, TableSchema> inputFileSchemaMap) throws BusinessServiceException {
		BuildConfiguration config = build.getConfiguration();
		RF2Classifier rf2Classifier = getRF2Classifier(config);
		ClassificationInputInfo classifierFiles = rf2Classifier.constructClassificationInputInfo(inputFileSchemaMap);
		if (!classifierFiles.isSufficientToClassify()) {
			throw new BusinessServiceException("Classification can't be run due to stated relationship and concept files are missing");
		}
		File tempDir = Files.createTempDir();
		File equivalencyReportOutputFile = new File(tempDir, EQUIVALENCY_REPORT_TXT);
		File classifierResultOutputFile = rf2Classifier.run(build, equivalencyReportOutputFile, classifierFiles, tempDir);
		if (classifierResultOutputFile == null) {
			throw new BusinessServiceException("No inferred relationship delta file found in the classification result.");
		}
		try {
			logger.info("Classification finished.");
			uploadLog(build, equivalencyReportOutputFile, EQUIVALENCY_REPORT_TXT);
			// Upload classification results into S3 
			buildDAO.putTransformedFile(build, classifierResultOutputFile);
			Map<String, String> conceptToModuleIdMap = null;
			Map<String, String> changedConceptToModuleIdMap = null;
			//only required for international release
			if (config.useExternalClassifier() && config.getExtensionConfig() == null) {
				String conceptSnapshot = classifierFiles.getConceptFileNames().get(0).replace(DELTA, SNAPSHOT);
				if (!conceptSnapshot.startsWith(BETA_RELEASE_PREFIX) && config.isBetaRelease()) {
					conceptSnapshot = BETA_RELEASE_PREFIX + conceptSnapshot;
				}
				conceptToModuleIdMap = RelationshipHelper.buildConceptToModuleIdMap(buildDAO.getOutputFileInputStream(build, conceptSnapshot));
				// find active concepts with module id changed since last release.
				InputStream previousSnapshotStream = getPreviousConceptSnapshotInputStream(build, conceptSnapshot);
				if (previousSnapshotStream != null) {
					changedConceptToModuleIdMap = RelationshipHelper.getConceptsWithModuleChange(previousSnapshotStream, conceptToModuleIdMap);
				}
			}
			
			Map<String,String> uuidToSctidMap = new HashMap<>();
			if (!classifierFiles.getLocalPreviousInferredRelationshipFilePaths().isEmpty()) {
				uuidToSctidMap = RelationshipHelper.buildUuidSctidMapFromPreviousRelationshipFile(
						classifierFiles.getLocalPreviousInferredRelationshipFilePaths().get(0), RelationshipFileType.INFERRED);
			}
			
			String inferredFilename = getInferredFilename(classifierFiles, build.getConfiguration());
			transformationService.transformInferredRelationshipFile(build, new FileInputStream(classifierResultOutputFile), 
					inferredFilename, uuidToSctidMap, conceptToModuleIdMap);
			boolean isSnapshot = config.useExternalClassifier() ? false : true;
			ClassificationResult classificationResult = new ClassificationResult(inferredFilename, isSnapshot);
			
			if (changedConceptToModuleIdMap != null && !changedConceptToModuleIdMap.isEmpty()) {
				InputStream inferredDeltaStream = buildDAO.getTransformedFileAsInputStream(build, inferredFilename);
				InputStream previousInferredSnapshotInput = getPreviousInferredSnapshotInputStream(build, inferredFilename);
				File extraDelta = RelationshipHelper.generateRelationshipDeltaDueToModuleIdChange(changedConceptToModuleIdMap, 
						inferredDeltaStream, previousInferredSnapshotInput, build.getConfiguration().getEffectiveTimeSnomedFormat());
				if (extraDelta != null) {
					classificationResult.setExtraResultFileName(extraDelta.getName());
					buildDAO.putTransformedFile(build, extraDelta);
				}
			}
			return classificationResult;

		} catch (BusinessServiceException | IOException e) {
			String msg = "Failed to generate inferred relationship snapshot due to: ";
			msg += e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
			throw new ProcessingException(msg, e);
		} finally {
			FileUtils.deleteQuietly(tempDir);
		}
	}
	
	private RF2Classifier getRF2Classifier(BuildConfiguration configuration) throws BusinessServiceException {
		if (!configuration.useExternalClassifier()) {
			throw new BusinessServiceException("The internal classifier has now been removed. Please use the external classifier.");
		}
		return externalRF2Classifier;
	}

	private InputStream getPreviousInferredSnapshotInputStream(Build build, String inferredDeltaFilename) throws IOException, BusinessServiceException {
		String inferredSnapshot = inferredDeltaFilename.replace(DELTA, SNAPSHOT);
		if (build.getConfiguration().isBetaRelease()) {
			inferredSnapshot = inferredSnapshot.replaceFirst(BETA_RELEASE_PREFIX, "");
		}
		InputStream previousSnapshotInput = buildDAO.getPublishedFileArchiveEntry(build.getProduct().getReleaseCenter(),
				inferredSnapshot, build.getConfiguration().getPreviousPublishedPackage());
		
		if (previousSnapshotInput == null) {
			throw new BusinessServiceException("No equivalent file found in the previous published release:" + inferredSnapshot);
		}
		return previousSnapshotInput;
	}

	private InputStream getPreviousConceptSnapshotInputStream(Build build, String currentSnapshot) throws BusinessServiceException, IOException {
		if (build.getConfiguration().isFirstTimeRelease()) {
			return null;
		}
		String conceptSnapshot = currentSnapshot;
		if (build.getConfiguration().isBetaRelease()) {
			conceptSnapshot = conceptSnapshot.replaceFirst(BETA_RELEASE_PREFIX, "");
		}
		InputStream previousSnapshotInput = buildDAO.getPublishedFileArchiveEntry(build.getProduct().getReleaseCenter(),
				conceptSnapshot, build.getConfiguration().getPreviousPublishedPackage());
		
		if (previousSnapshotInput == null) {
			throw new BusinessServiceException("No equivalent concept file found in the previous published release:" + conceptSnapshot);
		}
		return previousSnapshotInput;
	}


	private String getInferredFilename(ClassificationInputInfo classifierFiles, BuildConfiguration config) {
		String stated = classifierFiles.getStatedRelationshipFileNames().get(0);
		if (!stated.startsWith(BETA_RELEASE_PREFIX) && config.isBetaRelease()) {
			stated = BETA_RELEASE_PREFIX + stated;
		}
		String inferred = stated.replace(ComponentType.STATED_RELATIONSHIP.toString(), ComponentType.RELATIONSHIP.toString());
		inferred = config.useExternalClassifier() ? inferred.replace(SNAPSHOT, DELTA) : inferred;
		return inferred;
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
}

package org.ihtsdo.buildcloud.service;

import com.google.common.io.Files;
import org.apache.commons.io.FileUtils;
import org.ihtsdo.buildcloud.dao.BuildDAO;
import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.ExtensionConfig;
import org.ihtsdo.buildcloud.service.classifier.ClassificationServiceRestClient;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.otf.rest.exception.ProcessingException;
import org.ihtsdo.otf.utils.ZipFileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.ihtsdo.buildcloud.service.build.RF2Constants.*;

@Service
public class RF2ClassificationService {

	@Autowired
	private ClassificationServiceRestClient classificationRestClient;

	@Autowired
	private BuildDAO buildDAO;

	@Autowired
	private ProductInputFileService productInputFileService;

	private static final String OWL_REFSET_FILE_PATTERN = ".*_sRefset_OWL.*";

	private static final String MRCM_ATTRIBUTE_DOMAIN_DELTA = ".*_cissccRefset_MRCMAttributeDomainDelta";

	private static final String CONCEPT_PATTERN = ".*_Concept_.*Delta.*";

	// sct2_RelationshipConcreteValues_Delta or sct2_Relationship_Delta
	private static final String RELATIONSHIP_PATTERN = ".*_Relationship.*Delta.*";

	private static final String STATED_RELATIONSHIP_PATTERN = ".*_StatedRelationship_.*Delta.*";

	private static final String REFSET = "Refset_";

	public static final String EQUIVALENT_CONCEPT_REFSET = "der2_sRefset_EquivalentConceptSimpleMapDelta";

	private static final Logger LOGGER = LoggerFactory.getLogger(RF2ClassificationService.class);

	private static final String ERROR_MSG_FORMAT = "Inconsistencies found in %s file in classification results " +
			"- expected 0 records but found %d records from classification service." +
			" Please see detailed failures via the classificationResultsOutputFiles_url link listed.";

	public File classify(Build build, File rf2DeltaFile) throws BusinessServiceException {
		LOGGER.info("Run classification for product {} and build id {}", build.getProduct(), build.getId());
		if (!rf2DeltaFile.exists() || !rf2DeltaFile.canRead()) {
			throw new IllegalArgumentException("File doesn't exist " + rf2DeltaFile.getAbsolutePath());
		}
		String previousPublished = build.getConfiguration().getPreviousPublishedPackage();
		String dependencyRelease = null;
		ExtensionConfig extensionConfig = build.getConfiguration().getExtensionConfig();
		if (extensionConfig != null) {
			dependencyRelease = extensionConfig.getDependencyRelease();
			if (dependencyRelease == null || dependencyRelease.isEmpty()) {
				if (extensionConfig.isReleaseAsAnEdition()) {
					// This can happen when the previous edition release has got the latest published international release.
					LOGGER.info("The product is configured as an edition without dependency package." +
							" Only previous package {} will be used in classification", previousPublished);
				} else {
					throw new BusinessServiceException(String.format("International dependency release can't be null for extension release build %s", build.getProduct()));
				}
			}
		}
		File zipTempDir = Files.createTempDir();
		File rf2DeltaZipFile = new File(zipTempDir, "rf2Delta_" + build.getId() + ".zip");
		// Create a Delta package based on the input files and send to classification service
		try {
			ZipFileUtils.zip(rf2DeltaFile.getAbsolutePath(), rf2DeltaZipFile.getAbsolutePath());
			// Classify
			return classificationRestClient.classify(rf2DeltaZipFile, previousPublished, dependencyRelease);
		} catch (IOException e) {
			throw new BusinessServiceException("Failed to zip rf2 delta files for classification", e);
		} finally {
			FileUtils.deleteQuietly(zipTempDir);
		}
	}

	public File downloadInputDelta(final Build build) throws ProcessingException, IOException {
		LOGGER.info("Creating delta archive for classification");
		final File deltaTempDir = Files.createTempDir();
		for (String downloadFilename : buildDAO.listInputFileNames(build)) {
			if (!isRequiredFileForClassification(downloadFilename)) continue;
			LOGGER.info("Prepare {} for archiving", downloadFilename);
			String rename = (downloadFilename.contains(REFSET) && !downloadFilename.matches(OWL_REFSET_FILE_PATTERN)) ? downloadFilename.replace(INPUT_FILE_PREFIX, DER2) : downloadFilename.replace(INPUT_FILE_PREFIX, SCT2);
			LOGGER.info("Rename {} to {} for archiving", downloadFilename, rename);
			final File localFile = new File(deltaTempDir, rename);
			try (InputStream inputFileStream = productInputFileService.getFileInputStream(build.getProduct().getReleaseCenter().getBusinessKey()
					, build.getProduct().getBusinessKey(), downloadFilename);
			FileOutputStream out = new FileOutputStream(localFile)) {
				if (inputFileStream != null) {
					StreamUtils.copy(inputFileStream, out);
				} else {
					throw new ProcessingException("Didn't find input file:" + downloadFilename);
				}
			}
		}
		return deltaTempDir;
	}

	public File downloadOutputDelta(final Build build) throws ProcessingException, IOException {
		LOGGER.info("Creating delta archive for classification with output files");
		final File deltaTempDir = Files.createTempDir();
		for (String fileName : buildDAO.listOutputFilePaths(build)) {
			if (!isRequiredFileForClassification(fileName)) continue;
			LOGGER.info("Prepare {} for archiving", fileName);
			String rename = fileName.startsWith("x") ? fileName.substring(1) : fileName;
			LOGGER.info("Rename {} to {} for archiving", fileName, rename);
			final File localFile = new File(deltaTempDir, rename);
			try (InputStream inputFileStream = buildDAO.getOutputFileInputStream(build, fileName);
			FileOutputStream out = new FileOutputStream(localFile)) {
				if (inputFileStream != null) {
					StreamUtils.copy(inputFileStream, out);
				} else {
					throw new ProcessingException("Didn't find input file:" + fileName);
				}
			}
		}
		return deltaTempDir;
	}

	public String validateClassificationResults(File classificationResults, Build build) throws IOException, BusinessServiceException {
		// Raise the errors if the results returned from Classification Service are not empty
		StringBuilder errorMessageBuilder = new StringBuilder();
		File classifierResult = new File(Files.createTempDir(), "result");
		if (!classifierResult.exists()) {
			classifierResult.mkdir();
		}
		if (classifierResult.exists() && classifierResult.isDirectory()) {
			ZipFileUtils.extractFilesFromZipToOneFolder(classificationResults, classifierResult.getAbsolutePath());
		} else {
			throw new BusinessServiceException("Failed to create folder to extract classification results:" + classifierResult);
		}
		for (File file : classifierResult.listFiles()) {
			if (file.getName().endsWith(TXT_FILE_EXTENSION)) {
				if (file.getName().startsWith(RELASHIONSHIP_DELTA_PREFIX)) {
					List<String> results = FileUtils.readLines(file);
					if (results.size() > 1) {
						String errorMessage = String.format(ERROR_MSG_FORMAT, "relationship", (results.size() - 1));
						LOGGER.error(errorMessage);
						buildDAO.putClassificationResultOutputFile(build, file);
						errorMessageBuilder.append(errorMessage);
					}
				} else if(file.getName().startsWith(EQUIVALENT_CONCEPT_REFSET)) {
					List<String> results = FileUtils.readLines(file);
					if (results.size() > 1) {
						String errorMessage = String.format(ERROR_MSG_FORMAT, "equivalent concept refsets", (results.size() - 1));
						LOGGER.error(errorMessage);
						buildDAO.putClassificationResultOutputFile(build, file);
						errorMessageBuilder.append(errorMessage);
					}
				}

			}
		}
		return errorMessageBuilder.toString();
	}

	private boolean isRequiredFileForClassification(String filename) {
		return filename.matches(CONCEPT_PATTERN) || filename.matches(STATED_RELATIONSHIP_PATTERN) || filename.matches(RELATIONSHIP_PATTERN)
				|| filename.contains(MRCM_ATTRIBUTE_DOMAIN_DELTA) || filename.matches(OWL_REFSET_FILE_PATTERN);
	}

}

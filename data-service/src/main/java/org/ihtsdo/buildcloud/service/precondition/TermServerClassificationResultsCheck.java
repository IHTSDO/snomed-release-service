package org.ihtsdo.buildcloud.service.precondition;

import com.google.common.io.Files;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.ihtsdo.buildcloud.dao.BuildDAO;
import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.ExtensionConfig;
import org.ihtsdo.buildcloud.service.NetworkRequired;
import org.ihtsdo.buildcloud.service.ProductInputFileService;
import org.ihtsdo.buildcloud.service.classifier.ExternalRF2ClassifierRestClient;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.otf.rest.exception.ProcessingException;
import org.ihtsdo.otf.utils.ZipFileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StreamUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.ihtsdo.buildcloud.service.build.RF2Constants.BETA_RELEASE_PREFIX;
import static org.ihtsdo.buildcloud.service.build.RF2Constants.DER2;
import static org.ihtsdo.buildcloud.service.build.RF2Constants.EQUIVALENCY_REPORT_TXT;
import static org.ihtsdo.buildcloud.service.build.RF2Constants.INPUT_FILE_PREFIX;
import static org.ihtsdo.buildcloud.service.build.RF2Constants.RELASHIONSHIP_DELTA_PREFIX;
import static org.ihtsdo.buildcloud.service.build.RF2Constants.SCT2;
import static org.ihtsdo.buildcloud.service.build.RF2Constants.TXT_FILE_EXTENSION;

public class TermServerClassificationResultsCheck extends PreconditionCheck implements NetworkRequired {

	@Autowired
	private BuildDAO buildDAO;

	@Autowired
	private ProductInputFileService productInputFileService;

	@Autowired
	private ExternalRF2ClassifierRestClient classifierRestClient;

	private static final String OWL_REFSET_FILE_PATTERN = ".*_sRefset_OWL.*";

	private static final String REL2_MRCM_ATTRIBUTE_DOMAIN_DELTA = "rel2_cissccRefset_MRCMAttributeDomainDelta";

	private static final String REL2_CONCEPT_PATTERN = ".*rel2_Concept_.*Delta.*";

	private static final String REL2_RELATIONSHIP_PATTERN = ".*rel2_Relationship_.*Delta.*";

	private static final String REL2_STATED_RELATIONSHIP_PATTERN = ".*rel2_StatedRelationship_.*Delta.*";

	private static final String REFSET = "Refset_";

	private static final String EQUIVALENT_CONCEPT_REFSET = "der2_sRefset_EquivalentConceptSimpleMapDelta";

	private static final Logger LOGGER = LoggerFactory.getLogger(TermServerClassificationResultsCheck.class);


	@Override
	public void runCheck(Build build) {
		boolean isDerivativeProduct = buildDAO.isDerivativeProduct(build);
		LOGGER.info("Term Server Classification Results Check: isDerivativeProduct={}", isDerivativeProduct);
		if (isDerivativeProduct) {
			StringBuilder reasonBuilder = new StringBuilder("Skipped Term Server Classification Results Check. Reason: ");
			reasonBuilder.append("This product is a derivative product.");
			LOGGER.info(reasonBuilder.toString());
			notRun(reasonBuilder.toString());
			return;
		}
		LOGGER.info("Run classification validation check for input files of build {}", build.getId());
		try {
			String previousPublished = build.getConfiguration().getPreviousPublishedPackage();
			String dependencyRelease = null;
			ExtensionConfig extensionConfig = build.getConfiguration().getExtensionConfig();
			if (extensionConfig != null) {
				dependencyRelease = extensionConfig.getDependencyRelease();
				if (dependencyRelease == null || dependencyRelease.isEmpty()) {
					if (extensionConfig.isReleaseAsAnEdition()) {
						LOGGER.warn("The product is configured as an edition without dependency package. Only previous package {} will be used in classification", previousPublished);
					} else {
						throw new BusinessServiceException("International dependency release can't be null for extension release build.");
					}
				}
			}
			File deltaTempDir = Files.createTempDir();
			File zipTempDir = Files.createTempDir();
			File resultTempDir = Files.createTempDir();
			File rf2DeltaZipFile = new File(zipTempDir, "rf2Delta_" + build.getId() + ".zip");
			// Create a Delta package based on the input files and send to classification service
			createDeltaArchiveForClassification(build, deltaTempDir);
			ZipFileUtils.zip(deltaTempDir.getAbsolutePath(), rf2DeltaZipFile.getAbsolutePath());
			// Classify and validate the returned results
			File classificationResults = classifierRestClient.classify(rf2DeltaZipFile, previousPublished, dependencyRelease);
			String errorMessage = validateClassificationResults(classificationResults, resultTempDir, build);
			if (StringUtils.isNotBlank(errorMessage)) {
				fatalError(errorMessage);
				LOGGER.error("Classification validation check has failed");
			} else {
				pass();
				LOGGER.info("Classification validation check has passed");
			}
			LOGGER.info("Complete classification validation check for input files of build {}", build.getId());

		} catch (Exception e) {
			LOGGER.error("Error occurred during classification results validation due to: ", e);
			fatalError("Error occurred during classification results validation due to: " + e.getMessage());
		}
	}

	private String validateClassificationResults(File classificationResults, File resultTemp, Build build) throws IOException, BusinessServiceException {
		// Raise the errors if the results returned from Classification Service are not empty
		StringBuilder errorMessageBuilder = new StringBuilder();
		File classifierResult = new File(resultTemp, "result");
		if (!classifierResult.exists()) {
			classifierResult.mkdir();
		}
		if (classifierResult.exists() && classifierResult.isDirectory()) {
			ZipFileUtils.extractFilesFromZipToOneFolder(classificationResults, classifierResult.getAbsolutePath().toString());
		} else {
			throw new BusinessServiceException("Failed to create folder to extract classification results:" + classifierResult);
		}
		for (File file : classifierResult.listFiles()) {
			if (file.getName().endsWith(TXT_FILE_EXTENSION)) {
				if(file.getName().startsWith(RELASHIONSHIP_DELTA_PREFIX)) {
					List<String> results = FileUtils.readLines(file);
					if (results.size() > 1) {
						String errorMessage = "Inconsistencies found in relationship file in classification results - expected 0 records but found " + (results.size() - 1) + " records from classification service. ";
						LOGGER.error(errorMessage);
						buildDAO.putClassificationResultOutputFile(build, file);
						errorMessageBuilder.append(errorMessage);
					}
				} else if(file.getName().startsWith(EQUIVALENT_CONCEPT_REFSET)) {
					List<String> results = FileUtils.readLines(file);
					if (results.size() > 1) {
						String errorMessage = "Inconsistencies found in equivalent concept refsets file in classification results - expected 0 records but found " + (results.size() - 1) + " records from classification service. ";
						LOGGER.error(errorMessage);
						buildDAO.putClassificationResultOutputFile(build, file);
						errorMessageBuilder.append(errorMessage);
					}
				}

			}
		}
		return errorMessageBuilder.toString();
	}

	private List<String> createDeltaArchiveForClassification(final Build build, final File deltaTempDir) throws ProcessingException, IOException {
		LOGGER.info("Creating delta archive for classification");
		final List<String> localFilePaths = new ArrayList<>();
		for (String downloadFilename : buildDAO.listInputFileNames(build)) {
			if (!isRequiredFileForClassification(downloadFilename)) continue;
			LOGGER.info("Prepare {} for archiving", downloadFilename);
			String rename = (downloadFilename.contains(REFSET) && !downloadFilename.matches(OWL_REFSET_FILE_PATTERN)) ? downloadFilename.replace(INPUT_FILE_PREFIX, DER2) : downloadFilename.replace(INPUT_FILE_PREFIX, SCT2);
			LOGGER.info("Rename {} to {} for archiving", downloadFilename, rename);
			final File localFile = new File(deltaTempDir, rename);
			InputStream inputFileStream = productInputFileService.getFileInputStream(build.getProduct().getReleaseCenter().getBusinessKey()
					, build.getProduct().getBusinessKey(), downloadFilename);
			FileOutputStream out = new FileOutputStream(localFile);
			if (inputFileStream != null) {
				try {
					StreamUtils.copy(inputFileStream, out);
					localFilePaths.add(localFile.getAbsolutePath());
				} finally {
					inputFileStream.close();
					out.close();
				}
			} else {
				throw new ProcessingException("Didn't find input file:" + downloadFilename);
			}
		}
		return localFilePaths;
	}

	private boolean isRequiredFileForClassification(String filename) {
		return filename.matches(REL2_CONCEPT_PATTERN) || filename.matches(REL2_STATED_RELATIONSHIP_PATTERN) || filename.matches(REL2_RELATIONSHIP_PATTERN)
				|| filename.contains(REL2_MRCM_ATTRIBUTE_DOMAIN_DELTA) || filename.matches(OWL_REFSET_FILE_PATTERN);
	}

}

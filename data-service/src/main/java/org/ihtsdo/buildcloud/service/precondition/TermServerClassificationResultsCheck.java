package org.ihtsdo.buildcloud.service.precondition;

import com.google.common.io.Files;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.ihtsdo.buildcloud.dao.BuildDAO;
import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.service.NetworkRequired;
import org.ihtsdo.buildcloud.service.RF2ClassificationService;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.otf.utils.ZipFileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.ihtsdo.buildcloud.service.RF2ClassificationService.EQUIVALENT_CONCEPT_REFSET;
import static org.ihtsdo.buildcloud.service.build.RF2Constants.RELASHIONSHIP_DELTA_PREFIX;
import static org.ihtsdo.buildcloud.service.build.RF2Constants.TXT_FILE_EXTENSION;

public class TermServerClassificationResultsCheck extends PreconditionCheck implements NetworkRequired {

	@Autowired
	private RF2ClassificationService rf2ClassificationService;

	@Autowired
	private BuildDAO buildDAO;

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
			// Classify and validate the returned results
			File classificationResults = rf2ClassificationService.classify(build);
			File resultTempDir = Files.createTempDir();
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
}

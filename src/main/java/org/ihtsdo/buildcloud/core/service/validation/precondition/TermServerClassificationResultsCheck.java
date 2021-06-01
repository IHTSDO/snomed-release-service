package org.ihtsdo.buildcloud.core.service.validation.precondition;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.ihtsdo.buildcloud.core.dao.BuildDAO;
import org.ihtsdo.buildcloud.core.entity.Build;
import org.ihtsdo.buildcloud.core.service.NetworkRequired;
import org.ihtsdo.buildcloud.core.service.RF2ClassificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
public class TermServerClassificationResultsCheck extends PreconditionCheck implements NetworkRequired {

	private static final Logger LOGGER = LoggerFactory.getLogger(TermServerClassificationResultsCheck.class);

	public static final String SKIP_MESSAGE = "Skipped TermServer Classification Results Check. Reason: This product is a derivative product";

	@Autowired
	private RF2ClassificationService rf2ClassificationService;

	@Autowired
	private BuildDAO buildDAO;

	@Override
	public void runCheck(Build build) {
		boolean isDerivativeProduct = buildDAO.isDerivativeProduct(build);
		LOGGER.info("TermServer Classification Results Check: isDerivativeProduct={}", isDerivativeProduct);
		if (isDerivativeProduct) {
			LOGGER.info(SKIP_MESSAGE);
			notRun(SKIP_MESSAGE);
			return;
		}
		LOGGER.info("Run classification validation check for input files of build {}", build.getId());
		File results = null;
		File deltaFile = null;
		try {
			// Classify and validate the returned results
			deltaFile = rf2ClassificationService.downloadInputDelta(build);
			results = rf2ClassificationService.classify(build, deltaFile);
			String errorMessage = rf2ClassificationService.validateClassificationResults(results, build);
			if (StringUtils.isNotBlank(errorMessage)) {
				fatalError(errorMessage);
				LOGGER.error("Classification validation check has failed");
			} else {
				pass();
				LOGGER.info("Classification validation check has passed");
			}
			LOGGER.info("Complete classification validation check for input files of build {}", build.getId());
		} catch (Exception e) {
			String errorMsg = "Error occurred during classification results validation due to: ";
			LOGGER.error(errorMsg, e);
			fatalError(errorMsg + e.getMessage());
		} finally {
			FileUtils.deleteQuietly(results);
			FileUtils.deleteQuietly(deltaFile);
		}
	}
}

package org.ihtsdo.buildcloud.service.postcondition;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.ihtsdo.buildcloud.dao.BuildDAO;
import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.service.NetworkRequired;
import org.ihtsdo.buildcloud.service.RF2ClassificationService;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;

@Service
public class TermServerClassificationResultsOutputCheck extends PostconditionCheck implements NetworkRequired {

	private static final Logger LOGGER = LoggerFactory.getLogger(TermServerClassificationResultsOutputCheck.class);

	public static final String SKIP_MESSAGE = "Skipped TermServer Classification Results Check. Reason: This product is a derivative product";

	@Autowired
	private BuildDAO buildDAO;

	@Autowired
	private RF2ClassificationService rf2ClassificationService;

	@Override
	public void runCheck(Build build) {
		boolean isDerivativeProduct = buildDAO.isDerivativeProduct(build);
		LOGGER.info("Term Server Classification Results Check: isDerivativeProduct={}", isDerivativeProduct);
		if (isDerivativeProduct) {
			LOGGER.info(SKIP_MESSAGE);
			notRun(SKIP_MESSAGE);
			return;

		}
		LOGGER.info("Run classification validation check for input files of build {}", build.getId());
		File outputDelta = null;
		File results = null;
		try {
			outputDelta = rf2ClassificationService.downloadOutputDelta(build);
			results = rf2ClassificationService.classify(build, outputDelta);
			String errorMessage = rf2ClassificationService.validateClassificationResults(results, build);
			if (StringUtils.isNotBlank(errorMessage)) {
				LOGGER.error("Classification validation check has failed");
				fail(errorMessage);
			} else {
				pass();
				LOGGER.info("Classification validation check has passed");
			}
			LOGGER.info("Complete classification validation check for output files of build {}", build.getId());
		} catch (IOException | BusinessServiceException e) {
			String errorMsg = "Error occurred when running classification";
			LOGGER.error(errorMsg, e);
			fail(errorMsg + e.getMessage());
		} finally {
			FileUtils.deleteQuietly(outputDelta);
			FileUtils.deleteQuietly(results);
		}
	}

	@Override
	public String getTestName() {
		return "ClassificationResultForOutputCheck";
	}

}

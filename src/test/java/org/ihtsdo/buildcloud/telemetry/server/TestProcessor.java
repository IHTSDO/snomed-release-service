package org.ihtsdo.buildcloud.telemetry.server;

import org.ihtsdo.buildcloud.telemetry.TestService;
import org.ihtsdo.buildcloud.telemetry.client.TelemetryStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class TestProcessor {

	public static void doProcessing(String streamDestination) {
		Logger logger = LoggerFactory.getLogger(TestService.class);

		logger.info("Before stream started");

		TelemetryStream.start(logger, streamDestination);
		logger.info("Start of event stream");

		logger.info("Processing...");

		logger.info("End of event stream");
		TelemetryStream.finish(logger);

		logger.info("After stream ended");
	}

	public static void doProcessingWithException(String streamDestination) {
		Logger logger = LoggerFactory.getLogger(TestService.class);

		logger.info("Before stream started");

		TelemetryStream.start(logger, streamDestination);
		logger.info("Start of event stream");

		logger.info("Processing...");
		String userInput = "a";
		try {
			Float.parseFloat(userInput);
		} catch (NumberFormatException e) {
			logger.error("User input is not a valid float: {}", userInput, e);
		}

		logger.info("End of event stream");
		TelemetryStream.finish(logger);

		logger.info("After stream ended");
	}

}

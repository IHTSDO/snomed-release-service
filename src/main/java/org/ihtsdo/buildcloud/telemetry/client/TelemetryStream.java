package org.ihtsdo.buildcloud.telemetry.client;

import org.apache.log4j.MDC;
import org.ihtsdo.buildcloud.telemetry.core.Constants;
import org.slf4j.Logger;

public class TelemetryStream {

	public static void start(Logger logger, String streamDestinationUri) {
		MDC.put(Constants.START_STREAM, streamDestinationUri);
		logger.info(Constants.START_STREAM);
	}

	public static void finish(Logger logger) {
		MDC.put(Constants.FINISH_STREAM, "");
		logger.info(Constants.FINISH_STREAM);
	}
}

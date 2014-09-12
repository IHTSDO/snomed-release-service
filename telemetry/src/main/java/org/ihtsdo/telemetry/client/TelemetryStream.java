package org.ihtsdo.telemetry.client;

import org.apache.log4j.MDC;
import org.ihtsdo.telemetry.core.Constants;
import org.slf4j.Logger;

public class TelemetryStream {

	public static void start(Logger logger, String uri) {
		MDC.put(Constants.START_STREAM, uri);
		logger.info(Constants.START_STREAM);
	}

	public static void finish(Logger logger) {
		MDC.put(Constants.FINISH_STREAM, "");
		logger.info(Constants.FINISH_STREAM);
	}
}

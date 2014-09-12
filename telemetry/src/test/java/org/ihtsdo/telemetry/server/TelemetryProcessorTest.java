package org.ihtsdo.telemetry.server;

import org.apache.log4j.helpers.LogLog;
import org.ihtsdo.telemetry.TestService;
import org.ihtsdo.telemetry.client.TelemetryStream;
import org.ihtsdo.telemetry.core.Constants;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.FileCopyUtils;

import java.io.FileReader;
import java.io.IOException;

public class TelemetryProcessorTest {

	private static final String FILE_TMP_STREAM_TXT = "file:///tmp/test_telemetry_stream.txt";

	private TestBroker testBroker;
	private TelemetryProcessor telemetryProcessor;

	@Before
	public void setUp() throws Exception {
		LogLog.setInternalDebugging(true);
		LogLog.setQuietMode(false);
		testBroker = new TestBroker();

		// Set system property to override log4j appender default broker url
		System.setProperty(Constants.SYS_PROP_BROKER_URL, "vm://localhost?create=false");
		telemetryProcessor = new TelemetryProcessor();
	}

	@Test
	public void testAggregateEvents() throws IOException, InterruptedException {

		doProcessing();
		// Wait for the aggregator to finish.
		Thread.sleep(1000);

		String capturedEventStream = FileCopyUtils.copyToString(new FileReader("/tmp/test_telemetry_stream.txt"));
		Assert.assertNotNull(capturedEventStream);
		Assert.assertEquals("Start of event stream\n" +
				"Processing...\n" +
				"End of event stream\n", capturedEventStream);
	}

	public void doProcessing() {
		Logger logger = LoggerFactory.getLogger(TestService.class);

		logger.info("Before stream started");

		TelemetryStream.start(logger, FILE_TMP_STREAM_TXT);
		logger.info("Start of event stream");

		logger.info("Processing...");

		logger.info("End of event stream");
		TelemetryStream.finish(logger);

		logger.info("After stream ended");
	}


	@After
	public void tearDown() throws Exception {
		testBroker.close();
	}
}

package org.ihtsdo.telemetry.server;

import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.Upload;
import com.amazonaws.services.s3.transfer.model.UploadResult;
import org.apache.log4j.helpers.LogLog;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.easymock.MockType;
import org.easymock.internal.MocksControl;
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

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class TelemetryProcessorTest {

	private static final String STREAM_FILE_DESTINATION = "file:///tmp/test_telemetry_stream.txt";
	private static final String STREAM_S3_DESTINATION = "s3://local.execution.bucket/test_telemetry_stream.txt";

	private TestBroker testBroker;
	private TelemetryProcessor telemetryProcessor;
	private MocksControl mocksControl;
	private StreamFactory streamFactory;
	private TransferManager mockTransferManager;
	private Upload mockUpload;

	@Before
	public void setUp() throws Exception {
		LogLog.setInternalDebugging(true);
		LogLog.setQuietMode(false);
		testBroker = new TestBroker();

		mocksControl = new MocksControl(MockType.DEFAULT);
		mockTransferManager = mocksControl.createMock(TransferManager.class);
		mockUpload = mocksControl.createMock(Upload.class);

		// Set system property to override log4j appender default broker url
		System.setProperty(Constants.SYS_PROP_BROKER_URL, "vm://localhost?create=false");

		streamFactory = new StreamFactory(mockTransferManager);
		telemetryProcessor = new TelemetryProcessor(streamFactory);
	}

	@Test
	public void testAggregateEventsToFile() throws IOException, InterruptedException {
		doProcessing(STREAM_FILE_DESTINATION);
		// Wait for the aggregator to finish.
		Thread.sleep(1000);

		String capturedEventStream = fileToString(new File("/tmp/test_telemetry_stream.txt"));
		Assert.assertNotNull(capturedEventStream);
		Assert.assertEquals("Line count", 3, capturedEventStream.split("\n").length);
		Assert.assertTrue(capturedEventStream.matches("[^ ]+ INFO  org.ihtsdo.telemetry.server.TelemetryProcessorTest.doProcessing - Start of event stream\n" +
				"[^ ]+ INFO  org.ihtsdo.telemetry.server.TelemetryProcessorTest.doProcessing - Processing...\n" +
				"[^ ]+ INFO  org.ihtsdo.telemetry.server.TelemetryProcessorTest.doProcessing - End of event stream\n"));
	}

	@Test
	public void testAggregateEventsToS3() throws IOException, InterruptedException {
		// Set up mock expectations
		final Capture<File> fileCapture = new Capture<>();
		final BooleanHolder fileAssertionsRan = new BooleanHolder();
		EasyMock.expect(mockTransferManager.upload(EasyMock.eq("local.execution.bucket"), EasyMock.eq("test_telemetry_stream.txt"), EasyMock.capture(fileCapture))).andReturn(mockUpload);
		EasyMock.expect(mockUpload.waitForUploadResult()).andAnswer(new IAnswer<UploadResult>() {
			@Override
			public UploadResult answer() throws Throwable {
				// Run temp file assertions before it's deleted
				File capturedFile = fileCapture.getValue();
				Assert.assertNotNull(capturedFile);
				String capturedEventStream = fileToString(capturedFile);
				Assert.assertNotNull(capturedEventStream);
				Assert.assertEquals("Line count", 3, capturedEventStream.split("\n").length);
				Assert.assertTrue(capturedEventStream.matches("[^ ]+ INFO  org.ihtsdo.telemetry.server.TelemetryProcessorTest.doProcessing - Start of event stream\n" +
						"[^ ]+ INFO  org.ihtsdo.telemetry.server.TelemetryProcessorTest.doProcessing - Processing...\n" +
						"[^ ]+ INFO  org.ihtsdo.telemetry.server.TelemetryProcessorTest.doProcessing - End of event stream\n"));
				fileAssertionsRan.b = true;
				return null;
			}
		});
		mocksControl.replay();

		// Perform test scenario
		doProcessing(STREAM_S3_DESTINATION);
		// Wait for the aggregator to finish.
		Thread.sleep(1000);

		// Assert mock expectations
		mocksControl.verify();
		Assert.assertTrue(fileAssertionsRan.b);
	}

	public void doProcessing(String streamDestination) {
		Logger logger = LoggerFactory.getLogger(TestService.class);

		logger.info("Before stream started");

		TelemetryStream.start(logger, streamDestination);
		logger.info("Start of event stream");

		logger.info("Processing...");

		logger.info("End of event stream");
		TelemetryStream.finish(logger);

		logger.info("After stream ended");
	}

	private String fileToString(File file) throws IOException {
		return FileCopyUtils.copyToString(new FileReader(file));
	}

	@After
	public void tearDown() throws Exception {
		telemetryProcessor.shutdown();
		testBroker.close();
	}

	private static final class BooleanHolder {
		boolean b = false;
	}
}

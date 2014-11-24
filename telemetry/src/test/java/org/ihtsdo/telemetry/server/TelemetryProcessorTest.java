package org.ihtsdo.telemetry.server;

import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.Upload;
import com.amazonaws.services.s3.transfer.model.UploadResult;

import mockit.Expectations;
import mockit.Mocked;
import mockit.NonStrictExpectations;
import mockit.integration.junit4.JMockit;

import org.apache.log4j.helpers.LogLog;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.easymock.MockType;
import org.easymock.internal.MocksControl;
import org.ihtsdo.commons.email.EmailRequest;
import org.ihtsdo.commons.email.EmailSender;
import org.ihtsdo.telemetry.TestService;
import org.ihtsdo.telemetry.client.TelemetryStream;
import org.ihtsdo.telemetry.core.Constants;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.FileCopyUtils;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.UUID;

@RunWith(JMockit.class)
public class TelemetryProcessorTest {

	private TestBroker testBroker;
	private TelemetryProcessor telemetryProcessor;
	private MocksControl mocksControl;
	private StreamFactory streamFactory;
	private TransferManager mockTransferManager;
	private Upload mockUpload;
	private File testStreamFile;

	private static String streamFileName;
	private static String streamFileDestination;
	private static String streamS3Destination;

	@Mocked
	EmailSender emailSender;

	@Before
	public void setUp() throws Exception {
		LogLog.setInternalDebugging(true);
		LogLog.setQuietMode(false);

		UUID uniqueSuffix = UUID.randomUUID();
		streamFileName = "test_telemetry_stream_" + uniqueSuffix + ".txt";
		streamFileDestination = "file:///tmp/" + streamFileName;
		streamS3Destination = "s3://local.build.bucket/test_telemetry_stream_" + uniqueSuffix + ".txt";

		testBroker = new TestBroker();

		mocksControl = new MocksControl(MockType.DEFAULT);
		mockTransferManager = mocksControl.createMock(TransferManager.class);
		mockUpload = mocksControl.createMock(Upload.class);

		// Set system property to override log4j appender default broker url
		System.setProperty(Constants.SYS_PROP_BROKER_URL, "vm://localhost?create=false");

		streamFactory = new StreamFactory(mockTransferManager, false);

		new NonStrictExpectations() {
			{
				emailSender.send((EmailRequest) any);
			}
		};

		telemetryProcessor = new TelemetryProcessor(streamFactory, "foo@bar.com", null, emailSender);
		telemetryProcessor.startup();
		testStreamFile = new File("/tmp/" + streamFileName);
		testStreamFile.delete();
	}

	@Test
	public void testErrorDetection() throws IOException, InterruptedException {

		// Make sure previous test has had time to finish
		// Thread.sleep(1000);

		Logger logger = LoggerFactory.getLogger(TestService.class);

		try {
			throw new Exception("Simulating thrown Exception");
		} catch (Exception e) {
			logger.error("Correctly detected thrown exception.", e);
		}
		// Thread.sleep(100000); // Needed if you're going to breakpoint in the server code.
	}

	@Test
	public void testAggregateEventsToFile() throws IOException, InterruptedException {
		doProcessing(TelemetryProcessorTest.streamFileDestination);
		// Wait for the aggregator to finish.
		Thread.sleep(1000);

		String capturedEventStream = fileToString(testStreamFile);
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
		EasyMock.expect(mockTransferManager.upload(EasyMock.eq("local.build.bucket"), EasyMock.eq(streamFileName), EasyMock.capture(fileCapture))).andReturn(mockUpload);
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
		doProcessing(TelemetryProcessorTest.streamS3Destination);
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
		testStreamFile.delete();
		telemetryProcessor.shutdown();
		testBroker.close();
	}

	private static final class BooleanHolder {
		boolean b = false;
	}
}

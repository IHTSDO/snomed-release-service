package org.ihtsdo.telemetry.server;

import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.Upload;
import com.amazonaws.services.s3.transfer.model.UploadResult;
import mockit.Mocked;
import mockit.NonStrictExpectations;
import mockit.integration.junit4.JMockit;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.helpers.LogLog;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.easymock.MockType;
import org.easymock.internal.MocksControl;
import org.ihtsdo.commons.email.EmailRequest;
import org.ihtsdo.commons.email.EmailSender;
import org.ihtsdo.telemetry.TestService;
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
import java.util.*;

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
		TestProcessor.doProcessing(TelemetryProcessorTest.streamFileDestination);
		// Wait for the aggregator to finish.
		Thread.sleep(1000);

		String capturedEventStream = replaceDates(fileToString(testStreamFile));
		Assert.assertNotNull(capturedEventStream);
		Assert.assertEquals("Line count", 3, capturedEventStream.split("\n").length);
		Assert.assertEquals("DATE INFO  org.ihtsdo.telemetry.server.TestProcessor.doProcessing - Start of event stream\n" +
				"DATE INFO  org.ihtsdo.telemetry.server.TestProcessor.doProcessing - Processing...\n" +
				"DATE INFO  org.ihtsdo.telemetry.server.TestProcessor.doProcessing - End of event stream\n",
				capturedEventStream);
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
				String capturedEventStream = replaceDates(fileToString(capturedFile));
				Assert.assertNotNull(capturedEventStream);
				Assert.assertEquals("Line count", 3, capturedEventStream.split("\n").length);
				Assert.assertEquals("DATE INFO  org.ihtsdo.telemetry.server.TestProcessor.doProcessing - Start of event stream\n" +
						"DATE INFO  org.ihtsdo.telemetry.server.TestProcessor.doProcessing - Processing...\n" +
						"DATE INFO  org.ihtsdo.telemetry.server.TestProcessor.doProcessing - End of event stream\n",
						capturedEventStream);
				fileAssertionsRan.b = true;
				return null;
			}
		});
		mocksControl.replay();

		// Perform test scenario
		TestProcessor.doProcessing(TelemetryProcessorTest.streamS3Destination);
		// Wait for the aggregator to finish.
		Thread.sleep(1000);

		// Assert mock expectations
		mocksControl.verify();
		Assert.assertTrue(fileAssertionsRan.b);
	}

	@Test
	public void testAggregateEventsToFileWithException() throws IOException, InterruptedException {
		TestProcessor.doProcessingWithException(TelemetryProcessorTest.streamFileDestination);
		// Wait for the aggregator to finish.
		Thread.sleep(1000);

		String capturedEventStream = replaceDates(fileToString(testStreamFile));

		// Grab first 8 lines. The lower part of the stack includes the container (Maven, IDE etc.) so should not be part of unit test.
		String capturedEventStreamFirstEightLines = StringUtils.join(Arrays.copyOfRange(capturedEventStream.split("\n"), 0, 8), "\n");

		String expected = "DATE INFO  org.ihtsdo.telemetry.server.TestProcessor.doProcessingWithException - Start of event stream\n" +
				"DATE INFO  org.ihtsdo.telemetry.server.TestProcessor.doProcessingWithException - Processing...\n" +
				"DATE ERROR org.ihtsdo.telemetry.server.TestProcessor.doProcessingWithException - User input is not a valid float: a\n" +
				"java.lang.NumberFormatException: For input string: \"a\"\n" +
				"\tat sun.misc.FloatingDecimal.readJavaFormatString(FloatingDecimal.java:1241)\n" +
				"\tat java.lang.Float.parseFloat(Float.java:452)\n" +
				"\tat org.ihtsdo.telemetry.server.TestProcessor.doProcessingWithException(TestProcessor.java:37)\n" +
				"\tat org.ihtsdo.telemetry.server.TelemetryProcessorTest.testAggregateEventsToFileWithException(TelemetryProcessorTest.java:153)";

		Assert.assertEquals(expected, capturedEventStreamFirstEightLines);
	}

	private String replaceDates(String capturedEventStream) {
		return capturedEventStream.replaceAll("[\\d]{8}[^ ]* ", "DATE ");
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

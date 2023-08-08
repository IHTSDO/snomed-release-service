package org.ihtsdo.buildcloud.telemetry.server;

import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.Upload;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.helpers.LogLog;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.MockType;
import org.easymock.internal.MocksControl;
import org.ihtsdo.buildcloud.telemetry.TestService;
import org.ihtsdo.buildcloud.telemetry.core.Constants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.FileCopyUtils;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class TelemetryProcessorTest {

	private TestBroker testBroker;
	private TelemetryProcessor telemetryProcessor;
	private MocksControl mocksControl;
	private TransferManager mockTransferManager;
	private Upload mockUpload;
	private File testStreamFile;

	private static String streamFileName;
	private static String streamFileDestination;
	private static String streamS3Destination;

	@BeforeEach
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

		final ResourceLoader resourceLoader = mocksControl.createMock(ResourceLoader.class);

		telemetryProcessor = new TelemetryProcessor(testBroker.getSession(), resourceLoader, true);
		telemetryProcessor.startup();
		testStreamFile = new File("/tmp/" + streamFileName);
		testStreamFile.delete();
	}

	@Disabled
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
	@Disabled
	public void testAggregateEventsToFile() throws IOException, InterruptedException {
		TestProcessor.doProcessing(TelemetryProcessorTest.streamFileDestination);
		// Wait for the aggregator to finish.
		Thread.sleep(1000);

		String capturedEventStream = replaceDates(fileToString(testStreamFile));
		assertNotNull(capturedEventStream);
		assertEquals(3, capturedEventStream.split("\n").length, "Line count");
		assertEquals("""
                        DATE INFO  org.ihtsdo.buildcloud.telemetry.server.TestProcessor.doProcessing - Start of event stream
                        DATE INFO  org.ihtsdo.buildcloud.telemetry.server.TestProcessor.doProcessing - Processing...
                        DATE INFO  org.ihtsdo.buildcloud.telemetry.server.TestProcessor.doProcessing - End of event stream
                        """,
				capturedEventStream);
	}

	@Test
	@Disabled
	public void testAggregateEventsToS3() throws IOException, InterruptedException {
		// Set up mock expectations
		final Capture<File> fileCapture = Capture.newInstance();
		final BooleanHolder fileAssertionsRan = new BooleanHolder();
		EasyMock.expect(mockTransferManager.upload(EasyMock.eq("local.build.bucket"), EasyMock.eq(streamFileName), EasyMock.capture(fileCapture))).andReturn(mockUpload);
		EasyMock.expect(mockUpload.waitForUploadResult()).andAnswer(() -> {
            // Run temp file assertions before it's deleted
            File capturedFile = fileCapture.getValue();
            assertNotNull(capturedFile);
            String capturedEventStream = replaceDates(fileToString(capturedFile));
            assertNotNull(capturedEventStream);
            assertEquals(3, capturedEventStream.split("\n").length, "Line count");
            assertEquals("""
                            DATE INFO  org.ihtsdo.telemetry.server.TestProcessor.doProcessing - Start of event stream
                            DATE INFO  org.ihtsdo.telemetry.server.TestProcessor.doProcessing - Processing...
                            DATE INFO  org.ihtsdo.telemetry.server.TestProcessor.doProcessing - End of event stream
                            """,
                    capturedEventStream);
            fileAssertionsRan.b = true;
            return null;
        });
		mocksControl.replay();

		// Perform test scenario
		TestProcessor.doProcessing(TelemetryProcessorTest.streamS3Destination);
		// Wait for the aggregator to finish.
		Thread.sleep(1000);

		// Assert mock expectations
		mocksControl.verify();
		assertTrue(fileAssertionsRan.b);
	}

	@Disabled
	@Test
	public void testAggregateEventsToFileWithException() throws IOException, InterruptedException {
		TestProcessor.doProcessingWithException(TelemetryProcessorTest.streamFileDestination);
		// Wait for the aggregator to finish.
		Thread.sleep(1000);

		String capturedEventStream = stripLineNumbersFromStackTrace(replaceDates(fileToString(testStreamFile)));

		// Grab first 8 lines. The lower part of the stack includes the container (Maven, IDE etc.) so should not be part of unit test.
		String capturedEventStreamFirstEightLines = StringUtils.join(Arrays.copyOfRange(capturedEventStream.split("\n"), 0, 8), "\n");

		String expected = """
                DATE INFO  org.ihtsdo.telemetry.server.TestProcessor.doProcessingWithException - Start of event stream
                DATE INFO  org.ihtsdo.telemetry.server.TestProcessor.doProcessingWithException - Processing...
                DATE ERROR org.ihtsdo.telemetry.server.TestProcessor.doProcessingWithException - User input is not a valid float: a
                java.lang.NumberFormatException: For input string: "a"
                \tat sun.misc.FloatingDecimal.readJavaFormatString(FloatingDecimal.java:LINE)
                \tat java.lang.Float.parseFloat(Float.java:LINE)
                \tat org.ihtsdo.telemetry.server.TestProcessor.doProcessingWithException(TestProcessor.java:LINE)
                \tat org.ihtsdo.telemetry.server.TelemetryProcessorTest.testAggregateEventsToFileWithException(TelemetryProcessorTest.java:LINE)""";

		assertEquals(expected, capturedEventStreamFirstEightLines);
	}

	private String stripLineNumbersFromStackTrace(String s) {
		return s.replaceAll(":[\\d]+", ":LINE");
	}

	private String replaceDates(String capturedEventStream) {
		return capturedEventStream.replaceAll("[\\d]{8}[^ ]* ", "DATE ");
	}

	private String fileToString(File file) throws IOException {
		return FileCopyUtils.copyToString(new FileReader(file));
	}

	@AfterEach
	public void tearDown() throws Exception {
		testStreamFile.delete();
		telemetryProcessor.shutdown();
		testBroker.close();
	}

	private static final class BooleanHolder {
		boolean b = false;
	}
}

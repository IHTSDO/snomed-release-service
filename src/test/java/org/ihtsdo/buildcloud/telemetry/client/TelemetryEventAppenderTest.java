package org.ihtsdo.buildcloud.telemetry.client;

import org.ihtsdo.buildcloud.telemetry.TestService;
import org.ihtsdo.buildcloud.telemetry.core.Constants;
import org.ihtsdo.buildcloud.telemetry.server.TestBroker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.TextMessage;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TelemetryEventAppenderTest {

	private TestBroker testBroker;

	@BeforeEach
	public void setup() throws JMSException {
		testBroker = new TestBroker();

		// Set system property to override log4j appender default broker url
		System.setProperty(Constants.SYS_PROP_BROKER_URL, "vm://localhost?create=false");
	}

	@Test
	@Disabled
	public void testLogInfoEvent() throws JMSException, InterruptedException {
		// Create logger
		Logger logger = LoggerFactory.getLogger(TestService.class);

		testBroker.consumeMessages();

		// Log an event
		logger.info("test event");
		// Wait for jms message to come through
		Thread.sleep(1000);

		// Assert message received
		List<Message> messages = testBroker.getMessages();
		assertEquals(1, messages.size());
		TextMessage actual = (TextMessage) messages.get(0);
		assertTrue(actual.getText().matches("[^ ]+ INFO  org.ihtsdo.buildcloud.telemetry.client.TelemetryEventAppenderTest.testLogInfoEvent - test event\n"));
		assertEquals("INFO", actual.getStringProperty("level"));
	}

	@AfterEach
	public void after() throws JMSException {
		testBroker.close();
	}

}

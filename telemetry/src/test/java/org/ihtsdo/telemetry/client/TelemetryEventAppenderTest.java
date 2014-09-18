package org.ihtsdo.telemetry.client;

import org.ihtsdo.telemetry.TestService;
import org.ihtsdo.telemetry.core.Constants;
import org.ihtsdo.telemetry.server.TestBroker;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.TextMessage;
import java.util.List;

public class TelemetryEventAppenderTest {

	private TestBroker testBroker;

	@Before
	public void setup() throws JMSException {
		testBroker = new TestBroker();

		// Set system property to override log4j appender default broker url
		System.setProperty(Constants.SYS_PROP_BROKER_URL, "vm://localhost?create=false");
	}

	@Test
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
		Assert.assertEquals(1, messages.size());
		TextMessage actual = (TextMessage) messages.get(0);
		Assert.assertTrue(actual.getText().matches("[^ ]+ INFO  org.ihtsdo.telemetry.client.TelemetryEventAppenderTest.testLogInfoEvent - test event\n"));
		Assert.assertEquals("INFO", actual.getStringProperty("level"));
	}

	@After
	public void after() throws JMSException {
		testBroker.close();
	}

}

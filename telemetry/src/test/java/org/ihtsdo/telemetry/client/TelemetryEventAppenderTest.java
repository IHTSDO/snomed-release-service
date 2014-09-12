package org.ihtsdo.telemetry.client;

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
		// Creaate logger
		Logger logger = LoggerFactory.getLogger(getClass());

		testBroker.consumeMessages();

		// Log an event
		logger.info("test event");
		// Wait for jms message to come through
		Thread.sleep(1000);

		// Assert message received
		List<Message> messages = testBroker.getMessages();
		Assert.assertEquals(1, messages.size());
		System.out.println(messages.get(0).getClass());
		TextMessage actual = (TextMessage) messages.get(0);
		Assert.assertEquals("test event", actual.getText());
		Assert.assertEquals("INFO", actual.getStringProperty("level"));
	}

	@After
	public void after() throws JMSException {
		testBroker.close();
	}

}

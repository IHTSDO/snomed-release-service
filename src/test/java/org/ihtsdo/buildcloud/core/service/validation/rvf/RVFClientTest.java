package org.ihtsdo.buildcloud.core.service.validation.rvf;

import org.ihtsdo.buildcloud.core.service.validation.rvf.RVFClient;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.StringReader;
import java.io.StringWriter;

public class RVFClientTest {

	private RVFClient rvfClient;

	@Before
	public void setup() {
		rvfClient = new RVFClient("");
	}

	@Test
	public void testProcessResponseNoFailures() throws Exception {
		String response = "Result\tRow-Column\tFile Name\tFile Path\tColumn Name\tTest Type\tTest Pattern\tFailure Details\tNumber of occurences\n" +
				"\n" +
				"Number of tests run: 37362\n" +
				"Total number of failures: 0\n" +
				"Total number of successes: 37362\n";

		long failureCount = rvfClient.processResponse(new BufferedReader(new StringReader(response)),
				new BufferedWriter(new StringWriter()), "Occured during testProcessResponseNoFailures");

		Assert.assertEquals(0, failureCount);
	}

	@Test
	public void testProcessResponseFailures() throws Exception {
		String response = "Result\tRow-Column\tFile Name\tFile Path\tColumn Name\tTest Type\tTest Pattern\tFailure Details\tNumber of occurences\n" +
				"\n" +
				"Number of tests run: 37362\n" +
				"Total number of failures: 2\n" +
				"Total number of successes: 37360\n";

		long failureCount = rvfClient.processResponse(new BufferedReader(new StringReader(response)),
				new BufferedWriter(new StringWriter()), "Occured during testProcessResponseFailures");

		Assert.assertEquals(2, failureCount);
	}
}
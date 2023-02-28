package org.ihtsdo.buildcloud.core.service.validation.rvf;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.StringReader;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;

public class RVFClientTest {

	private RVFClient rvfClient;

	@BeforeEach
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

		RVFFailDetail failDetail = rvfClient.processResponse(new BufferedReader(new StringReader(response)),
				new BufferedWriter(new StringWriter()), "Occured during testProcessResponseNoFailures");

		assertEquals(0, failDetail.getFailedCount());
	}

	@Test
	public void testProcessResponseFailures() throws Exception {
		String response = "Result\tRow-Column\tFile Name\tFile Path\tColumn Name\tTest Type\tTest Pattern\tFailure Details\tNumber of occurences\n" +
				"\n" +
				"Number of tests run: 37362\n" +
				"Total number of failures: 2\n" +
				"Total number of successes: 37360\n";

		RVFFailDetail failDetail = rvfClient.processResponse(new BufferedReader(new StringReader(response)),
				new BufferedWriter(new StringWriter()), "Occured during testProcessResponseFailures");

		assertEquals(2, failDetail.getFailedCount());
	}
}
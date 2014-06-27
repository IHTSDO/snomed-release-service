package org.ihtsdo;

import org.junit.Assert;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class StreamTestUtils {

	public static void assertStreamsEqualLineByLine(InputStream expectedInputStream, InputStream actualInputStream) throws IOException {
		BufferedReader expectedReader = new BufferedReader(new InputStreamReader(expectedInputStream));
		BufferedReader actualReader = new BufferedReader(new InputStreamReader(actualInputStream));

		int line = 1;
		String actualLine;
		String expectedLine;
		while ((actualLine = actualReader.readLine()) != null) {
			expectedLine = expectedReader.readLine();
			if (expectedLine == null) {
				Assert.fail("Line count mismatch. Expected stream has ended but Actual stream has more lines starting with line " + line + ": " + actualLine);
			} else {
				Assert.assertEquals("Content should be equal on line " + line, expectedLine, actualLine);
			}
			line++;
		}
		if ((expectedLine = expectedReader.readLine()) != null) {
			Assert.fail("Line count mismatch. Actual stream has ended but Expected stream has more lines starting with line " + line + ": " + expectedLine);
		}
	}

}

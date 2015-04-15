package org.ihtsdo.buildcloud.test;

import org.ihtsdo.buildcloud.service.build.RF2Constants;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class StreamTestUtils {

	private static final String UUID_PATTERN = "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}";

	public static void assertStreamsEqualLineByLine(String streamName, InputStream expectedInputStream, InputStream actualInputStream) throws IOException {
		assertStreamsEqualLineByLine(streamName, expectedInputStream, actualInputStream, false);
	}

	public static void assertStreamsEqualLineByLine(InputStream expectedInputStream, InputStream actualInputStream) throws IOException {
		assertStreamsEqualLineByLine(null, expectedInputStream, actualInputStream, false);
	}

	public static void assertStreamsEqualLineByLine(String streamName, InputStream expectedInputStream, BufferedReader actualReader) throws IOException {
		assertStreamsEqualLineByLine(streamName, expectedInputStream, actualReader, false);
	}

	private static void assertStreamsEqualLineByLine(String streamName, InputStream expectedInputStream, InputStream actualInputStream, boolean usePatterns) throws IOException {
		String errorMessageNamePart = getErrorMessagePart(streamName);
		Assert.assertNotNull("Actual InputStream should not be null" + errorMessageNamePart + ".", actualInputStream);
		BufferedReader actualReader = new BufferedReader(new InputStreamReader(actualInputStream, RF2Constants.UTF_8));
		assertStreamsEqualLineByLine(streamName, expectedInputStream, actualReader, usePatterns);
	}

	private static void assertStreamsEqualLineByLine(String streamName, InputStream expectedInputStream, BufferedReader actualReader, boolean usePatterns) throws IOException {
		String errorMessageNamePart = getErrorMessagePart(streamName);

		Assert.assertNotNull("Expected InputStream should not be null" + errorMessageNamePart + ".", expectedInputStream);
		BufferedReader expectedReader = new BufferedReader(new InputStreamReader(expectedInputStream, RF2Constants.UTF_8));

		int line = 1;
		String actualLine;
		String expectedLine;
		while ((actualLine = actualReader.readLine()) != null) {
			expectedLine = expectedReader.readLine();
			if (expectedLine == null) {
				int moreLinesCount = 1 + countRemainingLines(actualReader);
				Assert.fail("Line count mismatch" + errorMessageNamePart + ". Expected stream has ended but Actual stream has " + moreLinesCount + " more lines starting with line " + line + ": " + actualLine);
			} else {
				if (usePatterns) {
					assertEqualsWithPatterns("Content should match pattern" + errorMessageNamePart + " on line " + line, expectedLine, actualLine);
				} else {
					String msg = "Content should be equal" + errorMessageNamePart + " on line " + line + 
							". Expected: " + expectedLine +
							" Actual: " + actualLine;
					Assert.assertEquals(msg, expectedLine, actualLine);
				}
			}
			line++;
		}
		if ((expectedLine = expectedReader.readLine()) != null) {
			int moreLinesCount = 1 + countRemainingLines(expectedReader);
			Assert.fail("Line count mismatch" + errorMessageNamePart + ". Actual stream has ended but Expected stream has " + moreLinesCount + " more lines starting with line " + line + ": " + expectedLine);
		}
	}

	private static String getErrorMessagePart(String streamName) {
		return streamName != null ? " in stream '" + streamName + "'" : "";
	}

	private static int countRemainingLines(BufferedReader reader) throws IOException {
		int moreLinesCount = 0;
		while (reader.readLine() != null) {
			moreLinesCount++;
		}
		return moreLinesCount;
	}

	static void assertEqualsWithPatterns(String message, String expectedLine, String actualLine) {
		String expectedLinePattern = expectedLine.replace("(", "\\(").replace(")", "\\)").replace("|uuid|", UUID_PATTERN);
		Assert.assertMatches(message, expectedLinePattern, actualLine);
	}

	/**
	 * This class here rather than having test dependencies going into the software package.
	 */
	private static class Assert {

		private static void assertEquals(String message, String expectedLine, String actual) {
			if (!expectedLine.equals(actual)) {
				throw new AssertionError(message + "\nExpected '" + expectedLine + "'\nActual   '" + actual + "'");
			}
		}

		public static void assertMatches(String message, String pattern, String actual) {
			if (!actual.matches(pattern)) {
				throw new AssertionError(message + "\nPattern '" + pattern + "'\nActual  '" + actual + "'");
			}
		}

		public static void assertNotNull(String message, Object object) {
			if (object == null) {
				throw new AssertionError(message);
			}
		}

		private static void fail(String message) {
			throw new AssertionError(message);
		}
	}

}

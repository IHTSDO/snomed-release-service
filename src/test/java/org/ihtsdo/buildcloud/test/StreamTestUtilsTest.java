package org.ihtsdo.buildcloud.test;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class StreamTestUtilsTest {

	public static final String BLANK_MESSAGE = "";

	@Test
	public void testAssertEqualsWithPatternsPass() throws Exception {
		StreamTestUtils.assertEqualsWithPatterns(BLANK_MESSAGE, "|uuid| abc", "cb8bee92-e78a-40c8-838c-7a086af1e27d abc");
		StreamTestUtils.assertEqualsWithPatterns(BLANK_MESSAGE, "abc |uuid| abc", "abc cb8bee92-e78a-40c8-838c-7a086af1e27d abc");
		StreamTestUtils.assertEqualsWithPatterns(BLANK_MESSAGE, "abc |uuid|", "abc cb8bee92-e78a-40c8-838c-7a086af1e27d");
		StreamTestUtils.assertEqualsWithPatterns(BLANK_MESSAGE, "abc |uuid| absdfsd |uuid|", "abc cb8bee92-e78a-40c8-838c-7a086af1e27d absdfsd fee8159c-d0db-4fd0-8164-97709ad509fd");
		StreamTestUtils.assertEqualsWithPatterns(BLANK_MESSAGE, "123123", "123123");
	}

	@Test
	public void testAssertEqualsWithPatternsFail() throws Exception {
		assertPatternFails("|uuid| abc", " abc");
		assertPatternFails("|uuid| abc", "123 abc");
	}

	private void assertPatternFails(String expectedLine, String actualLine) {
		boolean exceptionThrown = false;
		try {
			StreamTestUtils.assertEqualsWithPatterns(BLANK_MESSAGE, expectedLine, actualLine);
		} catch (AssertionError e) {
			exceptionThrown = true;
		}
		if (!exceptionThrown) {
			fail("Should have failed.");
		}
	}

}

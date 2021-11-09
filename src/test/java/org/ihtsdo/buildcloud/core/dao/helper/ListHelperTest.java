package org.ihtsdo.buildcloud.core.dao.helper;

import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ListHelperTest {
	@Test
	public void page_ShouldThrowException_WhenGivenIllegalInput() {
		assertThrows(IllegalArgumentException.class, () -> ListHelper.page(null, 10, 10));
		assertThrows(IllegalArgumentException.class, () -> ListHelper.page(List.of("1"), -1, 10));
		assertThrows(IllegalArgumentException.class, () -> ListHelper.page(List.of("1"), 1, 0));
	}

	@Test
	public void page_ShouldReturnExpectedResults_WhenPagingPage0() {
		// given
		List<Scenario> scenarios = List.of(
				new Scenario(
						0,
						1,
						List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10"),
						List.of("1")
				),
				new Scenario(
						0,
						5,
						List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10"),
						List.of("1", "2", "3", "4", "5")
				),
				new Scenario(
						0,
						9,
						List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10"),
						List.of("1", "2", "3", "4", "5", "6", "7", "8", "9")
				),
				new Scenario(
						0,
						10,
						List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10"),
						List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10")
				),
				new Scenario(
						0,
						11,
						List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10"),
						List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10") // Requested out of bounds; return remaining
				),
				new Scenario(
						0,
						50,
						List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10"),
						List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10") // Requested out of bounds; return remaining
				)
		);

		for (Scenario scenario : scenarios) {
			// when
			List<String> result = ListHelper.page(scenario.getSource(), scenario.getPageNumber(), scenario.getPageSize());

			// then
			assertResult(result, scenario.getExpectedResult());
		}
	}

	@Test
	public void page_ShouldReturnExpectedResults_WhenPagingPage1() {
		// given
		List<Scenario> scenarios = List.of(
				new Scenario(
						1,
						1,
						List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10"),
						List.of("2")
				),
				new Scenario(
						1,
						3,
						List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10"),
						List.of("4", "5", "6")
				),
				new Scenario(
						1,
						4,
						List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10"),
						List.of("5", "6", "7", "8")
				),
				new Scenario(
						1,
						5,
						List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10"),
						List.of("6", "7", "8", "9", "10")
				),
				new Scenario(
						1,
						6,
						List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10"),
						List.of("7", "8", "9", "10")
				)
		);

		for (Scenario scenario : scenarios) {
			// when
			List<String> result = ListHelper.page(scenario.getSource(), scenario.getPageNumber(), scenario.getPageSize());

			// then
			assertResult(result, scenario.getExpectedResult());
		}
	}

	@Test
	public void page_ShouldReturnExpectedResults_WhenPagingPage2() {
		// given
		List<Scenario> scenarios = List.of(
				new Scenario(
						2,
						1,
						List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10"),
						List.of("3")
				),
				new Scenario(
						2,
						3,
						List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10"),
						List.of("7", "8", "9")
				),
				new Scenario(
						2,
						4,
						List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10"),
						List.of("9", "10")
				)
		);

		for (Scenario scenario : scenarios) {
			// when
			List<String> result = ListHelper.page(scenario.getSource(), scenario.getPageNumber(), scenario.getPageSize());

			// then
			assertResult(result, scenario.getExpectedResult());
		}
	}

	@Test
	public void page_ShouldReturnExpectedResults_WhenPagingAfterEnd() {
		// given
		List<Scenario> scenarios = List.of(
				new Scenario(
						10,
						10,
						List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10"),
						Collections.emptyList()
				)
		);

		for (Scenario scenario : scenarios) {
			// when
			List<String> result = ListHelper.page(scenario.getSource(), scenario.getPageNumber(), scenario.getPageSize());

			// then
			assertResult(result, scenario.getExpectedResult());
		}
	}

	private void assertResult(List<String> result, List<String> expectedResult) {
		assertEquals(expectedResult.size(), result.size(), "Size of assertions do not match.");
		for (int x = 0; x < expectedResult.size(); x++) {
			assertEquals(result.get(x), expectedResult.get(x), "Index of assertion does not match.");
		}
	}

	private static class Scenario {
		/**
		 * Number of page to request in test.
		 */
		private final int pageNumber;

		/**
		 * Size of page to request in test.
		 */
		private final int pageSize;

		/**
		 * Collection to page in test.
		 */
		private final List<String> source;

		/**
		 * Expected returned collection from helper in test.
		 */
		private final List<String> expectedResult;

		private Scenario(int pageNumber, int pageSize, List<String> source, List<String> expectedResult) {
			this.pageNumber = pageNumber;
			this.pageSize = pageSize;
			this.source = source;
			this.expectedResult = expectedResult;
		}

		public int getPageNumber() {
			return pageNumber;
		}

		public int getPageSize() {
			return pageSize;
		}

		public List<String> getSource() {
			return source;
		}

		public List<String> getExpectedResult() {
			return expectedResult;
		}
	}
}

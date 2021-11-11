package org.ihtsdo.buildcloud.core.dao.helper;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ListHelperTest {
	@Test
	public void page_ShouldThrowException_WhenGivenIllegalInput() {
		assertThrows(IllegalArgumentException.class, () -> ListHelper.page(List.of("1"), -1, 10));
		assertThrows(IllegalArgumentException.class, () -> ListHelper.page(List.of("1"), 1, 0));
	}

	@Test
	public void page_ShouldReturnExpectedResults_WhenPagingEmptyData() {
		// given
		List<Scenario> scenarios = List.of(
				new Scenario(
						0,
						1,
						Collections.emptyList(),
						Collections.emptyList()
				),
				new Scenario(
						0,
						1,
						null,
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

	@Test
	public void getTotalPages_ShouldThrowException_WhenGivenIllegalInput() {
		assertThrows(IllegalArgumentException.class, () -> ListHelper.getTotalPages(new ArrayList<>(), -1));
		assertThrows(IllegalArgumentException.class, () -> ListHelper.getTotalPages(new ArrayList<>(), 0));
	}

	@Test
	public void getTotalPages_ShouldReturnExpectedResults_WhenQueryingPage0() {
		// given
		List<Scenario> scenarios = List.of(
				new Scenario(
						1,
						Collections.emptyList(),
						0
				),
				new Scenario(
						2,
						List.of("1"),
						1
				),
				new Scenario(
						1,
						List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10"),
						10
				),
				new Scenario(
						2,
						List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10"),
						5
				),
				new Scenario(
						3,
						List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10"),
						4
				),
				new Scenario(
						4,
						List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10"),
						3
				),
				new Scenario(
						5,
						List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10"),
						2
				),
				new Scenario(
						6,
						List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10"),
						2
				),
				new Scenario(
						7,
						List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10"),
						2
				),
				new Scenario(
						8,
						List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10"),
						2
				),
				new Scenario(
						9,
						List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10"),
						2
				),
				new Scenario(
						10,
						List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10"),
						1
				)
		);

		for (Scenario scenario : scenarios) {
			// when
			int result = ListHelper.getTotalPages(scenario.getSource(), scenario.getPageSize());

			// then
			assertResult(result, scenario.getExpectedTotalPages());
		}
	}

	private void assertResult(List<String> result, List<String> expectedResult) {
		assertEquals(expectedResult.size(), result.size(), "Size of assertions do not match.");
		for (int x = 0; x < expectedResult.size(); x++) {
			assertEquals(result.get(x), expectedResult.get(x), "Index of assertion does not match.");
		}
	}

	private void assertResult(int actualResult, int expectedResult) {
		assertEquals(expectedResult, actualResult, "Size of total pages do not match.");
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

		/**
		 * Expected returned total pages from helper in test.
		 */
		private final int expectedTotalPages;

		/**
		 * Scenario for asserting paging with expected elements returned.
		 */
		private Scenario(int pageNumber, int pageSize, List<String> source, List<String> expectedResult) {
			this.pageNumber = pageNumber;
			this.pageSize = pageSize;
			this.source = source;
			this.expectedResult = expectedResult;
			this.expectedTotalPages = 0;
		}

		/**
		 * Scenario for asserting paging with expected total pages returned.
		 */
		private Scenario(int pageSize, List<String> source, int expectedTotalPages) {
			this.pageNumber = 0;
			this.pageSize = pageSize;
			this.source = source;
			this.expectedResult = new ArrayList<>();
			this.expectedTotalPages = expectedTotalPages;
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

		public int getExpectedTotalPages() {
			return expectedTotalPages;
		}
	}
}

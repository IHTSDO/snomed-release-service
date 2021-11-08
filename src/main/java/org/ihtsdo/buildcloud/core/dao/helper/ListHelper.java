package org.ihtsdo.buildcloud.core.dao.helper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ListHelper {
	private ListHelper() {
		// ignore
	}

	private static void verifyParams(int pageNumber, int pageSize) {
		if (pageNumber < 0) {
			throw new IllegalArgumentException("Invalid pageNumber.");
		}

		if (pageSize <= 0) {
			throw new IllegalArgumentException("Invalid pageSize.");
		}
	}

	private static <T> List<T> subList(List<T> source, int forwardToIndex) {
		List<T> mutableList = new ArrayList<>(source);
		mutableList.subList(0, forwardToIndex).clear();

		return mutableList;
	}

	/**
	 * Return sub list from given list. This method will page through the given list in a null and exception safe manner.
	 *
	 * @param source     List to return sub list from.
	 * @param pageNumber Start index of page.
	 * @param pageSize   End index of page.
	 * @param <T>        Type held in collection.
	 * @return Sub list from given list.
	 */
	public static <T> List<T> page(List<T> source, int pageNumber, int pageSize) {
		verifyParams(pageNumber, pageSize);

		if (source == null || source.isEmpty()) {
			return Collections.emptyList();
		}

		int sourceSize = source.size();
		if (pageNumber == 0) { // No fast-forwarding required.
			if (sourceSize <= pageSize) {
				return source; // Return remaining as requested more than what's available
			}

			return source.subList(pageNumber, pageSize); // Return sub list
		}

		int fastForwardToIndex = pageNumber * pageSize; // Same size for all previous page(s)
		if (fastForwardToIndex > sourceSize) {
			return Collections.emptyList(); // Return nothing if fast-forwarded past what's available
		}

		List<T> subList = subList(source, fastForwardToIndex);
		if (pageSize > subList.size()) {
			return subList; // Return remaining as requested more than what's available
		}

		return subList.subList(0, pageSize); // Return sub list
	}
}

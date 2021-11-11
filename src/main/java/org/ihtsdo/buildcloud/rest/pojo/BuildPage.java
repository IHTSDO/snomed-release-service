package org.ihtsdo.buildcloud.rest.pojo;

import java.util.Collections;
import java.util.List;

public class BuildPage<T> {
	private final int totalElements;
	private final int totalPages;
	private final int pageNumber;
	private final int pageSize;
	private final boolean empty;
	private final List<T> content;

	/**
	 * Return BuildPage representing no content.
	 *
	 * @param <T> Type held in content.
	 * @return BuildPage representing no content.
	 */
	public static <T> BuildPage<T> empty() {
		return new BuildPage<>(0, 0, 0, 1, Collections.emptyList());
	}

	public BuildPage(int totalElements, int totalPages, int pageNumber, int pageSize, List<T> content) {
		this.totalElements = totalElements;
		this.totalPages = totalPages;
		this.pageNumber = pageNumber;
		this.pageSize = pageSize;
		this.empty = content.isEmpty();
		this.content = content;
	}

	public int getTotalElements() {
		return totalElements;
	}

	public int getTotalPages() {
		return totalPages;
	}

	public int getPageNumber() {
		return pageNumber;
	}

	public int getPageSize() {
		return pageSize;
	}

	public boolean isEmpty() {
		return empty;
	}

	public List<T> getContent() {
		return content;
	}
}

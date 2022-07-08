package org.ihtsdo.buildcloud.rest.controller.helper;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;

public class PageRequestHelper {
	public static PageRequest createPageRequest(Integer page, Integer size, List<String> fields, List<String> directions) {
		int offset = page != null ? page : 0;
		int limit = size != null ? size : 10;
		if (fields != null) {
			List<Sort.Order> orders = new ArrayList<>();
			for (int i = 0; i < fields.size(); i++) {
				String fieldSortDirection = directions.get(i);
				Sort.Direction sortDirection = (fieldSortDirection != null && fieldSortDirection.equalsIgnoreCase("desc")) ? Sort.Direction.DESC : Sort.Direction.ASC;
				Sort.Order order = new Sort.Order(sortDirection, fields.get(i));
				orders.add(order);
			}
			return PageRequest.of(offset, limit, Sort.by(orders));
		} else {
			return PageRequest.of(offset, limit);
		}
	}
}

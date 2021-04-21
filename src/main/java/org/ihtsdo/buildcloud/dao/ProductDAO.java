package org.ihtsdo.buildcloud.dao;

import org.ihtsdo.buildcloud.entity.Product;
import org.ihtsdo.buildcloud.service.helper.FilterOption;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.Set;

public interface ProductDAO extends EntityDAO<Product> {

	Page<Product> findAll(Set<FilterOption> filterOptions, Pageable pageable);

	Page<Product> findAll(String releaseCenterBusinessKey, Set<FilterOption> filterOptions, Pageable pageable);

	Product find(String releaseCenterKey, String productKey);

	Product find(Long id);

}

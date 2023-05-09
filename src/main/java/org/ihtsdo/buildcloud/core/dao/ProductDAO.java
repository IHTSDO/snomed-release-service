package org.ihtsdo.buildcloud.core.dao;

import org.ihtsdo.buildcloud.core.entity.Product;
import org.ihtsdo.buildcloud.core.service.helper.FilterOption;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Set;

public interface ProductDAO extends EntityDAO<Product> {

	Page<Product> findAll(Set<FilterOption> filterOptions, Pageable pageable);

	Page<Product> findAll(String releaseCenterKey, Set<FilterOption> filterOptions, Pageable pageable);

	Page<Product> findHiddenProducts(String releaseCenterKey, Pageable pageable);

	Product find(String releaseCenterKey, String productKey);

	Product find(String productKey);

	Product find(Long id);

}

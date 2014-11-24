package org.ihtsdo.buildcloud.dao;

import org.ihtsdo.buildcloud.entity.Product;
import org.ihtsdo.buildcloud.entity.User;
import org.ihtsdo.buildcloud.service.helper.FilterOption;

import java.util.List;
import java.util.Set;

public interface ProductDAO extends EntityDAO<Product> {

	List<Product> findAll(Set<FilterOption> filterOptions, User user);

	List<Product> findAll(String releaseCenterBusinessKey, Set<FilterOption> filterOptions, User user);

	Product find(String releaseCenterKey, String productKey, User user);

	Product find(Long id, User user);

}

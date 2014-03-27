package org.ihtsdo.buildcloud.dao;

import org.ihtsdo.buildcloud.entity.Product;

public interface ProductDAO extends EntityDAO<Product> {

	Product find(String releaseCenterBusinessKey, String extensionBusinessKey, String productBusinessKey, String authenticatedId);

}

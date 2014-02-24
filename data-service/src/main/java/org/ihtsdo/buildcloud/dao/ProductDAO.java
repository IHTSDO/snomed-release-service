package org.ihtsdo.buildcloud.dao;

import org.ihtsdo.buildcloud.entity.Product;

public interface ProductDAO extends EntityDAO<Product> {

	Product find(String releaseCentreBusinessKey, String extensionBusinessKey, String productBusinessKey, String authenticatedId);

}

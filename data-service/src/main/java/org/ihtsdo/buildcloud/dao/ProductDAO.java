package org.ihtsdo.buildcloud.dao;

import org.ihtsdo.buildcloud.entity.Product;
import org.ihtsdo.buildcloud.entity.User;

public interface ProductDAO extends EntityDAO<Product> {

	Product find(String releaseCenterBusinessKey, String extensionBusinessKey, String productBusinessKey, User user);

}

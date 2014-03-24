package org.ihtsdo.buildcloud.service;

import org.ihtsdo.buildcloud.entity.Product;

import java.util.List;

public interface ProductService extends EntityService<Product> {

	List<Product> findAll(String releaseCentreBusinessKey, String extensionBusinessKey, String oauthId) throws Exception;

	Product find(String releaseCentreBusinessKey, String extensionBusinessKey, String productBusinessKey, String oauthId);

	Product create(String releaseCentreBusinessKey, String extensionBusinessKey, String name, String authenticatedId);

}

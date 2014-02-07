package org.ihtsdo.buildcloud.service;

import org.ihtsdo.buildcloud.entity.Product;

import java.util.List;
import java.util.Set;

public interface ProductService {

	List<Product> findAll(String releaseCentreBusinessKey, String extensionBusinessKey, String oauthId);

	Product find(String releaseCentreBusinessKey, String extensionBusinessKey, String productBusinessKey, String oauthId);

}

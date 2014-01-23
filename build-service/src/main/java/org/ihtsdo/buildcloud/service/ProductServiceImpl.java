package org.ihtsdo.buildcloud.service;

import org.hibernate.Hibernate;
import org.ihtsdo.buildcloud.dao.ExtensionDAO;
import org.ihtsdo.buildcloud.dao.ProductDAO;
import org.ihtsdo.buildcloud.dao.ReleaseCentreDAO;
import org.ihtsdo.buildcloud.entity.Extension;
import org.ihtsdo.buildcloud.entity.Product;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@Transactional
public class ProductServiceImpl implements ProductService {

	@Autowired
	private ProductDAO productDAO;

	@Autowired
	private ExtensionDAO extensionDAO;

	@Override
	public Set<Product> findAll(String releaseCentreBusinessKey, String extensionBusinessKey, String oauthId) {
		Set<Product> products = extensionDAO.find(releaseCentreBusinessKey, extensionBusinessKey, oauthId).getProducts();
		Hibernate.initialize(products);
		return products;
	}

	@Override
	public Product find(String releaseCentreBusinessKey, String extensionBusinessKey, String productBusinessKey, String authenticatedId) {
		return productDAO.find(releaseCentreBusinessKey, extensionBusinessKey, productBusinessKey, authenticatedId);
	}

}

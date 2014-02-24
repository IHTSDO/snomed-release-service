package org.ihtsdo.buildcloud.service;

import org.hibernate.Hibernate;
import org.ihtsdo.buildcloud.dao.ExtensionDAO;
import org.ihtsdo.buildcloud.dao.ProductDAO;
import org.ihtsdo.buildcloud.entity.Product;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ProductServiceImpl extends EntityServiceImpl<Product> implements ProductService {

	@Autowired
	private ProductDAO productDAO;

	@Autowired
	private ExtensionDAO extensionDAO;

	@Autowired
	protected ProductServiceImpl(ProductDAO dao) {
		super(dao);
	}

	@Override
	public List<Product> findAll(String releaseCentreBusinessKey, String extensionBusinessKey, String oauthId) {
		List<Product> products = extensionDAO.find(releaseCentreBusinessKey, extensionBusinessKey, oauthId).getProducts();
		Hibernate.initialize(products);
		return products;
	}

	@Override
	public Product find(String releaseCentreBusinessKey, String extensionBusinessKey, String productBusinessKey, String oauthId) {
		return productDAO.find(releaseCentreBusinessKey, extensionBusinessKey, productBusinessKey, oauthId);
	}

}

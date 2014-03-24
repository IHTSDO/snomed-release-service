package org.ihtsdo.buildcloud.service;

import org.hibernate.Hibernate;
import org.ihtsdo.buildcloud.dao.ExtensionDAO;
import org.ihtsdo.buildcloud.dao.ProductDAO;
import org.ihtsdo.buildcloud.entity.Extension;
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
	public List<Product> findAll(String releaseCentreBusinessKey, String extensionBusinessKey, String oauthId) throws Exception{
		Extension extension = extensionDAO.find(releaseCentreBusinessKey, extensionBusinessKey, oauthId);
		
		if (extension == null) {
			throw new Exception ("Unable to find extension with path: " + releaseCentreBusinessKey + "/" + extensionBusinessKey);
		}
		List<Product> products = extension.getProducts();
		Hibernate.initialize(products);
		return products;
	}

	@Override
	public Product find(String releaseCentreBusinessKey, String extensionBusinessKey, String productBusinessKey, String oauthId) {
		return productDAO.find(releaseCentreBusinessKey, extensionBusinessKey, productBusinessKey, oauthId);
	}

	@Override
	public Product create(String releaseCentreBusinessKey, String extensionBusinessKey, String name, String authenticatedId) {
		Extension extension = extensionDAO.find(releaseCentreBusinessKey, extensionBusinessKey, authenticatedId);
		Product product = new Product(name);
		extension.addProduct(product);
		productDAO.save(product);
		return product;
	}

}

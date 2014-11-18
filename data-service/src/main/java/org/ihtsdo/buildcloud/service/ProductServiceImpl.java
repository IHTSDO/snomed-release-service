package org.ihtsdo.buildcloud.service;

import org.hibernate.Hibernate;
import org.ihtsdo.buildcloud.dao.ExtensionDAO;
import org.ihtsdo.buildcloud.dao.ProductDAO;
import org.ihtsdo.buildcloud.entity.Extension;
import org.ihtsdo.buildcloud.entity.Product;
import org.ihtsdo.buildcloud.entity.User;
import org.ihtsdo.buildcloud.entity.helper.EntityHelper;
import org.ihtsdo.buildcloud.service.exception.BusinessServiceException;
import org.ihtsdo.buildcloud.service.exception.EntityAlreadyExistsException;
import org.ihtsdo.buildcloud.service.exception.ResourceNotFoundException;
import org.ihtsdo.buildcloud.service.helper.CompositeKeyHelper;
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
	public List<Product> findAll(String releaseCenterBusinessKey, String extensionBusinessKey, User authenticatedUser) throws ResourceNotFoundException {
		Extension extension = extensionDAO.find(releaseCenterBusinessKey, extensionBusinessKey, authenticatedUser);

		if (extension == null) {
			String item = CompositeKeyHelper.getPath(releaseCenterBusinessKey, extensionBusinessKey);
			throw new ResourceNotFoundException("Unable to find extension: " + item);
		}
		List<Product> products = extension.getProducts();
		Hibernate.initialize(products);
		return products;
	}

	@Override
	public Product find(String releaseCenterBusinessKey, String extensionBusinessKey, String productBusinessKey, User authenticatedUser) {
		return productDAO.find(releaseCenterBusinessKey, extensionBusinessKey, productBusinessKey, authenticatedUser);
	}

	@Override
	public Product create(String releaseCenterBusinessKey, String extensionBusinessKey, String name, User authenticatedUser) throws BusinessServiceException {
		Extension extension = extensionDAO.find(releaseCenterBusinessKey, extensionBusinessKey, authenticatedUser);
		if (extension == null) {
			String item = CompositeKeyHelper.getPath(releaseCenterBusinessKey, extensionBusinessKey);
			throw new ResourceNotFoundException("Unable to find extension: " + item);
		}

		//Check that we don't already have one of these
		String productBusinessKey = EntityHelper.formatAsBusinessKey(name);
		Product existingProduct = productDAO.find(releaseCenterBusinessKey, extensionBusinessKey, productBusinessKey, authenticatedUser);
		if (existingProduct != null) {
			throw new EntityAlreadyExistsException(name + " already exists.");
		}

		Product product = new Product(name);
		extension.addProduct(product);
		productDAO.save(product);
		return product;
	}

}

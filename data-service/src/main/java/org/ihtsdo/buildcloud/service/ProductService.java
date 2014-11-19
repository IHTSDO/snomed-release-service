package org.ihtsdo.buildcloud.service;

import org.ihtsdo.buildcloud.entity.Product;
import org.ihtsdo.buildcloud.service.exception.BusinessServiceException;
import org.ihtsdo.buildcloud.service.exception.ResourceNotFoundException;

import java.util.List;

public interface ProductService extends EntityService<Product> {

	List<Product> findAll(String releaseCenterBusinessKey, String extensionBusinessKey) throws ResourceNotFoundException;

	Product find(String releaseCenterBusinessKey, String extensionBusinessKey, String productBusinessKey);

	Product create(String releaseCenterBusinessKey, String extensionBusinessKey, String name) throws BusinessServiceException;

}

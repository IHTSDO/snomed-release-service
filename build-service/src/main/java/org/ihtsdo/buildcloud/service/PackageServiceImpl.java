package org.ihtsdo.buildcloud.service;

import org.hibernate.Hibernate;
import org.ihtsdo.buildcloud.dao.PackageDAO;
import org.ihtsdo.buildcloud.dao.ProductDAO;
import org.ihtsdo.buildcloud.entity.Package;
import org.ihtsdo.buildcloud.entity.Product;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@Transactional
public class PackageServiceImpl implements PackageService {

	@Autowired
	private PackageDAO packageDAO;

	@Autowired
	private ProductDAO productDAO;

	@Override
	public Set<Package> findAll(String releaseCentreBusinessKey, String extensionBusinessKey, String productBusinessKey, String oauthId) {
		Set<Package> packages = productDAO.find(releaseCentreBusinessKey, extensionBusinessKey, productBusinessKey, oauthId).getPackages();
		Hibernate.initialize(packages);
		return packages;
	}

	@Override
	public Package find(String releaseCentreBusinessKey, String extensionBusinessKey, String productBusinessKey,
						String packageBusinessKey, String oauthId) {
		return packageDAO.find(releaseCentreBusinessKey, extensionBusinessKey, productBusinessKey, packageBusinessKey, oauthId);
	}
}

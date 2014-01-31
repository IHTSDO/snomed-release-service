package org.ihtsdo.buildcloud.service;

import org.hibernate.Hibernate;
import org.ihtsdo.buildcloud.dao.BuildDAO;
import org.ihtsdo.buildcloud.dao.PackageDAO;
import org.ihtsdo.buildcloud.entity.Package;
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
	private BuildDAO buildDAO;

	@Override
	public Set<Package> findAll(String releaseCentreBusinessKey, String extensionBusinessKey, String productBusinessKey, String buildBusinessKey, String authenticatedId) {
		Set<Package> packages = buildDAO.find(releaseCentreBusinessKey, extensionBusinessKey, productBusinessKey, buildBusinessKey, authenticatedId).getPackages();
		Hibernate.initialize(packages);
		return packages;
	}

	@Override
	public Package find(String releaseCentreBusinessKey, String extensionBusinessKey, String productBusinessKey,
						String buildBusinessKey, String packageBusinessKey, String authenticatedId) {
		return packageDAO.find(releaseCentreBusinessKey, extensionBusinessKey, productBusinessKey, buildBusinessKey, packageBusinessKey, authenticatedId);
	}
}

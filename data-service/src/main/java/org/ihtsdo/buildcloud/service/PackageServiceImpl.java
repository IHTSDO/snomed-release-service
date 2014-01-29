package org.ihtsdo.buildcloud.service;

import org.hibernate.Hibernate;
import org.ihtsdo.buildcloud.dao.PackageDAO;
import org.ihtsdo.buildcloud.dao.ReleaseDAO;
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
	private ReleaseDAO releaseDAO;

	@Override
	public Set<Package> findAll(String releaseCentreBusinessKey, String extensionBusinessKey, String productBusinessKey, String releaseBusinessKey, String authenticatedId) {
		Set<Package> packages = releaseDAO.find(releaseCentreBusinessKey, extensionBusinessKey, productBusinessKey, releaseBusinessKey, authenticatedId).getPackages();
		Hibernate.initialize(packages);
		return packages;
	}

	@Override
	public Package find(String releaseCentreBusinessKey, String extensionBusinessKey, String productBusinessKey,
						String releaseBusinessKey, String packageBusinessKey, String authenticatedId) {
		return packageDAO.find(releaseCentreBusinessKey, extensionBusinessKey, productBusinessKey, releaseBusinessKey, packageBusinessKey, authenticatedId);
	}
}

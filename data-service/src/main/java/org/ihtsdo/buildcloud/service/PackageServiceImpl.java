package org.ihtsdo.buildcloud.service;

import org.ihtsdo.buildcloud.dao.BuildDAO;
import org.ihtsdo.buildcloud.dao.PackageDAO;
import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.Package;
import org.ihtsdo.buildcloud.entity.User;
import org.ihtsdo.buildcloud.service.helper.CompositeKeyHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class PackageServiceImpl extends EntityServiceImpl<Package> implements PackageService {

	@Autowired
	private PackageDAO packageDAO;

	@Autowired
	private BuildDAO buildDAO;

	@Autowired
	protected PackageServiceImpl(PackageDAO dao) {
		super(dao);
	}

	@Override
	public Package find(String buildCompositeKey, String packageBusinessKey, User authenticatedUser) {
		Long buildId = CompositeKeyHelper.getId(buildCompositeKey);
		return packageDAO.find(buildId, packageBusinessKey, authenticatedUser);
	}

	@Override
	public List<Package> findAll(String buildCompositeKey, User authenticatedUser) {
		Long buildId = CompositeKeyHelper.getId(buildCompositeKey);
		return new ArrayList<Package>(buildDAO.find(buildId, authenticatedUser).getPackages());
	}
	
	@Override
	public Package create(String buildCompositeKey, String name, User authenticatedUser) {
		
		Long buildId = CompositeKeyHelper.getId(buildCompositeKey);
		Build build = buildDAO.find(buildId, authenticatedUser);
		Package pkg = new Package(name);
		build.addPackage(pkg);
		packageDAO.save(pkg);
		return pkg;
	}	
}

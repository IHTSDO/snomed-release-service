package org.ihtsdo.buildcloud.service;

import org.ihtsdo.buildcloud.dao.BuildDAO;
import org.ihtsdo.buildcloud.dao.PackageDAO;
import org.ihtsdo.buildcloud.entity.Package;
import org.ihtsdo.buildcloud.service.helper.CompositeKeyHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class PackageServiceImpl implements PackageService {

	@Autowired
	private PackageDAO packageDAO;

	@Autowired
	private BuildDAO buildDAO;

	@Override
	public Package find(String buildCompositeKey, String packageBusinessKey, String authenticatedId) {
		Long buildId = CompositeKeyHelper.getId(buildCompositeKey);
		return packageDAO.find(buildId, packageBusinessKey, authenticatedId);
	}

	@Override
	public List<Package> findAll(String buildCompositeKey, String authenticatedId) {
		Long buildId = CompositeKeyHelper.getId(buildCompositeKey);
		return new ArrayList(buildDAO.find(buildId, authenticatedId).getPackages());
	}
}

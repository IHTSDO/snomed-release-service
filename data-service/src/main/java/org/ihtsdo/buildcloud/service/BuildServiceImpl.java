package org.ihtsdo.buildcloud.service;

import org.hibernate.Hibernate;
import org.ihtsdo.buildcloud.dao.BuildDAO;
import org.ihtsdo.buildcloud.dao.ProductDAO;
import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.service.helper.CompositeKeyHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@Transactional
public class BuildServiceImpl implements BuildService {

	@Autowired
	private BuildDAO buildDAO;

	@Autowired
	private ProductDAO productDAO;

	@Override
	public List<Build> findAll(String authenticatedId) {
		return buildDAO.findAll(authenticatedId);
	}

	@Override
	public Build find(String buildCompositeKey, String authenticatedId) {
		Long id = CompositeKeyHelper.getId(buildCompositeKey);
		return buildDAO.find(id, authenticatedId);
	}

	@Override
	public Set<Build> findForProduct(String releaseCentreBusinessKey, String extensionBusinessKey, String productBusinessKey, String authenticatedId) {
		Set<Build> builds = productDAO.find(releaseCentreBusinessKey, extensionBusinessKey, productBusinessKey, authenticatedId).getBuilds();
		Hibernate.initialize(builds);
		return builds;
	}

}

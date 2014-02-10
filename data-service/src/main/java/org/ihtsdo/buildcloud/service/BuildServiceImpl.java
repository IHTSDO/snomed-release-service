package org.ihtsdo.buildcloud.service;

import org.hibernate.Hibernate;
import org.ihtsdo.buildcloud.dao.BuildDAO;
import org.ihtsdo.buildcloud.dao.ProductDAO;
import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.service.helper.CompositeKeyHelper;
import org.ihtsdo.buildcloud.service.maven.MavenExecutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class BuildServiceImpl implements BuildService {

	@Autowired
	private BuildDAO buildDAO;

	@Autowired
	private ProductDAO productDAO;

	@Autowired
	private MavenExecutor mavenExecutor;

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
	public 	Map<String, Object> getConfig(String buildCompositeKey, String authenticatedId) {
		Long id = CompositeKeyHelper.getId(buildCompositeKey);
		Build build =  buildDAO.find(id, authenticatedId);
		return build.getConfig();
	}

	@Override
	public List<Build> findForProduct(String releaseCentreBusinessKey, String extensionBusinessKey, String productBusinessKey, String authenticatedId) {
		List<Build> builds = productDAO.find(releaseCentreBusinessKey, extensionBusinessKey, productBusinessKey, authenticatedId).getBuilds();
		Hibernate.initialize(builds);
		return builds;
	}

	@Override
	public String run(String buildCompositeKey, String authenticatedId) throws IOException {
		Date triggerDate = new Date();

		// Call generate poms
		ClassPathResource buildFilesDirectory = new ClassPathResource("/example-build/");
		Long id = CompositeKeyHelper.getId(buildCompositeKey);
		Build build = buildDAO.find(id, authenticatedId);
		Hibernate.initialize(build.getPackages());

		return mavenExecutor.exec(build, buildFilesDirectory, triggerDate);
	}

}

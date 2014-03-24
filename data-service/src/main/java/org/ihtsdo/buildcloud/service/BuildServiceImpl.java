package org.ihtsdo.buildcloud.service;

import org.hibernate.Hibernate;
import org.ihtsdo.buildcloud.dao.BuildDAO;
import org.ihtsdo.buildcloud.dao.ProductDAO;
import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.Product;
import org.ihtsdo.buildcloud.service.helper.CompositeKeyHelper;
import org.ihtsdo.buildcloud.service.helper.FilterOption;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;

@Service
@Transactional
public class BuildServiceImpl extends EntityServiceImpl<Build> implements BuildService {

	@Autowired
	private BuildDAO buildDAO;

	@Autowired
	private ProductDAO productDAO;

	@Autowired
	public BuildServiceImpl(BuildDAO dao) {
		super(dao);
	}

	@Override
	public List<Build> findAll(EnumSet<FilterOption> filterOptions, String authenticatedId) {
		return buildDAO.findAll(filterOptions, authenticatedId);
	}

	@Override
	public Build find(String buildCompositeKey, String authenticatedId) {
		Long id = CompositeKeyHelper.getId(buildCompositeKey);
		return buildDAO.find(id, authenticatedId);
	}
	
	@Override
	public List<Build> findForProduct(String releaseCentreBusinessKey, String extensionBusinessKey, String productBusinessKey, String authenticatedId) {
		List<Build> builds = productDAO.find(releaseCentreBusinessKey, extensionBusinessKey, productBusinessKey, authenticatedId).getBuilds();
		Hibernate.initialize(builds);
		return builds;
	}
	
	@Override
	public List<Build> findForExtension(String releaseCentreBusinessKey, String extensionBusinessKey, EnumSet<FilterOption> filterOptions, String authenticatedId) {
		List<Build> builds = buildDAO.findAll(releaseCentreBusinessKey, extensionBusinessKey,  filterOptions, authenticatedId);
		Hibernate.initialize(builds);
		return builds;
	}

	@Override
	public Build create(String releaseCentreBusinessKey, String extensionBusinessKey, String productBusinessKey, String name, String authenticatedId) throws Exception{
		Product product = productDAO.find(releaseCentreBusinessKey, extensionBusinessKey, productBusinessKey, authenticatedId);
		
		if (product == null) {
			throw new Exception ("Unable to find product with path: " + releaseCentreBusinessKey + "/" +  extensionBusinessKey + "/" + productBusinessKey);
		}
		
		Build build = new Build(name);
		product.addBuild(build);
		buildDAO.save(build);
		return build;
	}	

}

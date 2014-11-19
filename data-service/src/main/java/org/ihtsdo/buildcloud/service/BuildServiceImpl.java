package org.ihtsdo.buildcloud.service;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.hibernate.Hibernate;
import org.ihtsdo.buildcloud.dao.BuildDAO;
import org.ihtsdo.buildcloud.dao.ProductDAO;
import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.Product;
import org.ihtsdo.buildcloud.entity.User;
import org.ihtsdo.buildcloud.entity.helper.EntityHelper;
import org.ihtsdo.buildcloud.service.exception.*;
import org.ihtsdo.buildcloud.service.helper.CompositeKeyHelper;
import org.ihtsdo.buildcloud.service.helper.FilterOption;
import org.ihtsdo.buildcloud.service.security.SecurityHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Transactional
public class BuildServiceImpl extends EntityServiceImpl<Build> implements BuildService {

	private static final Logger LOGGER = LoggerFactory.getLogger(BuildServiceImpl.class);

	@Autowired
	private BuildDAO buildDAO;

	@Autowired
	private ProductDAO productDAO;

	@Autowired
	public BuildServiceImpl(BuildDAO dao) {
		super(dao);
	}

	@Override
	public List<Build> findAll(Set<FilterOption> filterOptions) throws AuthenticationException {
		return buildDAO.findAll(filterOptions, SecurityHelper.getRequiredUser());
	}

	@Override
	public Build find(String buildCompositeKey) throws BusinessServiceException {
		Long id = CompositeKeyHelper.getId(buildCompositeKey);
		if (id == null) {
			throw new ResourceNotFoundException("Unable to find build: " + buildCompositeKey);
		}
		return buildDAO.find(id, SecurityHelper.getRequiredUser());
	}

	@Override
	public Build find(String releaseCenterBusinessKey, String extensionBusinessKey, String productBusinessKey,
			String buildBusinessKey) throws ResourceNotFoundException {
		Build build = buildDAO.find(releaseCenterBusinessKey, extensionBusinessKey, productBusinessKey, buildBusinessKey, SecurityHelper.getRequiredUser());
		if (build == null) {
			throw new ResourceNotFoundException("Unable to find build: " + buildBusinessKey);
		}
		return build;
	}

	@Override
	public List<Build> findForExtension(String releaseCenterBusinessKey, String extensionBusinessKey, Set<FilterOption> filterOptions) throws AuthenticationException {
		List<Build> builds = buildDAO.findAll(releaseCenterBusinessKey, extensionBusinessKey, filterOptions, SecurityHelper.getRequiredUser());
		Hibernate.initialize(builds);
		return builds;
	}

	@Override
	public List<Build> findForProduct(String releaseCenterBusinessKey, String extensionBusinessKey, String productBusinessKey) throws ResourceNotFoundException {
		Product product = productDAO.find(releaseCenterBusinessKey, extensionBusinessKey, productBusinessKey, SecurityHelper.getRequiredUser());

		if (product == null) {
			String item = CompositeKeyHelper.getPath(releaseCenterBusinessKey, extensionBusinessKey, productBusinessKey);
			throw new ResourceNotFoundException("Unable to find product: " + item);
		}
		List<Build> builds = product.getBuilds();
		Hibernate.initialize(builds);
		return builds;
	}

	@Override
	public Build create(String releaseCenterBusinessKey, String extensionBusinessKey, String productBusinessKey, String name) throws BusinessServiceException {
		User user = SecurityHelper.getRequiredUser();
		LOGGER.info("create build, releaseCenterBusinessKey: {}, extensionBusinessKey: {}", releaseCenterBusinessKey, extensionBusinessKey);
	    Product product = productDAO.find(releaseCenterBusinessKey, extensionBusinessKey, productBusinessKey, user);

		if (product == null) {
			String item = CompositeKeyHelper.getPath(releaseCenterBusinessKey, extensionBusinessKey, productBusinessKey);
			throw new ResourceNotFoundException("Unable to find product: " + item);
		}

		//Check that we don't already have one of these
		String buildBusinessKey = EntityHelper.formatAsBusinessKey(name);
		Build existingBuild = buildDAO.find(releaseCenterBusinessKey, extensionBusinessKey, productBusinessKey, buildBusinessKey, user);
		if (existingBuild != null) {
			throw new EntityAlreadyExistsException(name + " already exists.");
		}


		Build build = new Build(name);
		product.addBuild(build);
		buildDAO.save(build);
		return build;
	}

	@Override
	public Build update(String buildCompositeKey, Map<String, String> newPropertyValues) throws BusinessServiceException {
		LOGGER.info("update build, newPropertyValues: {}", newPropertyValues);
		Build build = find(buildCompositeKey);

		if (build == null) {
			throw new ResourceNotFoundException("Unable to find build: " + buildCompositeKey);
		}
		if (newPropertyValues.containsKey(EFFECTIVE_TIME)) {
			try {
				Date date = DateFormatUtils.ISO_DATE_FORMAT.parse(newPropertyValues.get(EFFECTIVE_TIME));
				build.setEffectiveTime(date);
			} catch (ParseException e) {
				throw new BadRequestException("Invalid " + EFFECTIVE_TIME + " format. Expecting format " + DateFormatUtils.ISO_DATE_FORMAT.getPattern() + ".", e);
			}
		}
		buildDAO.update(build);
		return build;
	}

}

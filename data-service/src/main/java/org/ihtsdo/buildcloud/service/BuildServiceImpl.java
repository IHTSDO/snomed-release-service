package org.ihtsdo.buildcloud.service;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.hibernate.Hibernate;
import org.ihtsdo.buildcloud.dao.BuildDAO;
import org.ihtsdo.buildcloud.dao.ProductDAO;
import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.Product;
import org.ihtsdo.buildcloud.entity.User;
import org.ihtsdo.buildcloud.service.exception.BadRequestException;
import org.ihtsdo.buildcloud.service.helper.CompositeKeyHelper;
import org.ihtsdo.buildcloud.service.helper.FilterOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class BuildServiceImpl extends EntityServiceImpl<Build> implements BuildService {

	@Autowired
	private BuildDAO buildDAO;
	@Autowired
	private ProductDAO productDAO;

	private static final String FIRST_TIME_RELEASE = "firstTimeRelease";
	private static final String EFFECTIVE_DATE = "effectiveDate";
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd");

	private static final Logger LOGGER = LoggerFactory.getLogger(BuildServiceImpl.class);

	@Autowired
	public BuildServiceImpl(BuildDAO dao) {
		super(dao);
	}

	@Override
	public List<Build> findAll(EnumSet<FilterOption> filterOptions, User authenticatedUser) {
		return buildDAO.findAll(filterOptions, authenticatedUser);
	}

	@Override
	public Build find(String buildCompositeKey, User authenticatedUser) {
		Long id = CompositeKeyHelper.getId(buildCompositeKey);
		return buildDAO.find(id, authenticatedUser);
	}
	
	@Override
	public List<Build> findForProduct(String releaseCenterBusinessKey, String extensionBusinessKey, String productBusinessKey, User authenticatedUser) {
		List<Build> builds = productDAO.find(releaseCenterBusinessKey, extensionBusinessKey, productBusinessKey, authenticatedUser).getBuilds();
		Hibernate.initialize(builds);
		return builds;
	}
	
	@Override
	public List<Build> findForExtension(String releaseCenterBusinessKey, String extensionBusinessKey, EnumSet<FilterOption> filterOptions, User authenticatedUser) {
		List<Build> builds = buildDAO.findAll(releaseCenterBusinessKey, extensionBusinessKey,  filterOptions, authenticatedUser);
		Hibernate.initialize(builds);
		return builds;
	}

	@Override
	public Build create(String releaseCenterBusinessKey, String extensionBusinessKey, String productBusinessKey, String name, User authenticatedUser) throws Exception{
		Product product = productDAO.find(releaseCenterBusinessKey, extensionBusinessKey, productBusinessKey, authenticatedUser);
		
		if (product == null) {
			throw new Exception ("Unable to find product with path: " + releaseCenterBusinessKey + "/" +  extensionBusinessKey + "/" + productBusinessKey);
		}
		
		Build build = new Build(name);
		product.addBuild(build);
		buildDAO.save(build);
		return build;
	}

	@Override
	public Build update(String buildCompositeKey, Map<String, String> newPropertyValues, User authenticatedUser) throws BadRequestException {
		LOGGER.debug("update, newPropertyValues: {}", newPropertyValues);
		Build build = find(buildCompositeKey, authenticatedUser);
		if (newPropertyValues.containsKey(FIRST_TIME_RELEASE)) {
			build.setFirstTimeRelease("true".equals(newPropertyValues.get(FIRST_TIME_RELEASE)));
		}
		if (newPropertyValues.containsKey(EFFECTIVE_DATE)) {
			try {
				Date date = DateFormatUtils.ISO_DATE_FORMAT.parse(newPropertyValues.get(EFFECTIVE_DATE));
				build.setEffectiveDate(date);
			} catch (ParseException e) {
				throw new BadRequestException("Invalid " + EFFECTIVE_DATE + " format.", e);
			}
		}
		buildDAO.update(build);
		return build;
	}
}

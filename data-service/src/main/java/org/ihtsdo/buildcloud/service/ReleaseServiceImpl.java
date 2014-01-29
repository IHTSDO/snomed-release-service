package org.ihtsdo.buildcloud.service;

import org.hibernate.Hibernate;
import org.ihtsdo.buildcloud.dao.ProductDAO;
import org.ihtsdo.buildcloud.dao.ReleaseDAO;
import org.ihtsdo.buildcloud.entity.Release;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@Transactional
public class ReleaseServiceImpl implements ReleaseService {

	@Autowired
	private ReleaseDAO releaseDAO;

	@Autowired
	private ProductDAO productDAO;

	@Override
	public Set<Release> findAll(String releaseCentreBusinessKey, String extensionBusinessKey, String productBusinessKey, String authenticatedId) {
		Set<Release> releases = productDAO.find(releaseCentreBusinessKey, extensionBusinessKey, productBusinessKey, authenticatedId).getReleases();
		Hibernate.initialize(releases);
		return releases;
	}

	@Override
	public Release find(String releaseCentreBusinessKey, String extensionBusinessKey, String productBusinessKey, String releaseBusinessKey, String authenticatedId) {
		return releaseDAO.find(releaseCentreBusinessKey, extensionBusinessKey, productBusinessKey, releaseBusinessKey, authenticatedId);
	}

}

package org.ihtsdo.buildcloud.service;

import org.ihtsdo.buildcloud.dao.ReleaseCentreDAO;
import org.ihtsdo.buildcloud.dao.ReleaseCentreMembershipDAO;
import org.ihtsdo.buildcloud.dao.UserDAO;
import org.ihtsdo.buildcloud.entity.ReleaseCentre;
import org.ihtsdo.buildcloud.entity.ReleaseCentreMembership;
import org.ihtsdo.buildcloud.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ReleaseCentreServiceImpl extends EntityServiceImpl<ReleaseCentre> implements ReleaseCentreService {

	@Autowired
	private ReleaseCentreDAO dao;

	@Autowired
	private ReleaseCentreMembershipDAO membershipDAO;

	@Autowired
	private UserDAO userDAO;

	@Autowired
	public ReleaseCentreServiceImpl(ReleaseCentreDAO dao) {
		super(dao);
	}

	@Override
	public List<ReleaseCentre> findAll(String oauthId) {
		return dao.findAll(oauthId);
	}

	@Override
	public ReleaseCentre find(String businessKey, String oauthId) {
		return dao.find(businessKey, oauthId);
	}

	@Override
	public ReleaseCentre create(String name, String shortName, String oauthId) {
		ReleaseCentre releaseCentre = new ReleaseCentre(name, shortName);
		dao.save(releaseCentre);

		User user = userDAO.find(oauthId);
		ReleaseCentreMembership releaseCentreMembership = new ReleaseCentreMembership(releaseCentre, user);
		membershipDAO.save(releaseCentreMembership);

		return releaseCentre;
	}

}

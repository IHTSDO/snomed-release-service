package org.ihtsdo.buildcloud.service;

import org.ihtsdo.buildcloud.dao.ReleaseCenterDAO;
import org.ihtsdo.buildcloud.dao.ReleaseCenterMembershipDAO;
import org.ihtsdo.buildcloud.dao.UserDAO;
import org.ihtsdo.buildcloud.entity.ReleaseCenter;
import org.ihtsdo.buildcloud.entity.ReleaseCenterMembership;
import org.ihtsdo.buildcloud.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ReleaseCenterServiceImpl extends EntityServiceImpl<ReleaseCenter> implements ReleaseCenterService {

	@Autowired
	private ReleaseCenterDAO dao;

	@Autowired
	private ReleaseCenterMembershipDAO membershipDAO;

	@Autowired
	private UserDAO userDAO;

	@Autowired
	public ReleaseCenterServiceImpl(ReleaseCenterDAO dao) {
		super(dao);
	}

	@Override
	public List<ReleaseCenter> findAll(User authenticatedUser) {
		return dao.findAll(authenticatedUser);
	}

	@Override
	public ReleaseCenter find(String businessKey, User authenticatedUser) {
		return dao.find(businessKey, authenticatedUser);
	}

	@Override
	public ReleaseCenter create(String name, String shortName, User user) {
		ReleaseCenter releaseCenter = new ReleaseCenter(name, shortName);
		dao.save(releaseCenter);

		ReleaseCenterMembership releaseCenterMembership = new ReleaseCenterMembership(releaseCenter, user);
		membershipDAO.save(releaseCenterMembership);

		return releaseCenter;
	}

}

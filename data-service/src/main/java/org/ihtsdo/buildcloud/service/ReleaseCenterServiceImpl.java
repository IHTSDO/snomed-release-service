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
	public List<ReleaseCenter> findAll(String oauthId) {
		return dao.findAll(oauthId);
	}

	@Override
	public ReleaseCenter find(String businessKey, String oauthId) {
		return dao.find(businessKey, oauthId);
	}

	@Override
	public ReleaseCenter create(String name, String shortName, String oauthId) {
		ReleaseCenter releaseCenter = new ReleaseCenter(name, shortName);
		dao.save(releaseCenter);

		User user = userDAO.find(oauthId);
		ReleaseCenterMembership releaseCenterMembership = new ReleaseCenterMembership(releaseCenter, user);
		membershipDAO.save(releaseCenterMembership);

		return releaseCenter;
	}

}

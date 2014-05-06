package org.ihtsdo.buildcloud.dao;

import org.ihtsdo.buildcloud.entity.ReleaseCenterMembership;
import org.springframework.stereotype.Repository;

@Repository
public class ReleaseCenterMembershipDAOImpl extends EntityDAOImpl<ReleaseCenterMembership> implements ReleaseCenterMembershipDAO {

	protected ReleaseCenterMembershipDAOImpl() {
		super(ReleaseCenterMembership.class);
	}

}

package org.ihtsdo.buildcloud.dao;

import org.ihtsdo.buildcloud.entity.AuthToken;

public class AuthTokenDAOImpl extends EntityDAOImpl<AuthToken> implements AuthTokenDAO {

	protected AuthTokenDAOImpl() {
		super(AuthToken.class);
	}

}

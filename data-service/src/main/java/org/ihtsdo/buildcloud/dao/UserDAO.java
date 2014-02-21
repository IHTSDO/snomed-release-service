package org.ihtsdo.buildcloud.dao;

import org.ihtsdo.buildcloud.entity.User;

public interface UserDAO {

	User find(String authenticatedId);

}

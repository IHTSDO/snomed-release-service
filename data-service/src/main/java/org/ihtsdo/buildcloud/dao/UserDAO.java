package org.ihtsdo.buildcloud.dao;

import org.ihtsdo.buildcloud.entity.User;

public interface UserDAO extends EntityDAO<User> {

	User find(String authenticatedId);

}

package org.ihtsdo.buildcloud.dao;

import org.hibernate.Query;
import org.ihtsdo.buildcloud.entity.User;
import org.springframework.stereotype.Repository;

@Repository
public class UserDAOImpl extends EntityDAOImpl<User> implements UserDAO {

	protected UserDAOImpl() {
		super(User.class);
	}

	@Override
	public User find(String username) {
		Query query = getCurrentSession().createQuery("from User where username = :username");
		query.setString("username", username);
		return (User) query.uniqueResult();
	}

}

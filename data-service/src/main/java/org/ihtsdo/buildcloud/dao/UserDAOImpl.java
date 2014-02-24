package org.ihtsdo.buildcloud.dao;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.ihtsdo.buildcloud.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class UserDAOImpl extends EntityDAOImpl<User> implements UserDAO {

	@Override
	public User find(String authenticatedId) {
		Query query = getCurrentSession().createQuery("from User where oauthId = :oauthId");
		query.setParameter("oauthId", authenticatedId);
		return (User) query.uniqueResult();
	}

}

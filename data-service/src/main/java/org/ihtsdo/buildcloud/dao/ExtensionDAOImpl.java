package org.ihtsdo.buildcloud.dao;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.ihtsdo.buildcloud.entity.Extension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class ExtensionDAOImpl implements ExtensionDAO {

	@Autowired
	private SessionFactory sessionFactory;

	@Override
	public Extension find(String releaseCentreBusinessKey, String extensionBusinessKey, String authenticatedId) {
		Query query = getCurrentSession().createQuery(
				"select extension " +
				"from ReleaseCentreMembership membership " +
				"join membership.releaseCentre releaseCentre " +
				"join releaseCentre.extensions extension " +
				"where membership.user.oauthId = :oauthId " +
				"and releaseCentre.businessKey = :releaseCentreBusinessKey " +
				"and extension.businessKey = :extensionBusinessKey " +
				"order by extension.id ");
		query.setString("oauthId", authenticatedId);
		query.setString("releaseCentreBusinessKey", releaseCentreBusinessKey);
		query.setString("extensionBusinessKey", extensionBusinessKey);
		return (Extension) query.uniqueResult();

	}

	private Session getCurrentSession() {
		return sessionFactory.getCurrentSession();
	}

}

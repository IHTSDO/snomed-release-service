package org.ihtsdo.buildcloud.dao;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.ihtsdo.buildcloud.entity.Build;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class BuildDAOImpl implements BuildDAO {

	@Autowired
	private SessionFactory sessionFactory;

	@Override
	public Build find(String releaseCentreBusinessKey, String extensionBusinessKey, String productBusinessKey,
						String buildBusinessKey, String authenticatedId) {
		Query query = getCurrentSession().createQuery(
				"select build " +
				"from ReleaseCentreMembership membership " +
				"join membership.releaseCentre releaseCentre " +
				"join releaseCentre.extensions extension " +
				"join extension.products product " +
				"join product.builds build " +
				"where membership.user.oauthId = :oauthId " +
				"and releaseCentre.businessKey = :releaseCentreBusinessKey " +
				"and extension.businessKey = :extensionBusinessKey " +
				"and product.businessKey = :productBusinessKey " +
				"and build.businessKey = :buildBusinessKey ");
		query.setString("oauthId", authenticatedId);
		query.setString("releaseCentreBusinessKey", releaseCentreBusinessKey);
		query.setString("extensionBusinessKey", extensionBusinessKey);
		query.setString("productBusinessKey", productBusinessKey);
		query.setString("buildBusinessKey", buildBusinessKey);
		return (Build) query.uniqueResult();

	}

	private Session getCurrentSession() {
		return sessionFactory.getCurrentSession();
	}
}

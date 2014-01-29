package org.ihtsdo.buildcloud.dao;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.ihtsdo.buildcloud.entity.Package;
import org.ihtsdo.buildcloud.entity.Release;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class ReleaseDAOImpl implements ReleaseDAO {

	@Autowired
	private SessionFactory sessionFactory;

	@Override
	public Release find(String releaseCentreBusinessKey, String extensionBusinessKey, String productBusinessKey,
						String releaseBusinessKey, String authenticatedId) {
		Query query = getCurrentSession().createQuery(
				"select release " +
				"from ReleaseCentreMembership membership " +
				"join membership.releaseCentre releaseCentre " +
				"join releaseCentre.extensions extension " +
				"join extension.products product " +
				"join product.releases release " +
				"where membership.user.oauthId = :oauthId " +
				"and releaseCentre.businessKey = :releaseCentreBusinessKey " +
				"and extension.businessKey = :extensionBusinessKey " +
				"and product.businessKey = :productBusinessKey " +
				"and release.businessKey = :releaseBusinessKey ");
		query.setString("oauthId", authenticatedId);
		query.setString("releaseCentreBusinessKey", releaseCentreBusinessKey);
		query.setString("extensionBusinessKey", extensionBusinessKey);
		query.setString("productBusinessKey", productBusinessKey);
		query.setString("releaseBusinessKey", releaseBusinessKey);
		return (Release) query.uniqueResult();

	}

	private Session getCurrentSession() {
		return sessionFactory.getCurrentSession();
	}
}

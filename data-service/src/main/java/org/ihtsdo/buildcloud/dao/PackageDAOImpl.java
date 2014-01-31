package org.ihtsdo.buildcloud.dao;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.ihtsdo.buildcloud.entity.Package;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class PackageDAOImpl implements PackageDAO {

	@Autowired
	private SessionFactory sessionFactory;

	@Override
	public Package find(String releaseCentreBusinessKey, String extensionBusinessKey, String productBusinessKey,
						String buildBusinessKey, String packageBusinessKey, String authenticatedId) {
		Query query = getCurrentSession().createQuery(
				"select package " +
				"from ReleaseCentreMembership membership " +
				"join membership.releaseCentre releaseCentre " +
				"join releaseCentre.extensions extension " +
				"join extension.products product " +
				"join product.builds build " +
				"join build.packages package " +
				"where membership.user.oauthId = :oauthId " +
				"and releaseCentre.businessKey = :releaseCentreBusinessKey " +
				"and extension.businessKey = :extensionBusinessKey " +
				"and product.businessKey = :productBusinessKey " +
				"and build.businessKey = :buildBusinessKey " +
				"and package.businessKey = :packageBusinessKey ");
		query.setString("oauthId", authenticatedId);
		query.setString("releaseCentreBusinessKey", releaseCentreBusinessKey);
		query.setString("extensionBusinessKey", extensionBusinessKey);
		query.setString("productBusinessKey", productBusinessKey);
		query.setString("buildBusinessKey", buildBusinessKey);
		query.setString("packageBusinessKey", packageBusinessKey);

		return (Package) query.uniqueResult();
	}

	private Session getCurrentSession() {
		return sessionFactory.getCurrentSession();
	}

}

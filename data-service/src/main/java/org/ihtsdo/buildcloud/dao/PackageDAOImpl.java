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
	public Package find(Long buildId, String packageBusinessKey, String authenticatedId) {
		Query query = getCurrentSession().createQuery(
				"select package " +
				"from ReleaseCentreMembership membership " +
				"join membership.releaseCentre releaseCentre " +
				"join releaseCentre.extensions extension " +
				"join extension.products product " +
				"join product.builds build " +
				"join build.packages package " +
				"where membership.user.oauthId = :oauthId " +
				"and build.id = :buildId " +
				"and package.businessKey = :packageBusinessKey " +
				"order by package.id ");
		query.setString("oauthId", authenticatedId);
		query.setLong("buildId", buildId);
		query.setString("packageBusinessKey", packageBusinessKey);

		return (Package) query.uniqueResult();
	}

	private Session getCurrentSession() {
		return sessionFactory.getCurrentSession();
	}
}

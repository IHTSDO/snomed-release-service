package org.ihtsdo.buildcloud.dao;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.ihtsdo.buildcloud.entity.Package;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class PackageDAOImpl extends EntityDAOImpl<Package> implements PackageDAO {

	@Override
	public Package find(Long buildId, String packageBusinessKey, String authenticatedId) {
		Query query = getCurrentSession().createQuery(
				"select package " +
				"from ReleaseCenterMembership membership " +
				"join membership.releaseCenter releaseCenter " +
				"join releaseCenter.extensions extension " +
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

}

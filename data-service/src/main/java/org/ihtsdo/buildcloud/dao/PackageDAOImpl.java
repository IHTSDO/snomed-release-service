package org.ihtsdo.buildcloud.dao;

import org.hibernate.Query;
import org.ihtsdo.buildcloud.entity.Package;
import org.ihtsdo.buildcloud.entity.User;
import org.springframework.stereotype.Repository;

@Repository
public class PackageDAOImpl extends EntityDAOImpl<Package> implements PackageDAO {

	protected PackageDAOImpl() {
		super(Package.class);
	}

	@Override
	public Package find(Long buildId, String packageBusinessKey, User user) {
		Query query = getCurrentSession().createQuery(
				"select package " +
						"from ReleaseCenterMembership membership " +
						"join membership.releaseCenter releaseCenter " +
						"join releaseCenter.extensions extension " +
						"join extension.products product " +
						"join product.builds build " +
						"join build.packages package " +
						"where membership.user = :user " +
						"and build.id = :buildId " +
						"and package.businessKey = :packageBusinessKey " +
						"order by package.id ");
		query.setEntity("user", user);
		query.setLong("buildId", buildId);
		query.setString("packageBusinessKey", packageBusinessKey);

		return (Package) query.uniqueResult();
	}

}

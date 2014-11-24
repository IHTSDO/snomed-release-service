package org.ihtsdo.buildcloud.dao;

import org.hibernate.Query;
import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.User;
import org.ihtsdo.buildcloud.service.helper.FilterOption;
import org.springframework.stereotype.Repository;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Set;

@Repository
public class BuildDAOImpl extends EntityDAOImpl<Build> implements BuildDAO {

	public BuildDAOImpl() {
		super(Build.class);
	}

	@Override
	public List<Build> findAll(Set<FilterOption> filterOptions, User user) {
		return findAll(null, filterOptions, user);
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Build> findAll(String releaseCenterBusinessKey, Set<FilterOption> filterOptions, User user) {

		String filter = "";
		if (filterOptions != null && filterOptions.contains(FilterOption.INCLUDE_REMOVED)) {
			filter += " and ( removed = 'N' or removed is null) ";
		}
		if (releaseCenterBusinessKey != null) {
			filter += " and releaseCenter.businessKey = :releaseCenterBusinessKey ";
		}
		Query query = getCurrentSession().createQuery(
				"select build " +
						"from ReleaseCenterMembership membership " +
						"join membership.releaseCenter releaseCenter " +
						"join releaseCenter.builds build " +
						"where membership.user = :user " +
						filter +
						"order by build.id ");
		query.setEntity("user", user);

		if (releaseCenterBusinessKey != null) {
			query.setString("releaseCenterBusinessKey", releaseCenterBusinessKey);
		}
		return query.list();
	}

	@Override
	public Build find(Long id, User user) {
		Query query = getCurrentSession().createQuery(
				"select build " +
						"from ReleaseCenterMembership membership " +
						"join membership.releaseCenter releaseCenter " +
						"join releaseCenter.builds build " +
						"where membership.user = :user " +
						"and build.id = :buildId " +
						"order by build.id ");
		query.setEntity("user", user);
		query.setLong("buildId", id);
		return (Build) query.uniqueResult();
	}

	/**
	 * Look for a specific build where the id (primary key) is not necessarily known
	 */
	@Override
	public Build find(String releaseCenterKey,
			String buildKey, User user) {
		Query query = getCurrentSession().createQuery(
				"select build " +
						"from ReleaseCenterMembership membership " +
						"join membership.releaseCenter releaseCenter " +
						"join releaseCenter.builds build " +
						"where membership.user = :user " +
						"and releaseCenter.businessKey = :releaseCenterBusinessKey " +
						"and build.businessKey = :buildBusinessKey ");
		query.setEntity("user", user);
		query.setString("releaseCenterBusinessKey", releaseCenterKey);
		query.setString("buildBusinessKey", buildKey);
		return (Build) query.uniqueResult();
	}

}

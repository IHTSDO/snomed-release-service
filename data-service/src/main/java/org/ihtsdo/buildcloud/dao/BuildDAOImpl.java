package org.ihtsdo.buildcloud.dao;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.service.helper.FilterOption;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.EnumSet;
import java.util.List;

@Repository
public class BuildDAOImpl extends EntityDAOImpl<Build> implements BuildDAO {

	@Override
	public List<Build> findAll(EnumSet<FilterOption> filterOptions, String authenticatedId) {
		return findAll(null, null, filterOptions, authenticatedId);
	}
	
	@Override
	public List<Build> findAll(String releaseCentreBusinessKey, String extensionBusinessKey, EnumSet<FilterOption> filterOptions, String authenticatedId) {
		
		String filter = "";
		if (filterOptions.contains(FilterOption.INCLUDE_REMOVED)) {
			filter += " and ( removed = 'N' or removed is null) ";
		}
		if (filterOptions.contains(FilterOption.STARRED_ONLY)) {
			filter += " and starred = true ";
		}
		if (releaseCentreBusinessKey != null) {
			filter += " and releaseCentre.businessKey = :releaseCentreBusinessKey ";
		}
		if (extensionBusinessKey != null) {
			//TODO Watch here that extension business key is not guaranteed unique, so 
			//potentially we want to only allow ext if rc also specified.
			filter += " and extension.businessKey = :extensionBusinessKey ";
		}		
		Query query = getCurrentSession().createQuery(
				"select build " +
				"from ReleaseCentreMembership membership " +
				"join membership.releaseCentre releaseCentre " +
				"join releaseCentre.extensions extension " +
				"join extension.products product " +
				"join product.builds build " +
				"where membership.user.oauthId = :oauthId " +
				filter +  
				"order by build.id ");
		query.setString("oauthId", authenticatedId);
		
		if (releaseCentreBusinessKey != null) {
			query.setString("releaseCentreBusinessKey", releaseCentreBusinessKey);
		}
		if (extensionBusinessKey != null){
			query.setString("extensionBusinessKey", extensionBusinessKey);
		}
		return query.list();
	}

	@Override
	public Build find(Long id, String authenticatedId) {
		Query query = getCurrentSession().createQuery(
				"select build " +
				"from ReleaseCentreMembership membership " +
				"join membership.releaseCentre releaseCentre " +
				"join releaseCentre.extensions extension " +
				"join extension.products product " +
				"join product.builds build " +
				"where membership.user.oauthId = :oauthId " +
				"and build.id = :buildId " +
				"order by build.id ");
		query.setString("oauthId", authenticatedId);
		query.setLong("buildId", id);
		return (Build) query.uniqueResult();
	}

}

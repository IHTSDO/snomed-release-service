package org.ihtsdo.buildcloud.dao;

import org.hibernate.Query;
import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.User;
import org.ihtsdo.buildcloud.service.helper.FilterOption;
import org.springframework.stereotype.Repository;

import java.util.EnumSet;
import java.util.List;

@Repository
public class BuildDAOImpl extends EntityDAOImpl<Build> implements BuildDAO {

	public BuildDAOImpl() {
		super(Build.class);
	}

	@Override
	public List<Build> findAll(EnumSet<FilterOption> filterOptions, User user) {
		return findAll(null, null, filterOptions, user);
	}
	
	@Override
	public List<Build> findAll(String releaseCenterBusinessKey, String extensionBusinessKey, EnumSet<FilterOption> filterOptions, User user) {
		
		/*List<Build> testList = new Vector<Build>();
		Build b = new Build("Peter");
		testList.add(b);
		return testList;*/
		
		String filter = "";
		if (filterOptions.contains(FilterOption.INCLUDE_REMOVED)) {
			filter += " and ( removed = 'N' or removed is null) ";
		}
		if (filterOptions.contains(FilterOption.STARRED_ONLY)) {
			filter += " and starred = true ";
		}
		if (releaseCenterBusinessKey != null) {
			filter += " and releaseCenter.businessKey = :releaseCenterBusinessKey ";
		}
		if (extensionBusinessKey != null) {
			//TODO Watch here that extension business key is not guaranteed unique, so 
			//potentially we want to only allow ext if rc also specified.
			filter += " and extension.businessKey = :extensionBusinessKey ";
		}		
		Query query = getCurrentSession().createQuery(
				"select build " +
				"from ReleaseCenterMembership membership " +
				"join membership.releaseCenter releaseCenter " +
				"join releaseCenter.extensions extension " +
				"join extension.products product " +
				"join product.builds build " +
				"where membership.user = :user " +
				filter +  
				"order by build.id ");
		query.setEntity("user", user);

		if (releaseCenterBusinessKey != null) {
			query.setString("releaseCenterBusinessKey", releaseCenterBusinessKey);
		}
		if (extensionBusinessKey != null){
			query.setString("extensionBusinessKey", extensionBusinessKey);
		}
		return query.list();
	}

	@Override
	public Build find(Long id, User user) {
		Query query = getCurrentSession().createQuery(
				"select build " +
				"from ReleaseCenterMembership membership " +
				"join membership.releaseCenter releaseCenter " +
				"join releaseCenter.extensions extension " +
				"join extension.products product " +
				"join product.builds build " +
				"where membership.user = :user " +
				"and build.id = :buildId " +
				"order by build.id ");
		query.setEntity("user", user);
		query.setLong("buildId", id);
		return (Build) query.uniqueResult();
	}

}

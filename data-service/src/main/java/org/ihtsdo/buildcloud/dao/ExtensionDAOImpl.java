package org.ihtsdo.buildcloud.dao;

import org.hibernate.Query;
import org.ihtsdo.buildcloud.entity.Extension;
import org.ihtsdo.buildcloud.entity.User;
import org.springframework.stereotype.Repository;

@Repository
public class ExtensionDAOImpl extends EntityDAOImpl<Extension> implements ExtensionDAO {

	public ExtensionDAOImpl() {
		super(Extension.class);
	}

	@Override
	public Extension find(String releaseCenterBusinessKey, String extensionBusinessKey, User user) {
		Query query = getCurrentSession().createQuery(
				"select extension " +
						"from ReleaseCenterMembership membership " +
						"join membership.releaseCenter releaseCenter " +
						"join releaseCenter.extensions extension " +
						"where membership.user = :user " +
						"and releaseCenter.businessKey = :releaseCenterBusinessKey " +
						"and extension.businessKey = :extensionBusinessKey " +
						"order by extension.id ");
		query.setEntity("user", user);
		query.setString("releaseCenterBusinessKey", releaseCenterBusinessKey);
		query.setString("extensionBusinessKey", extensionBusinessKey);
		return (Extension) query.uniqueResult();
	}

}

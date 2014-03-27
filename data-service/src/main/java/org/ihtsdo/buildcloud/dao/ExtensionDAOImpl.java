package org.ihtsdo.buildcloud.dao;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.ihtsdo.buildcloud.entity.Extension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class ExtensionDAOImpl extends EntityDAOImpl<Extension> implements ExtensionDAO {

	@Override
	public Extension find(String releaseCenterBusinessKey, String extensionBusinessKey, String authenticatedId) {
		Query query = getCurrentSession().createQuery(
				"select extension " +
				"from ReleaseCenterMembership membership " +
				"join membership.releaseCenter releaseCenter " +
				"join releaseCenter.extensions extension " +
				"where membership.user.oauthId = :oauthId " +
				"and releaseCenter.businessKey = :releaseCenterBusinessKey " +
				"and extension.businessKey = :extensionBusinessKey " +
				"order by extension.id ");
		query.setString("oauthId", authenticatedId);
		query.setString("releaseCenterBusinessKey", releaseCenterBusinessKey);
		query.setString("extensionBusinessKey", extensionBusinessKey);
		return (Extension) query.uniqueResult();
	}

}

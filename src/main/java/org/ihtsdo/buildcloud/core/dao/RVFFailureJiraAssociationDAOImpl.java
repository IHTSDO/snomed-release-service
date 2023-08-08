package org.ihtsdo.buildcloud.core.dao;

import org.hibernate.query.Query;
import org.ihtsdo.buildcloud.core.entity.RVFFailureJiraAssociation;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public class RVFFailureJiraAssociationDAOImpl extends EntityDAOImpl<RVFFailureJiraAssociation> implements RVFFailureJiraAssociationDAO {

	protected RVFFailureJiraAssociationDAOImpl() {
		super(RVFFailureJiraAssociation.class);
	}

	@Override
	public List<RVFFailureJiraAssociation> findByBuildKey(String centerKey, String productKey, String buildKey) {
		Query<RVFFailureJiraAssociation> query = getCurrentSession().createQuery(
				"select rvfFailureJiraAssoc " +
						"from RVFFailureJiraAssociation rvfFailureJiraAssoc join rvfFailureJiraAssoc.releaseCenter releaseCenter join rvfFailureJiraAssoc.product product " +
						"where releaseCenter.businessKey = :centerKey AND product.businessKey = :productKey AND rvfFailureJiraAssoc.buildId = :buildKey", RVFFailureJiraAssociation.class);
		query.setParameter("centerKey", centerKey);
		query.setParameter("productKey", productKey);
		query.setParameter("buildKey", buildKey);
		return query.list();
	}

	@Override
	public List<RVFFailureJiraAssociation> findByEffectiveTime(String centerKey, String productKey, Date effectiveTime) {
		Query<RVFFailureJiraAssociation> query = getCurrentSession().createQuery(
				"select rvfFailureJiraAssoc " +
						"from RVFFailureJiraAssociation rvfFailureJiraAssoc join rvfFailureJiraAssoc.releaseCenter releaseCenter join rvfFailureJiraAssoc.product product " +
						"where releaseCenter.businessKey = :centerKey AND product.businessKey = :productKey AND rvfFailureJiraAssoc.effectiveTime = :effectiveTime", RVFFailureJiraAssociation.class);
		query.setParameter("centerKey", centerKey);
		query.setParameter("productKey", productKey);
		query.setParameter("effectiveTime", effectiveTime);
		return query.list();
	}

}

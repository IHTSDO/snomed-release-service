package org.ihtsdo.buildcloud.core.dao;

import org.ihtsdo.buildcloud.core.entity.RVFFailureJiraAssociation;

import java.util.Date;
import java.util.List;

public interface RVFFailureJiraAssociationDAO extends EntityDAO<RVFFailureJiraAssociation> {
	List<RVFFailureJiraAssociation> findByBuildKey(String centerKey, String productKey, String buildKey);

	List<RVFFailureJiraAssociation> findByEffectiveTime(String centerKey, String productKey, Date effectiveTime);
}

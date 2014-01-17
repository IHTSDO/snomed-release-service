package org.ihtsdo.buildcloud.dao;

import org.ihtsdo.buildcloud.entity.Extension;
import org.ihtsdo.buildcloud.entity.ReleaseCentre;
import org.springframework.stereotype.Repository;

@Repository
public class ExtensionDAOImpl extends AbstractDAOImpl<Extension> implements ExtensionDAO {

	@Override
	protected String getEntityType() {
		return Extension.class.getName();
	}

}

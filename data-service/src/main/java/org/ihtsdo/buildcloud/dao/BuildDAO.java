package org.ihtsdo.buildcloud.dao;

import org.ihtsdo.buildcloud.entity.Build;

public interface BuildDAO {

	Build find(String releaseCentreBusinessKey, String extensionBusinessKey, String productBusinessKey,
				 String buildBusinessKey, String authenticatedId);

}

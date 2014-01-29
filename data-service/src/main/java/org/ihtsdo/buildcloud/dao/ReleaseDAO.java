package org.ihtsdo.buildcloud.dao;

import org.ihtsdo.buildcloud.entity.Release;

public interface ReleaseDAO {

	Release find(String releaseCentreBusinessKey, String extensionBusinessKey, String productBusinessKey,
				 String releaseBusinessKey, String authenticatedId);

}

package org.ihtsdo.buildcloud.dao;

import org.ihtsdo.buildcloud.entity.Package;

public interface PackageDAO {

	Package find(String releaseCentreBusinessKey, String extensionBusinessKey, String productBusinessKey,
				 String releaseBusinessKey, String packageBusinessKey, String authenticatedId);

}

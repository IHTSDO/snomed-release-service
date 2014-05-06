package org.ihtsdo.buildcloud.dao;

import org.ihtsdo.buildcloud.entity.Package;
import org.ihtsdo.buildcloud.entity.User;

public interface PackageDAO extends EntityDAO<Package> {
	Package find(Long buildId, String packageBusinessKey, User user);
}

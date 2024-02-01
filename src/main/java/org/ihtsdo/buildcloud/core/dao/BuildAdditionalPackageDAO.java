package org.ihtsdo.buildcloud.core.dao;

import org.ihtsdo.buildcloud.core.entity.BuildAdditionalPackage;

import java.util.List;

public interface BuildAdditionalPackageDAO extends EntityDAO<BuildAdditionalPackage> {
    List<BuildAdditionalPackage> findByBuildConfigId(long buildConfigId);

}

package org.ihtsdo.buildcloud.core.dao;

import org.hibernate.query.Query;
import org.ihtsdo.buildcloud.core.entity.BuildAdditionalPackage;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BuildAdditionalPackageDAOImpl extends EntityDAOImpl<BuildAdditionalPackage> implements BuildAdditionalPackageDAO {

    public BuildAdditionalPackageDAOImpl() {
        super(BuildAdditionalPackage.class);
    }

    @Override
    public List<BuildAdditionalPackage> findByBuildConfigId(long buildConfigId) {
        Query<BuildAdditionalPackage> query = getCurrentSession().createQuery(
                "select a " +
                        "from BuildAdditionalPackage a " +
                        "where a.buildConfiguration.id = :buildConfigId ", BuildAdditionalPackage.class);
        query.setParameter("buildConfigId", buildConfigId);
        return query.list();
    }
}

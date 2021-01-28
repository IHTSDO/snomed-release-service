package org.ihtsdo.buildcloud.service;

import org.ihtsdo.buildcloud.dao.ReleaseCenterDAO;
import org.ihtsdo.buildcloud.entity.ReleaseCenter;
import org.ihtsdo.buildcloud.entity.helper.EntityHelper;
import org.ihtsdo.otf.rest.exception.EntityAlreadyExistsException;
import org.ihtsdo.otf.rest.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ReleaseCenterServiceImpl extends EntityServiceImpl<ReleaseCenter> implements ReleaseCenterService {

    @Autowired
    private ReleaseCenterDAO dao;

    @Autowired
    public ReleaseCenterServiceImpl(ReleaseCenterDAO dao) {
        super(dao);
    }

    @Override
    @Cacheable("release-center-records")
    public List<ReleaseCenter> findAll() {
        return dao.findAll();
    }

    @Override
    @Cacheable(value = "release-center", key = "#businessKey")
    public ReleaseCenter find(String businessKey) throws ResourceNotFoundException {
        ReleaseCenter releaseCenter = dao.find(businessKey);
        if (releaseCenter != null) {
            return releaseCenter;
        } else {
            throw new ResourceNotFoundException("Release Center '" + businessKey + "' not found.");
        }
    }

    @Override
    @CacheEvict(value = "release-center-records", allEntries = true)
    public ReleaseCenter create(String name, String shortName, String codeSystem) throws EntityAlreadyExistsException {
        //Check that we don't already have one of these
        String releaseCenterBusinessKey = EntityHelper.formatAsBusinessKey(shortName);
        ReleaseCenter existingRC = dao.find(releaseCenterBusinessKey);
        if (existingRC != null) {
            throw new EntityAlreadyExistsException(name + " already exists.");
        }

        ReleaseCenter releaseCenter = new ReleaseCenter(name, shortName);
        dao.save(releaseCenter);

        return releaseCenter;
    }

    @Override
    @Caching(evict = {
            @CacheEvict("release-center-records"),
            @CacheEvict(value = "release-center", key = "#entity.businessKey")
    })
    public void update(ReleaseCenter entity) {
        super.update(entity);
    }
}

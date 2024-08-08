package org.ihtsdo.buildcloud.core.service;

import org.snomed.module.storage.ModuleMetadata;
import org.snomed.module.storage.ModuleStorageCoordinator;
import org.snomed.module.storage.ModuleStorageCoordinatorException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ModuleStorageCoordinatorCache {

    @Autowired
    private ModuleStorageCoordinator moduleStorageCoordinator;

    @Cacheable(value = "published-releases")
    public Map<String, List<ModuleMetadata>> getAllReleases() throws ModuleStorageCoordinatorException.OperationFailedException, ModuleStorageCoordinatorException.ResourceNotFoundException, ModuleStorageCoordinatorException.InvalidArgumentsException {
        return moduleStorageCoordinator.getAllReleases();
    }

    @CacheEvict(value = "published-releases", allEntries = true)
    public void clearCachedReleases() {
        // do nothing
    }
}

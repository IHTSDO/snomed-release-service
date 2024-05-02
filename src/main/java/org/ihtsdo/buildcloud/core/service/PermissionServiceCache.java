package org.ihtsdo.buildcloud.core.service;

import org.ihtsdo.otf.rest.client.RestClientException;
import org.ihtsdo.otf.rest.client.terminologyserver.pojo.Branch;
import org.ihtsdo.otf.rest.client.terminologyserver.pojo.CodeSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PermissionServiceCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionServiceCache.class);

    public static final String BRANCH_ROOT = "MAIN";

    @Autowired
    private TermServerService termServerService;

    @Cacheable(value = "global-roles", key = "#token")
    public Set<String> getGlobalRoles(String token) {
        try {
            Branch branch = termServerService.getBranch(BRANCH_ROOT);
            if (branch != null) {
                return branch.getGlobalUserRoles();
            }
        } catch (RestClientException e) {
            LOGGER.debug("Error while retrieving MAIN. Message: " + e.getMessage());
            return Collections.emptySet();
        }
        return Collections.emptySet();
    }

    @Cacheable(value = "code-system-roles", key = "#token")
    public Map<String, Set<String>> getCodeSystemRoles(String token) {
        List<CodeSystem> codeSystems = termServerService.getCodeSystems();
        if (!CollectionUtils.isEmpty(codeSystems)) {
            return codeSystems.stream().collect(Collectors.toMap(CodeSystem::getShortName, CodeSystem::getUserRoles));
        }

        return Collections.emptyMap();
    }
}

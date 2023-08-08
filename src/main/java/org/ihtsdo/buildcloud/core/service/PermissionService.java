package org.ihtsdo.buildcloud.core.service;

import org.ihtsdo.buildcloud.core.entity.ReleaseCenter;
import org.ihtsdo.sso.integration.SecurityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PermissionService {

    private final Logger logger = LoggerFactory.getLogger(PermissionService.class);

    public static final String GLOBAL_ROLE = "GLOBAL";
    public static final String USER_ROLE = "USER";
    public static final String GLOBAL_ROLE_SCOPE = "global";

    public enum Role {
        RELEASE_ADMIN, RELEASE_MANAGER, RELEASE_LEAD, RELEASE_USER, AUTHOR
    }

    @Autowired
    private PermissionServiceCache permissionServiceCache;

    @Autowired
    private ReleaseCenterService releaseCenterService;

    public boolean userHasRoleOnReleaseCenter(String role, String releaseCenterKey) {
        boolean contains = false;
        if (releaseCenterKey.equalsIgnoreCase(GLOBAL_ROLE_SCOPE)) {
            Set <String> globalRoles = permissionServiceCache.getGlobalRoles(SecurityUtil.getAuthenticationToken());
            contains = globalRoles.contains(role);
        } else {
            ReleaseCenter releaseCenter = releaseCenterService.find(releaseCenterKey);
            Map <String, Set <String>> codeSystemToRolesMap = permissionServiceCache.getCodeSystemRoles(SecurityUtil.getAuthenticationToken());
            if (StringUtils.hasLength(releaseCenter.getCodeSystem()) && codeSystemToRolesMap.containsKey(releaseCenter.getCodeSystem())) {
                Set <String> roles = codeSystemToRolesMap.get(releaseCenter.getCodeSystem());
                if (USER_ROLE.equalsIgnoreCase(role)) {
                    contains = roles.contains(Role.RELEASE_USER.name()) || roles.contains(Role.AUTHOR.name());
                } else {
                    contains = roles.contains(role);
                }
            }
        }

        if (!contains) {
            logger.info("User '{}' does not have required role '{}' on release center '{}'.", SecurityUtil.getUsername(), role, releaseCenterKey);
        }
        return contains;
    }

    public Map<String, Set<String>> getRolesForLoggedInUser() {
        Map<String, Set<String>> rolesMap = new HashMap<>();
        Set<String> globalRoles = permissionServiceCache.getGlobalRoles(SecurityUtil.getAuthenticationToken());
        Map<String, Set<String>> codeSystemToRolesMap = permissionServiceCache.getCodeSystemRoles(SecurityUtil.getAuthenticationToken());
        if (!globalRoles.isEmpty()) {
            globalRoles = globalRoles.stream()
                    .filter(line -> Role.RELEASE_ADMIN.name().equals(line)
                            || Role.RELEASE_MANAGER.name().equals(line)
                            || Role.RELEASE_LEAD.name().equals(line))
                    .collect(Collectors.toSet());
        }
        rolesMap.put(GLOBAL_ROLE, globalRoles);

        if (!codeSystemToRolesMap.isEmpty()) {
            codeSystemToRolesMap.forEach((codeSystem, roles) -> {
                if (!roles.isEmpty()) {
                    roles = roles.stream()
                            .filter(line -> Role.RELEASE_ADMIN.name().equals(line)
                                    || Role.RELEASE_MANAGER.name().equals(line)
                                    || Role.RELEASE_LEAD.name().equals(line)
                                    || Role.RELEASE_USER.name().equals(line)
                                    || Role.AUTHOR.name().equals(line))
                            .collect(Collectors.toSet());
                }
                rolesMap.put(codeSystem, roles);
            });
        }
        return rolesMap;
    }
}

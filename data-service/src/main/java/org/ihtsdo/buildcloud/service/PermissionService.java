package org.ihtsdo.buildcloud.service;

import org.ihtsdo.buildcloud.entity.ReleaseCenter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Service
public class PermissionService {

    private final Logger logger = LoggerFactory.getLogger(PermissionService.class);

    public final static String GLOBAL_SUFFIX = "_GLOBAL";

    public static final String GLOBAL_ROLE_SCOPE = "global";

    public enum Role {
        ADMIN, RELEASE_MANAGER, USER
    }

    @Autowired
    private PermissionServiceCache permissionServiceCache;

    @Autowired
    private ReleaseCenterService releaseCenterService;

    public boolean userHasRoleOnReleaseCenter(String role, String releaseCenterKey, Authentication authentication) {
        boolean contains = false;
        if (releaseCenterKey.equalsIgnoreCase(GLOBAL_ROLE_SCOPE)) {
            Set<String> globalRoles = permissionServiceCache.getGlobalRoles(authentication.getCredentials().toString());
            contains = globalRoles.contains(role);
        } else {
            ReleaseCenter releaseCenter = releaseCenterService.find(releaseCenterKey);
            Map<String, Set<String>> rolesToCodeSystemMap = permissionServiceCache.getCodeSystemRoles(authentication.getCredentials().toString());
            if (rolesToCodeSystemMap.containsKey(releaseCenter.getCodeSystem().toUpperCase())) {
                contains = rolesToCodeSystemMap.get(releaseCenter.getCodeSystem().toUpperCase()).contains(role);
            }
        }

        if (!contains) {
            logger.info("User '{}' does not have required role '{}' on release center '{}'.", getUsername(authentication), role, releaseCenterKey);
        }
        return contains;
    }

    public Map getRolesForLoggedInUser(Authentication authentication) {
        Map rolesMap = new HashMap();
        Set<String> globalRoles = permissionServiceCache.getGlobalRoles(authentication.getCredentials().toString());
        Map<String, Set<String>> rolesToCodeSystemMap = permissionServiceCache.getCodeSystemRoles(authentication.getCredentials().toString());
        rolesMap.put(Role.ADMIN + GLOBAL_SUFFIX, globalRoles.contains(Role.ADMIN));
        rolesMap.put(Role.RELEASE_MANAGER + GLOBAL_SUFFIX, globalRoles.contains(Role.RELEASE_MANAGER));
        rolesMap.put(Role.ADMIN, Collections.EMPTY_SET);
        rolesMap.put(Role.RELEASE_MANAGER, Collections.EMPTY_SET);
        rolesMap.put(Role.USER, Collections.EMPTY_SET);
        if (!rolesToCodeSystemMap.isEmpty()) {
            rolesToCodeSystemMap.forEach(
                    (codeSystem, roles) -> {
                        rolesMap.compute(Role.ADMIN, (key, val) -> roles.contains(Role.ADMIN) ? ((Set) val).add(codeSystem) : val);
                        rolesMap.compute(Role.RELEASE_MANAGER, (key, val) -> roles.contains(Role.RELEASE_MANAGER) ? ((Set) val).add(codeSystem) : val);
                        rolesMap.compute(Role.USER, (key, val) -> roles.contains(Role.USER) ? ((Set) val).add(codeSystem) : val);
                    }
            );

        }

        return rolesMap;
    }

    private String getUsername(Authentication authentication) {
        if (authentication != null) {
            Object principal = authentication.getPrincipal();
            if (principal != null) {
                return principal.toString();
            }
        }
        return null;
    }
}

package org.ihtsdo.buildcloud.service;

import org.ihtsdo.buildcloud.entity.ReleaseCenter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class PermissionService {

    private final Logger logger = LoggerFactory.getLogger(PermissionService.class);

    public final static String GLOBAL_SUFFIX = "_GLOBAL";

    public static final String GLOBAL_ROLE_SCOPE = "global";

    public enum Role {
        ADMIN, RELEASE_MANAGER, USER, AUTHOR
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
        rolesMap.put(Role.ADMIN + GLOBAL_SUFFIX, globalRoles.contains(Role.ADMIN.name()));
        rolesMap.put(Role.RELEASE_MANAGER + GLOBAL_SUFFIX, globalRoles.contains(Role.RELEASE_MANAGER.name()));
        rolesMap.put(Role.ADMIN, new HashSet<>());
        rolesMap.put(Role.RELEASE_MANAGER, new HashSet<>());
        rolesMap.put(Role.USER, new HashSet<>());
        if (!rolesToCodeSystemMap.isEmpty()) {
            rolesToCodeSystemMap.forEach(
                    (codeSystem, roles) -> {
                        if (roles.contains(Role.ADMIN.name())) {
                            ((Set) rolesMap.get(Role.ADMIN)).add(codeSystem);
                        }
                        if (roles.contains(Role.RELEASE_MANAGER.name())) {
                            ((Set) rolesMap.get(Role.RELEASE_MANAGER)).add(codeSystem);
                        }
                        if (roles.contains(Role.USER.name()) || roles.contains(Role.AUTHOR.name()) ) {
                            ((Set) rolesMap.get(Role.USER)).add(codeSystem);
                        }
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

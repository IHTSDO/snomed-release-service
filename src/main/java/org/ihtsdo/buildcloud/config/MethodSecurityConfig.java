package org.ihtsdo.buildcloud.config;

import org.ihtsdo.buildcloud.core.service.PermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;

import java.io.Serializable;

public class MethodSecurityConfig implements PermissionEvaluator  {

    @Autowired
    @Lazy
    private PermissionService permissionService;

    @Override
    public boolean hasPermission(Authentication authentication, Object role, Object releaseCenterKey) {
        if (releaseCenterKey == null) {
            throw new SecurityException("Release center is null, can not ascertain roles.");
        }
        return permissionService.userHasRoleOnReleaseCenter((String) role, (String) releaseCenterKey);
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
        return false;
    }
}


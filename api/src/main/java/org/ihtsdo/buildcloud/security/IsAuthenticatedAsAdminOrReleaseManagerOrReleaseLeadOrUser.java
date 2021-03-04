package org.ihtsdo.buildcloud.security;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("hasPermission('RELEASE_ADMIN', 'global') || hasPermission('RELEASE_ADMIN', #releaseCenterKey) || " +
        "hasPermission('RELEASE_MANAGER', 'global') || hasPermission('RELEASE_MANAGER', #releaseCenterKey) || " +
        "hasPermission('RELEASE_LEAD', 'global') || hasPermission('RELEASE_LEAD', #releaseCenterKey) || " +
        "hasPermission('USER', #releaseCenterKey)")
public @interface IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLeadOrUser {
}

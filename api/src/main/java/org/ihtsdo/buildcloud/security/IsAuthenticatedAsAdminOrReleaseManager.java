package org.ihtsdo.buildcloud.security;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("hasPermission('RAD_ADMIN', 'global') || hasPermission('RAD_ADMIN', #releaseCenterKey) || " +
              "hasPermission('RELEASE_MANAGER', 'global') || hasPermission('RELEASE_MANAGER', #releaseCenterKey)")
public @interface IsAuthenticatedAsAdminOrReleaseManager {
}

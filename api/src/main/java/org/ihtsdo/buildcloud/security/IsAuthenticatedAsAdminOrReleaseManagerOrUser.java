package org.ihtsdo.buildcloud.security;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("hasPermission('ADMIN','global') || hasPermission('ADMIN','#releaseCenterKey') || " +
        "hasPermission('RELEASE_MANAGER','global') || hasPermission('RELEASE_MANAGER','#releaseCenterKey') || " +
        "hasPermission('USER','#releaseCenterKey')")
public @interface IsAuthenticatedAsAdminOrReleaseManagerOrUser {
}

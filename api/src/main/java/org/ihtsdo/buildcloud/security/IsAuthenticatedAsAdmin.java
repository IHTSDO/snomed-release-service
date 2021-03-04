package org.ihtsdo.buildcloud.security;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("hasPermission('RELEASE_ADMIN', 'global') || hasPermission('RELEASE_ADMIN', #releaseCenterKey)")
public @interface IsAuthenticatedAsAdmin {
}

package org.ihtsdo.buildcloud.rest.security;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("hasPermission('RELEASE_ADMIN', 'global')")
public @interface IsAuthenticatedAsGlobalAdmin {
}

package org.ihtsdo.buildcloud.core.service;

import com.google.common.collect.Sets;
import org.ihtsdo.buildcloud.TestConfig;
import org.ihtsdo.otf.rest.client.RestClientException;
import org.ihtsdo.sso.integration.SecurityUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@ContextConfiguration(classes = TestConfig.class)
@Transactional
public class PermissionServiceTest {

    @InjectMocks
    @Autowired
    private PermissionService permissionService;

    @Mock
    private PermissionServiceCache permissionServiceCacheMock;

    @BeforeEach
    public void setup() {
        PreAuthenticatedAuthenticationToken authentication = new PreAuthenticatedAuthenticationToken("user", "password", Sets.newHashSet(new SimpleGrantedAuthority("USER")));
        SecurityContext context = new SecurityContextImpl();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testRoles() throws RestClientException {
        Set<String> globalRoles = Sets.newHashSet(Arrays.asList("RELEASE_ADMIN", "RELEASE_MANAGER", "RELEASE_LEAD"));
        Mockito.when(permissionServiceCacheMock.getGlobalRoles(SecurityUtil.getAuthenticationToken())).thenReturn(globalRoles);

        Map<String, Set<String>> userRoleMap = new HashMap<>();
        userRoleMap.put("SNOMEDCT", Sets.newHashSet("RELEASE_ADMIN"));
        userRoleMap.put("SNOMEDCT-DK", Sets.newHashSet("RELEASE_MANAGER"));
        userRoleMap.put("SNOMEDCT-IE", Sets.newHashSet("RELEASE_LEAD"));
        userRoleMap.put("SNOMEDCT-BE", Sets.newHashSet("RELEASE_USER"));
        Mockito.when(permissionServiceCacheMock.getCodeSystemRoles(SecurityUtil.getAuthenticationToken())).thenReturn(userRoleMap);

        Map<String, Set<String>> roleMap = permissionService.getRolesForLoggedInUser();

        assertTrue(roleMap.get("GLOBAL").contains(PermissionService.Role.RELEASE_ADMIN.name()));
        assertTrue(roleMap.get("GLOBAL").contains(PermissionService.Role.RELEASE_MANAGER.name()));
        assertTrue(roleMap.get("GLOBAL").contains(PermissionService.Role.RELEASE_LEAD.name()));

        assertEquals(1, roleMap.get("SNOMEDCT").size());
        assertTrue(roleMap.get("SNOMEDCT").contains(PermissionService.Role.RELEASE_ADMIN.name()));

        assertEquals(1, roleMap.get("SNOMEDCT-DK").size());
        assertTrue(roleMap.get("SNOMEDCT-DK").contains(PermissionService.Role.RELEASE_MANAGER.name()));

        assertEquals(1, roleMap.get("SNOMEDCT-IE").size());
        assertTrue(roleMap.get("SNOMEDCT-IE").contains(PermissionService.Role.RELEASE_LEAD.name()));

        assertEquals(1, roleMap.get("SNOMEDCT-BE").size());
        assertTrue(roleMap.get("SNOMEDCT-BE").contains(PermissionService.Role.RELEASE_USER.name()));
    }
}

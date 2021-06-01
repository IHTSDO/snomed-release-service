package org.ihtsdo.buildcloud.core.service;

import com.google.common.collect.Sets;
import org.easymock.MockType;
import org.easymock.internal.MocksControl;
import org.ihtsdo.buildcloud.config.TestConfig;
import org.ihtsdo.otf.rest.client.RestClientException;
import org.ihtsdo.sso.integration.SecurityUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@RunWith(MockitoJUnitRunner.class)
@ContextConfiguration(classes = TestConfig.class)
@Transactional
public class PermissionServiceTest {

    @InjectMocks
    @Autowired
    private PermissionService permissionService;

    @Mock
    private PermissionServiceCache permissionServiceCacheMock;

    private MocksControl mocksControl;

    @Before
    public void setup() {
        PreAuthenticatedAuthenticationToken authentication = new PreAuthenticatedAuthenticationToken("user", "password", Sets.newHashSet(new SimpleGrantedAuthority("USER")));
        SecurityContext context = new SecurityContextImpl();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        MockitoAnnotations.initMocks(this);
        mocksControl = new MocksControl(MockType.DEFAULT);
    }

    @Test
    public void testRoles() throws RestClientException {
        Set<String> globalRoles = Sets.newHashSet(Arrays.asList("RELEASE_ADMIN", "RELEASE_MANAGER", "RELEASE_LEAD"));
        Mockito.when(permissionServiceCacheMock.getGlobalRoles(SecurityUtil.getAuthenticationToken())).thenReturn(globalRoles);

        Map userRoleMap = new HashMap();
        userRoleMap.put("SNOMEDCT", Sets.newHashSet(Arrays.asList("RELEASE_ADMIN")));
        userRoleMap.put("SNOMEDCT-DK", Sets.newHashSet(Arrays.asList("RELEASE_MANAGER")));
        userRoleMap.put("SNOMEDCT-IE", Sets.newHashSet(Arrays.asList("RELEASE_LEAD")));
        userRoleMap.put("SNOMEDCT-BE", Sets.newHashSet(Arrays.asList("RELEASE_USER")));
        Mockito.when(permissionServiceCacheMock.getCodeSystemRoles(SecurityUtil.getAuthenticationToken())).thenReturn(userRoleMap);

        Map roleMap = permissionService.getRolesForLoggedInUser();

        Assert.assertTrue(((Set)roleMap.get("GLOBAL")).contains(PermissionService.Role.RELEASE_ADMIN.name()));
        Assert.assertTrue(((Set)roleMap.get("GLOBAL")).contains(PermissionService.Role.RELEASE_MANAGER.name()));
        Assert.assertTrue(((Set)roleMap.get("GLOBAL")).contains(PermissionService.Role.RELEASE_LEAD.name()));

        Assert.assertEquals(1, ((Set) roleMap.get("SNOMEDCT")).size());
        Assert.assertTrue(((Set) roleMap.get("SNOMEDCT")).contains(PermissionService.Role.RELEASE_ADMIN.name()));

        Assert.assertEquals(1, ((Set) roleMap.get("SNOMEDCT-DK")).size());
        Assert.assertTrue(((Set) roleMap.get("SNOMEDCT-DK")).contains(PermissionService.Role.RELEASE_MANAGER.name()));

        Assert.assertEquals(1, ((Set) roleMap.get("SNOMEDCT-IE")).size());
        Assert.assertTrue(((Set) roleMap.get("SNOMEDCT-IE")).contains(PermissionService.Role.RELEASE_LEAD.name()));

        Assert.assertEquals(1, ((Set) roleMap.get("SNOMEDCT-BE")).size());
        Assert.assertTrue(((Set) roleMap.get("SNOMEDCT-BE")).contains(PermissionService.Role.RELEASE_USER.name()));
    }
}

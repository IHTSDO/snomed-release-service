package org.ihtsdo.buildcloud.service;

import com.google.common.collect.Sets;
import org.easymock.MockType;
import org.easymock.internal.MocksControl;
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
@ContextConfiguration(locations={"/test/testDataServiceContext.xml"})
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
        Set<String> globalRoles = Sets.newHashSet(Arrays.asList("ADMIN", "RELEASE_MANAGER"));
        Mockito.when(permissionServiceCacheMock.getGlobalRoles(SecurityUtil.getAuthenticationToken())).thenReturn(globalRoles);

        Map userRoleMap = new HashMap();
        userRoleMap.put("SNOMEDCT", Sets.newHashSet(Arrays.asList("ADMIN")));
        userRoleMap.put("SNOMEDCT-DK", Sets.newHashSet(Arrays.asList("RELEASE_MANAGER")));
        userRoleMap.put("SNOMEDCT-BE", Sets.newHashSet(Arrays.asList("USER")));
        Mockito.when(permissionServiceCacheMock.getCodeSystemRoles(SecurityUtil.getAuthenticationToken())).thenReturn(userRoleMap);

        Map roleMap = permissionService.getRolesForLoggedInUser();

        Assert.assertTrue(Boolean.valueOf(roleMap.get("ADMIN_GLOBAL").toString()));
        Assert.assertTrue(Boolean.valueOf(roleMap.get("RELEASE_MANAGER_GLOBAL").toString()));
        Assert.assertEquals(1, ((Set) roleMap.get(PermissionService.Role.RELEASE_MANAGER)).size());
        Assert.assertTrue(((Set) roleMap.get(PermissionService.Role.RELEASE_MANAGER)).contains("SNOMEDCT-DK"));
        Assert.assertEquals(1, ((Set) roleMap.get(PermissionService.Role.ADMIN)).size());
        Assert.assertTrue(((Set) roleMap.get(PermissionService.Role.ADMIN)).contains("SNOMEDCT"));
        Assert.assertEquals(1, ((Set) roleMap.get(PermissionService.Role.RELEASE_MANAGER)).size());
        Assert.assertTrue(((Set) roleMap.get(PermissionService.Role.RELEASE_MANAGER)).contains("SNOMEDCT-DK"));
        Assert.assertEquals(1, ((Set) roleMap.get(PermissionService.Role.USER)).size());
        Assert.assertTrue(((Set) roleMap.get(PermissionService.Role.USER)).contains("SNOMEDCT-BE"));
    }
}

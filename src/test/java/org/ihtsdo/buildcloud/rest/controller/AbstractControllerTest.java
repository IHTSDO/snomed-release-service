package org.ihtsdo.buildcloud.rest.controller;

import org.ihtsdo.buildcloud.TestConfig;
import org.ihtsdo.buildcloud.core.service.PermissionService;
import org.ihtsdo.buildcloud.core.service.PermissionServiceCache;
import org.ihtsdo.buildcloud.test.AbstractTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.CharacterEncodingFilter;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = TestConfig.class)
public abstract class AbstractControllerTest extends AbstractTest {

	public static final MediaType APPLICATION_JSON = MediaType.APPLICATION_JSON;

	public static final String ROOT_URL = "http://localhost";

	protected MockMvc mockMvc;

	@Autowired
	private WebApplicationContext wac;

	@MockitoBean
	protected PermissionServiceCache permissionServiceCache;

	@BeforeEach
	public void setup() throws Exception {
		super.setup();
		SecurityContextHolder.getContext().setAuthentication(new PreAuthenticatedAuthenticationToken("test-user", "test-token",
				List.of(new SimpleGrantedAuthority(PermissionService.USER_ROLE))));
		when(permissionServiceCache.getGlobalRoles(any())).thenReturn(Set.of(PermissionService.Role.RELEASE_ADMIN.name()));
		CharacterEncodingFilter filter = new CharacterEncodingFilter();
		filter.setEncoding("UTF-8");
		filter.setForceEncoding(true);
		mockMvc = MockMvcBuilders.webAppContextSetup(wac).addFilter(filter, "/*").build();
		assertNotNull(mockMvc);
	}

	@AfterEach
	public void tearDown() {
		SecurityContextHolder.clearContext();
		try {
			super.tearDown();
		} catch (IOException e) {
			throw new IllegalStateException("Failed to delete test data", e);
		}
	}

}

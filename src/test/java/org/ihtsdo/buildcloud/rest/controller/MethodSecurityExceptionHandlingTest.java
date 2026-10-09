package org.ihtsdo.buildcloud.rest.controller;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MethodSecurityExceptionHandlingTest extends AbstractControllerTest {

	private static final String CENTER_URL = "/centers/international";

	@Test
	void returnsForbiddenWhenUserHasNoRoleOnReleaseCenter() throws Exception {
		when(permissionServiceCache.getGlobalRoles(any())).thenReturn(Collections.emptySet());
		when(permissionServiceCache.getCodeSystemRoles(any())).thenReturn(Collections.emptyMap());

		mockMvc.perform(get(CENTER_URL))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.HTTPStatus", is("403 FORBIDDEN")));
	}

	@Test
	void returnsUnauthorizedWhenNotAuthenticated() throws Exception {
		SecurityContextHolder.clearContext();

		mockMvc.perform(get(CENTER_URL))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.HTTPStatus", is("401 UNAUTHORIZED")));
	}
}

package org.ihtsdo.buildcloud.rest.controller;

import org.junit.Test;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Transactional
public class RootControllerTest extends AbstractControllerTest {

	@Test
	public void root_returns_centers_and_products() throws Exception {
		mockMvc.perform(get("/"))
				.andExpect(status().isOk())
				.andDo(print())
				.andExpect(content().contentType(APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.jsonPath("$.url", is(ROOT_URL)))
				.andExpect(jsonPath("$.user_url", is(ROOT_URL + "/user")))
				.andExpect(jsonPath("$.login_url", is(ROOT_URL + "/login")))
				.andExpect(jsonPath("$.centers_url", is(ROOT_URL + "/centers")));
	}

}

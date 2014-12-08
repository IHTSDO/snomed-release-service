package org.ihtsdo.buildcloud.controller;

import org.ihtsdo.buildcloud.entity.helper.EntityHelper;
import org.ihtsdo.buildcloud.entity.helper.TestEntityGenerator;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class ReleaseCenterControllerTest extends AbstractControllerTest {

	@Test
	public void test() throws Exception {
		String centerId = EntityHelper.formatAsBusinessKey(TestEntityGenerator.releaseCenterShortNames[0]);
		String centerUrl = ROOT_URL + "/centers/" + centerId;
		mockMvc.perform(get("/centers"))
				.andExpect(status().isOk())
				.andDo(print())
				.andExpect(content().contentType(APPLICATION_JSON_UTF8))
				.andExpect(jsonPath("[0]$.id", is(centerId)))
				.andExpect(jsonPath("[0]$.name", is(TestEntityGenerator.releaseCenterNames[0])))
				.andExpect(jsonPath("[0]$.shortName", is(TestEntityGenerator.releaseCenterShortNames[0])))
				.andExpect(jsonPath("[0]$.removed", is(false)))
				.andExpect(jsonPath("[0]$.url", is(centerUrl)))
				.andExpect(jsonPath("[0]$.products_url", is(centerUrl + "/products")))
				.andExpect(jsonPath("[0]$.published_url", is(centerUrl + "/published")))
		;
	}

}

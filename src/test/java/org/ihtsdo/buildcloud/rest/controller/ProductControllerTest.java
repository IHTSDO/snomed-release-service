package org.ihtsdo.buildcloud.rest.controller;

import org.ihtsdo.buildcloud.core.entity.helper.EntityHelper;
import org.ihtsdo.buildcloud.core.entity.helper.TestEntityGenerator;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class ProductControllerTest extends AbstractControllerTest {
	
	@Test
	public void returns_products() throws Exception {
		int random_product = 3;
		String productName = TestEntityGenerator.productNames[random_product];
		String businessKey = EntityHelper.formatAsBusinessKey(productName);

		String releaseCenterShortName = TestEntityGenerator.releaseCenterShortNames[0];
		String releaseCenterKey = EntityHelper.formatAsBusinessKey(releaseCenterShortName);

//		SecurityHelper.setUser(TestUtils.TEST_USER);

		//JSON object indexes start from 1 so add 1 to the traditional 0 based index
		String productsUrl = "/centers/" + releaseCenterKey + "/products/";
		mockMvc.perform(get(productsUrl))
				.andExpect(status().isOk())
				//.andDo(print())
				.andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON));
				//.andExpect(jsonPath("$[" + random_product + "].url", is(ROOT_URL + productsUrl + businessKey)));
	}

}

package org.ihtsdo.buildcloud.controller;

import org.ihtsdo.buildcloud.entity.helper.EntityHelper;
import org.ihtsdo.buildcloud.entity.helper.TestEntityGenerator;
import org.ihtsdo.buildcloud.service.security.SecurityHelper;
import org.ihtsdo.buildcloud.test.TestUtils;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
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

		SecurityHelper.setUser(TestUtils.TEST_USER);

		//JSON object indexes start from 1 so add 1 to the traditional 0 based index
		String productsUrl = "/centers/" + releaseCenterKey + "/products/";
		mockMvc.perform(get(productsUrl))
				.andExpect(status().isOk())
				//.andDo(print())
				.andExpect(content().contentType(APPLICATION_JSON_UTF8))
				.andExpect(jsonPath("$[" + random_product + "].url", is(ROOT_URL + productsUrl + businessKey)));
	}

}

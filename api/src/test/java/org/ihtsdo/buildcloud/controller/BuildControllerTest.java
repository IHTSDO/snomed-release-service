package org.ihtsdo.buildcloud.controller;

import org.hibernate.SessionFactory;
import org.ihtsdo.buildcloud.entity.helper.EntityHelper;
import org.ihtsdo.buildcloud.entity.helper.TestEntityGenerator;
import org.ihtsdo.buildcloud.security.SecurityHelper;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
//import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;

public class BuildControllerTest extends ControllerTest{
	
	@Autowired
	private SessionFactory sessionFactory;

	@Test
	public void returns_builds() throws Exception {
		
		int random_extension_index = 0;
		int random_product_index = 0;
		int random_build = 3;
		String buildStr = TestEntityGenerator.buildNames[random_extension_index][random_product_index][random_build];
		
		SecurityHelper.setSubject(TestEntityGenerator.TEST_USER);
		
		//JSON object indexes start from 1 so add 1 to the traditional 0 based index
		String build_id =  (random_build+1) + "_" + EntityHelper.formatAsBusinessKey(buildStr);
		mockMvc.perform(get("/builds"))
				.andExpect(status().isOk())
				//.andDo(print())
				.andExpect(content().contentType(APPLICATION_JSON_UTF8))
				.andExpect(jsonPath("$[" + random_build + "].url", is(ROOT_URL + "/builds/" + build_id)));
	}

}
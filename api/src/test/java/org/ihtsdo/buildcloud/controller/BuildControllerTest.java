package org.ihtsdo.buildcloud.controller;

import org.ihtsdo.buildcloud.entity.helper.EntityHelper;
import org.ihtsdo.buildcloud.entity.helper.TestEntityGenerator;
import org.ihtsdo.buildcloud.service.security.SecurityHelper;
import org.ihtsdo.buildcloud.test.TestUtils;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class BuildControllerTest extends AbstractControllerTest {
	
	@Test
	public void returns_builds() throws Exception {
		int random_build = 3;
		String buildName = TestEntityGenerator.buildNames[random_build];
		String businessKey = EntityHelper.formatAsBusinessKey(buildName);

		String releaseCenterShortName = TestEntityGenerator.releaseCenterShortNames[0];
		String releaseCenterKey = EntityHelper.formatAsBusinessKey(releaseCenterShortName);

		SecurityHelper.setUser(TestUtils.TEST_USER);

		//JSON object indexes start from 1 so add 1 to the traditional 0 based index
		String buildsUrl = "/centers/" + releaseCenterKey + "/builds/";
		mockMvc.perform(get(buildsUrl))
				.andExpect(status().isOk())
				//.andDo(print())
				.andExpect(content().contentType(APPLICATION_JSON_UTF8))
				.andExpect(jsonPath("$[" + random_build + "].url", is(ROOT_URL + buildsUrl + businessKey)));
	}

}

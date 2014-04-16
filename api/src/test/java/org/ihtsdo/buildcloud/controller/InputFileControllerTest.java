package org.ihtsdo.buildcloud.controller;

import javax.servlet.annotation.MultipartConfig;

import org.hibernate.SessionFactory;
import org.ihtsdo.buildcloud.entity.helper.EntityHelper;
import org.ihtsdo.buildcloud.entity.helper.TestEntityGenerator;
import org.ihtsdo.buildcloud.security.SecurityHelper;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
//import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;

public class InputFileControllerTest extends ControllerTest{
	
	@Autowired
	private SessionFactory sessionFactory;

	@Test
	public void uploadManifest() throws Exception {
		
		int random_extension_index = 0;
		int random_product_index = 0;
		int random_build = 3;
		int random_package = 0;
		String packageStr = EntityHelper.formatAsBusinessKey(TestEntityGenerator.packageNames[random_package]);
		String buildStr = TestEntityGenerator.buildNames[random_extension_index][random_product_index][random_build];
		
		SecurityHelper.setSubject(TestEntityGenerator.TEST_USER);
		//JSON object indexes start from 1 so add 1 to the traditional 0 based index
		String build_id =  (random_build+1) + "_" + EntityHelper.formatAsBusinessKey(buildStr);
		String testFileName = "test_manifest.xml";
		String postURL = ROOT_URL + "/builds/" + build_id + "/packages/" + packageStr + "/manifest";
		MockMultipartFile testFile = new MockMultipartFile("file", testFileName , null , "some manifest type text here".getBytes());
		
		String expectedResult = postURL + "/" + EntityHelper.formatAsBusinessKey(testFileName);
		mockMvc.perform(MockMvcRequestBuilders.fileUpload(postURL)
				.file(testFile)
				.param("buildCompositeKey", build_id)
				.param("packageBusinessKey", packageStr))
			//.andDo(print())
			.andExpect(status().is(200))
			.andExpect(content().contentType(APPLICATION_JSON_UTF8))
			.andExpect(jsonPath("$.url", is(expectedResult)));
	}

}
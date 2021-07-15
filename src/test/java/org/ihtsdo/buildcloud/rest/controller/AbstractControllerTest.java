package org.ihtsdo.buildcloud.rest.controller;

import org.ihtsdo.buildcloud.TestConfig;
import org.ihtsdo.buildcloud.test.AbstractTest;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.CharacterEncodingFilter;

import java.io.IOException;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = TestConfig.class)
public abstract class AbstractControllerTest extends AbstractTest {

	public static final MediaType APPLICATION_JSON = MediaType.APPLICATION_JSON;

	public static final String ROOT_URL = "http://localhost";

	protected MockMvc mockMvc;

	@Autowired
	private WebApplicationContext wac;

	@Before
	public void setup() throws Exception {
		super.setup();
		CharacterEncodingFilter filter = new CharacterEncodingFilter();
		filter.setEncoding("UTF-8");
		filter.setForceEncoding(true);
		mockMvc = MockMvcBuilders.webAppContextSetup(wac).addFilter(filter, "/*").build();
		Assert.assertNotNull(mockMvc);
	}

	@After
	public void tearDown() {
		try {
			super.tearDown();
		} catch (IOException e) {
			throw new IllegalStateException("Failed to delete test data", e);
		}
	}

}

package org.ihtsdo.buildcloud.controller;

import org.junit.Assert;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import javax.servlet.ServletException;
import java.nio.charset.Charset;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"/testDispatcherServletContext.xml"})
@WebAppConfiguration
public abstract class AbstractControllerTest {

	public static final MediaType APPLICATION_JSON_UTF8 = new MediaType(MediaType.APPLICATION_JSON.getType(),
			MediaType.APPLICATION_JSON.getSubtype(),
			Charset.forName("utf8")
		);

	public static final String ROOT_URL = "http://localhost:80";

	protected MockMvc mockMvc;

	@Autowired
	private WebApplicationContext wac;

	@Before
	public void setup() throws ServletException {
		mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
		Assert.assertNotNull(mockMvc);
	}

}

package org.ihtsdo.buildcloud.controller;

import com.jayway.jsonpath.JsonPath;
import org.apache.commons.codec.binary.Base64;
import org.ihtsdo.buildcloud.dao.s3.S3Client;
import org.ihtsdo.buildcloud.dao.s3.TestS3Client;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

import javax.servlet.ServletException;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class SimpleRefsetTestIntegration extends AbstractControllerTest {

	@Autowired
	private S3Client s3Client;

	@Test
	public void test() throws Exception {

		Assert.assertNotNull(mockMvc);

		// Login
		MvcResult loginResult = mockMvc.perform(
			post("/login")
				.param("username", "manager")
				.param("password", "test123")
			)
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(content().contentType(APPLICATION_JSON_UTF8))
			.andExpect(jsonPath("$.authenticationToken", notNullValue()))
			.andReturn();

		String authenticationToken = JsonPath.read(loginResult.getResponse().getContentAsString(), "$.authenticationToken");
		String basicDigestHeaderValue = "Basic " + new String(Base64.encodeBase64((authenticationToken + ":").getBytes()));


		// Create Build
		MvcResult createBuildResult = mockMvc.perform(
			post("/centers/international/extensions/snomed_ct_international_edition/products/nlm_example_refset/builds")
				.header("Authorization", basicDigestHeaderValue)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{ \"name\" : \"test-build\" }")
			)
			.andDo(print())
			.andExpect(status().isCreated())
			.andExpect(content().contentType(APPLICATION_JSON_UTF8))
			.andReturn();

		String buildId = JsonPath.read(createBuildResult.getResponse().getContentAsString(), "$.id");


		// Create Package
		mockMvc.perform(
			post("/builds/" + buildId + "/packages")
				.header("Authorization", basicDigestHeaderValue)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{ \"name\" : \"testpackage\" }")
			)
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(content().contentType(APPLICATION_JSON_UTF8));


		// Upload Manifest
		mockMvc.perform(
			fileUpload("/builds/" + buildId + "/packages/testpackage/manifest")
				.file(new MockMultipartFile("file", "simple_refset_manifest.xml", "text/plain", getClass().getResourceAsStream("/simple_refset_manifest.xml")))
				.header("Authorization", basicDigestHeaderValue)
			)
			.andDo(print())
			.andExpect(status().isOk());


		// Upload Input File
		mockMvc.perform(
			fileUpload("/builds/" + buildId + "/packages/testpackage/inputfiles")
				.file(new MockMultipartFile("file", "der2_Refset_SimpleDelta_INT_20140131.txt", "text/plain", getClass().getResourceAsStream("/der2_Refset_SimpleDelta_INT_20140131.txt")))
				.header("Authorization", basicDigestHeaderValue)
			)
			.andDo(print())
			.andExpect(status().isOk());


		// Set Build effectiveTime
		mockMvc.perform(
			request(HttpMethod.PATCH, "/builds/" + buildId)
				.header("Authorization", basicDigestHeaderValue)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{ \"effectiveTime\" : \"2014-01-31\" }")
			)
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(content().contentType(APPLICATION_JSON_UTF8));


		// Create Execution
		MvcResult createExecutionResult = mockMvc.perform(
			post("/builds/" + buildId + "/executions")
				.header("Authorization", basicDigestHeaderValue)
				.contentType(MediaType.APPLICATION_JSON)
			)
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(content().contentType(APPLICATION_JSON_UTF8))
			.andReturn();

		String executionId = JsonPath.read(createExecutionResult.getResponse().getContentAsString(), "$.id");


		// Trigger Execution
//		mockMvc.perform(
//			post("/builds/" + buildId + "/executions/" + executionId + "/trigger")
//				.header("Authorization", basicDigestHeaderValue)
//				.contentType(MediaType.APPLICATION_JSON)
//			)
//			.andDo(print())
//			.andExpect(status().isOk())
//			.andExpect(content().contentType(APPLICATION_JSON_UTF8));


	}

	@Before
	public void setup() throws ServletException {
		super.setup();
		((TestS3Client) s3Client).deleteBuckets();
	}

	@After
	public void tearDown() {
		((TestS3Client) s3Client).deleteBuckets();
	}

}

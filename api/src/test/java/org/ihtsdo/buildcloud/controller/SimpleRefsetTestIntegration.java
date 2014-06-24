package org.ihtsdo.buildcloud.controller;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.fileUpload;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import javax.servlet.ServletException;

import org.apache.commons.codec.binary.Base64;
import org.ihtsdo.buildcloud.dao.s3.S3Client;
import org.ihtsdo.buildcloud.dao.s3.TestS3Client;
import org.ihtsdo.buildcloud.service.BuildService;
import org.ihtsdo.buildcloud.service.PackageService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

import com.jayway.jsonpath.JsonPath;

public class SimpleRefsetTestIntegration extends AbstractControllerTest {

	@Autowired
	private S3Client s3Client;
	
	public static final String PRODUCT_URL = "/centers/international/extensions/snomed_ct_international_edition/products/nlm_example_refset";

	String basicDigestHeaderValue = "NOT_YET_AUTHENTICATED";
	
	static final String TEST_PACKAGE = "testpackage";
	
	@Test
	public void testMultipleReleases() throws Exception {
	    
	    try{
		boolean isFirstTimeRelease = true;
		String buildId = createBuildStructure( isFirstTimeRelease );
		doExecution (buildId,  "2014-01-31", isFirstTimeRelease);
		isFirstTimeRelease = false;
		buildId = createBuildStructure( isFirstTimeRelease );
		doExecution (buildId,  "2014-07-31",  isFirstTimeRelease);
	    }catch(Throwable e)
	    {
		Assert.fail("Error occurred!");
	    }
	    
	}
	
	private String createBuildStructure(boolean isFirstTimeRelease)  throws Exception {

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
		basicDigestHeaderValue = "Basic " + new String(Base64.encodeBase64((authenticationToken + ":").getBytes()));


		// Create Build
		MvcResult createBuildResult = mockMvc.perform(
			post(PRODUCT_URL +"/builds")
				.header("Authorization", basicDigestHeaderValue)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{ \"name\" : \"test-build\" }")
			)
			.andDo(print())
			.andExpect(status().isCreated())
			.andExpect(content().contentType(APPLICATION_JSON_UTF8))
			.andReturn();

		String buildId = JsonPath.read(createBuildResult.getResponse().getContentAsString(), "$.id");
		String packageURL = "/builds/" + buildId + "/packages/" + TEST_PACKAGE;

		
		// Create Package
		mockMvc.perform(
			post("/builds/" + buildId + "/packages")
				.header("Authorization", basicDigestHeaderValue)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{ \"name\" : \"" + TEST_PACKAGE + "\" }")
			)
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(content().contentType(APPLICATION_JSON_UTF8));

		
		// Upload Manifest
		mockMvc.perform(
			fileUpload(packageURL +"/manifest")
				.file(new MockMultipartFile("file", "simple_refset_manifest.xml", "text/plain", getClass().getResourceAsStream("/simple_refset_manifest.xml")))
				.header("Authorization", basicDigestHeaderValue)
			)
			.andDo(print())
			.andExpect(status().isOk());


		// Upload Input File
		
		String deltaFileName = "der2_Refset_SimpleDelta_INT_20140131.txt";
		if( !isFirstTimeRelease ){
			deltaFileName = "der2_Refset_SimpleDelta_INT_20140731.txt";
		}
		mockMvc.perform(
			fileUpload(packageURL +"/inputfiles")
				.file(new MockMultipartFile("file", deltaFileName, "text/plain", getClass().getResourceAsStream("/" + deltaFileName)))
				.header("Authorization", basicDigestHeaderValue)
			)
			.andDo(print())
			.andExpect(status().isOk());
		
		return buildId;
	}

	private void doExecution (String buildId, String effectiveDate, boolean isFirstTime) throws Exception {

		// Set Build effectiveTime
		String jsonContent = "{ " + jsonPair ( BuildService.EFFECTIVE_TIME, effectiveDate) +  " }";
		mockMvc.perform(
			request(HttpMethod.PATCH, "/builds/" + buildId)
				.header("Authorization", basicDigestHeaderValue)
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonContent)
			)
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(content().contentType(APPLICATION_JSON_UTF8));

		
		//Set isFirstTime and previousPublished file on the Package 
		String previousPublishedFile = "";
		if (!isFirstTime) {
			previousPublishedFile = getPreviousPublishedPackage();
		}

		String packageURL = "/builds/" + buildId + "/packages/" + TEST_PACKAGE;
		jsonContent = 	"{ "
						+ 		jsonPair ( PackageService.FIRST_TIME_RELEASE, Boolean.toString(isFirstTime))
						+ 		(isFirstTime ? "" : "," + jsonPair( PackageService.PREVIOUS_PUBLISHED_FULL_FILE, previousPublishedFile))
						+ " }";
		mockMvc.perform(
				request(HttpMethod.PATCH, packageURL)
					.header("Authorization", basicDigestHeaderValue)
					.contentType(MediaType.APPLICATION_JSON)
					.content(jsonContent)
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
		String executionURL = "/builds/" + buildId + "/executions/" + executionId;

		// Trigger Execution
		mockMvc.perform(
			post( executionURL + "/trigger")
				.header("Authorization", basicDigestHeaderValue)
				.contentType(MediaType.APPLICATION_JSON)
			)
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(content().contentType(APPLICATION_JSON_UTF8));

		//Publish output
		mockMvc.perform( 
				post( executionURL + "/output/publish")
				.header("Authorization", basicDigestHeaderValue)
				.contentType(MediaType.APPLICATION_JSON)
			)
			.andDo(print())
			.andExpect(status().isOk());
			//.andExpect(content().contentType(APPLICATION_JSON_UTF8));
		

	}
	
	private String getPreviousPublishedPackage() throws Exception {
		
		//Recover URL of published things from Product
		MvcResult productResult = mockMvc.perform( 
				post(PRODUCT_URL)
				.header("Authorization", basicDigestHeaderValue)
				.contentType(MediaType.APPLICATION_JSON)
			)
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(content().contentType(APPLICATION_JSON_UTF8)
			).andReturn();
		String publishedURL = JsonPath.read(productResult.getResponse().getContentAsString(), "$.published_url");
		String expectedURL = "http://localhost:80/centers/international/extensions/snomed_ct_international_edition/products/nlm_example_refset/published";

		Assert.assertEquals(expectedURL, publishedURL);
		
		//Recover list of published packages
		MvcResult publishedResult = mockMvc.perform( 
				post( publishedURL )
				.header("Authorization", basicDigestHeaderValue)
				.contentType(MediaType.APPLICATION_JSON)
			)
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(content().contentType(APPLICATION_JSON_UTF8)
			).andReturn();
		String publishedFile = JsonPath.read(publishedResult.getResponse().getContentAsString(), "$.publishedPackages[0]");
		return publishedFile;
	}
	
	/*
	 * @return a string formatted for use as a JSON key/value pair eg 	"\"effectiveTime\" : \""+ effectiveDate + "\","
	 * with a trailing comma just in case you want more than one and json is OK with that if there's only one
	 */
	private String jsonPair (String key, String value) {
		
		return "  \"" + key + "\" : \"" + value + "\" ";
		
	}

	@Override
	@Before
	public void setup() throws ServletException {
		super.setup();
		((TestS3Client) s3Client).deleteBuckets();
	}
//
//	@After
//	public void tearDown() {
//		((TestS3Client) s3Client).deleteBuckets();
//	}

}

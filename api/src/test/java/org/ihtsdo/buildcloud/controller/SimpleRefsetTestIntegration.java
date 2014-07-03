package org.ihtsdo.buildcloud.controller;

import com.jayway.jsonpath.JsonPath;
import org.apache.commons.codec.binary.Base64;
import org.ihtsdo.buildcloud.dao.s3.S3Client;
import org.ihtsdo.buildcloud.dao.s3.TestS3Client;
import org.ihtsdo.buildcloud.service.BuildService;
import org.ihtsdo.buildcloud.service.PackageService;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

import javax.servlet.ServletException;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class SimpleRefsetTestIntegration extends AbstractControllerTest {

	@Autowired
	private S3Client s3Client;

	public static final String PRODUCT_URL = "/centers/international/extensions/snomed_ct_international_edition/products/nlm_example_refset";

	String basicDigestHeaderValue = "NOT_YET_AUTHENTICATED";

	static final String TEST_PACKAGE = "testpackage";

	@Test
	public void testMultipleReleases() throws Exception {
		String buildId = createBuildStructure();

		// First time release
		String executionURL = doExecution(buildId, "2014-01-31", null);

		String expectedZipFilename = "SnomedCT_Release_INT_20140131.zip";
		String expectedZipEntries = "SnomedCT_Release_INT_20140131/\n" +
				"SnomedCT_Release_INT_20140131/RF2Release/\n" +
				"SnomedCT_Release_INT_20140131/RF2Release/Full/\n" +
				"SnomedCT_Release_INT_20140131/RF2Release/Full/Refset/\n" +
				"SnomedCT_Release_INT_20140131/RF2Release/Full/Refset/Content/\n" +
				"SnomedCT_Release_INT_20140131/RF2Release/Full/Refset/Content/der2_Refset_SimpleFull_INT_20140131.txt\n" +
				"SnomedCT_Release_INT_20140131/RF2Release/Snapshot/\n" +
				"SnomedCT_Release_INT_20140131/RF2Release/Snapshot/Refset/\n" +
				"SnomedCT_Release_INT_20140131/RF2Release/Snapshot/Refset/Content/\n" +
				"SnomedCT_Release_INT_20140131/RF2Release/Snapshot/Refset/Content/der2_Refset_SimpleSnapshot_INT_20140131.txt\n" +
				"SnomedCT_Release_INT_20140131/RF2Release/Delta/\n" +
				"SnomedCT_Release_INT_20140131/RF2Release/Delta/Refset/\n" +
				"SnomedCT_Release_INT_20140131/RF2Release/Delta/Refset/Content/\n" +
				"SnomedCT_Release_INT_20140131/RF2Release/Delta/Refset/Content/der2_Refset_SimpleDelta_INT_20140131.txt";
		testOutput(executionURL, expectedZipFilename, expectedZipEntries);

		Thread.sleep(1000); // Next build has to have a different timestamp

		// Second release
		executionURL = doExecution(buildId, "2014-07-31", "2014-01-31");
		expectedZipFilename = "SnomedCT_Release_INT_20140731.zip";
		expectedZipEntries = "SnomedCT_Release_INT_20140731/\n" +
				"SnomedCT_Release_INT_20140731/RF2Release/\n" +
				"SnomedCT_Release_INT_20140731/RF2Release/Full/\n" +
				"SnomedCT_Release_INT_20140731/RF2Release/Full/Refset/\n" +
				"SnomedCT_Release_INT_20140731/RF2Release/Full/Refset/Content/\n" +
				"SnomedCT_Release_INT_20140731/RF2Release/Full/Refset/Content/der2_Refset_SimpleFull_INT_20140731.txt\n" +
				"SnomedCT_Release_INT_20140731/RF2Release/Snapshot/\n" +
				"SnomedCT_Release_INT_20140731/RF2Release/Snapshot/Refset/\n" +
				"SnomedCT_Release_INT_20140731/RF2Release/Snapshot/Refset/Content/\n" +
				"SnomedCT_Release_INT_20140731/RF2Release/Snapshot/Refset/Content/der2_Refset_SimpleSnapshot_INT_20140731.txt\n" +
				"SnomedCT_Release_INT_20140731/RF2Release/Delta/\n" +
				"SnomedCT_Release_INT_20140731/RF2Release/Delta/Refset/\n" +
				"SnomedCT_Release_INT_20140731/RF2Release/Delta/Refset/Content/\n" +
				"SnomedCT_Release_INT_20140731/RF2Release/Delta/Refset/Content/der2_Refset_SimpleDelta_INT_20140731.txt";
		testOutput(executionURL, expectedZipFilename, expectedZipEntries);
	}

	private String createBuildStructure() throws Exception {

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
				post(PRODUCT_URL + "/builds")
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
						.content("{ \"name\" : \"" + TEST_PACKAGE + "\" }")
		)
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(content().contentType(APPLICATION_JSON_UTF8));

		return buildId;
	}

	/**
	 * @param buildId
	 * @param effectiveDate
	 * @param previousEffectiveDate if null, then we'll treat this as a first time execution
	 * @throws Exception
	 */
	private String doExecution(String buildId, String effectiveDate, String previousEffectiveDate) throws Exception {

		boolean isFirstTime = previousEffectiveDate == null;

		String packageURL = "/builds/" + buildId + "/packages/" + TEST_PACKAGE;

		//ISO format for date has dashes in it YYYY-MM-DD, strip out to use in filenames
		String effectiveDateStripped = effectiveDate.replace("-", "");

		// Upload Input File - specific to each run
		String deltaFileName = "der2_Refset_SimpleDelta_INT_" + effectiveDateStripped + ".txt";
		MockMultipartFile deltaFile = new MockMultipartFile("file", deltaFileName, "text/plain", getClass().getResourceAsStream("/" + deltaFileName));
		mockMvc.perform(
				fileUpload(packageURL + "/inputfiles")
						.file(deltaFile)
						.header("Authorization", basicDigestHeaderValue)
		)
				.andDo(print())
				.andExpect(status().isOk());

		//And if we're doing a subsequent run, we need to delete the input file from the previous run!
		if (!isFirstTime) {
			String previousEffectiveDateStripped = previousEffectiveDate.replace("-", "");
			String previousDeltaFileName = "der2_Refset_SimpleDelta_INT_" + previousEffectiveDateStripped + ".txt";  //%2E is url friendly hex for a full stop.
			mockMvc.perform(
					request(HttpMethod.DELETE, packageURL + "/inputfiles/" + previousDeltaFileName)
							.header("Authorization", basicDigestHeaderValue)
			)
					.andDo(print())
					.andExpect(status().isNoContent());
		}

		// Upload Manifest - again specific to the release date.
		// We're going to give it the same name on upload to ensure it gets overwritten, but the code wipes that directory
		// on upload anyway.
		String manifestFileName = "simple_refset_manifest_" + effectiveDateStripped + ".xml";
		String givenName = "manifest.xml";
		MockMultipartFile manifestFile = new MockMultipartFile("file", givenName, "text/plain", getClass().getResourceAsStream("/" + manifestFileName));
		mockMvc.perform(
				fileUpload(packageURL + "/manifest")
						.file(manifestFile)
						.header("Authorization", basicDigestHeaderValue)
		)
				.andDo(print())
				.andExpect(status().isOk());

		// Set Build effectiveTime
		String jsonContent = "{ " + jsonPair(BuildService.EFFECTIVE_TIME, effectiveDate) + " }";
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

		jsonContent = "{ "
				+ jsonPair(PackageService.FIRST_TIME_RELEASE, Boolean.toString(isFirstTime))
				+ (isFirstTime ? "" : "," + jsonPair(PackageService.PREVIOUS_PUBLISHED_PACKAGE, previousPublishedFile))
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

		// Set Readme Header
		mockMvc.perform(
				request(HttpMethod.PATCH, packageURL)
						.header("Authorization", basicDigestHeaderValue)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{ \"readmeHeader\" : \"This is the readme.\\nTable of contents:\\n\" }")
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
				post(executionURL + "/trigger")
						.header("Authorization", basicDigestHeaderValue)
						.contentType(MediaType.APPLICATION_JSON)
		)
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(content().contentType(APPLICATION_JSON_UTF8));

		//Publish output
		mockMvc.perform(
				post(executionURL + "/output/publish")
						.header("Authorization", basicDigestHeaderValue)
						.contentType(MediaType.APPLICATION_JSON)
		)
				.andDo(print())
				.andExpect(status().isOk());
		//.andExpect(content().contentType(APPLICATION_JSON_UTF8));

		return executionURL;
	}

	private void testOutput(String executionURL, String expectedZipFilename, String expectedZipEntries) throws Exception {
		MvcResult outputFileListResult = mockMvc.perform(
				get(executionURL + "/packages/" + TEST_PACKAGE + "/outputfiles")
						.header("Authorization", basicDigestHeaderValue)
						.contentType(MediaType.APPLICATION_JSON)
		)
				.andDo(print())
				.andExpect(status().isOk())
				.andReturn();

		String outputFileListJson = outputFileListResult.getResponse().getContentAsString();
		JSONArray jsonArray = new JSONArray(outputFileListJson);
		Assert.assertEquals(5, jsonArray.length());

		String zipFilePath = null;
		for (int a = 0; a < jsonArray.length(); a++) {
			JSONObject file = (JSONObject) jsonArray.get(a);
			String filePath = file.getString("id");
			if (filePath.endsWith(".zip")) {
				zipFilePath = filePath;
			}
		}

		Assert.assertEquals(expectedZipFilename, zipFilePath);

		ZipFile zipFile = new ZipFile(downloadToTempFile(executionURL, zipFilePath));
		List<String> entryPaths = getZipEntryPaths(zipFile);
		Assert.assertEquals("Zip entries expected.",
				expectedZipEntries,
				entryPaths.toString().replace(", ", "\n").replace("[", "").replace("]", ""));
	}

	private File downloadToTempFile(String executionURL, String zipFilePath) throws Exception {
		MvcResult outputFileResult = mockMvc.perform(
				get(executionURL + "/packages/" + TEST_PACKAGE + "/outputfiles/" + zipFilePath)
						.header("Authorization", basicDigestHeaderValue)
						.contentType(MediaType.APPLICATION_JSON)
		)
				.andExpect(status().isOk())
				.andReturn();

		Path tempFile = Files.createTempFile(getClass().getCanonicalName(), "output-file");
		try (FileOutputStream fileOutputStream = new FileOutputStream(tempFile.toFile())) {
			fileOutputStream.write(outputFileResult.getResponse().getContentAsByteArray());
		}
		return tempFile.toFile();
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
				.andExpect(content().contentType(APPLICATION_JSON_UTF8))
				.andReturn();

		String publishedURL = JsonPath.read(productResult.getResponse().getContentAsString(), "$.published_url");
		String expectedURL = "http://localhost:80/centers/international/extensions/snomed_ct_international_edition/products/nlm_example_refset/published";

		Assert.assertEquals(expectedURL, publishedURL);

		//Recover list of published packages
		MvcResult publishedResult = mockMvc.perform(
				post(publishedURL)
						.header("Authorization", basicDigestHeaderValue)
						.contentType(MediaType.APPLICATION_JSON)
		)
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(content().contentType(APPLICATION_JSON_UTF8))
				.andReturn();

		return JsonPath.read(publishedResult.getResponse().getContentAsString(), "$.publishedPackages[0]");
	}

	private List<String> getZipEntryPaths(ZipFile zipFile) {
		List<String> entryPaths = new ArrayList();
		Enumeration<? extends ZipEntry> zipEntries = zipFile.entries();
		while (zipEntries.hasMoreElements()) {
			entryPaths.add(zipEntries.nextElement().getName());
		}
		return entryPaths;
	}

	/*
	 * @return a string formatted for use as a JSON key/value pair eg 	"\"effectiveTime\" : \""+ effectiveDate + "\","
	 * with a trailing comma just in case you want more than one and json is OK with that if there's only one
	 */
	private String jsonPair(String key, String value) {
		return "  \"" + key + "\" : \"" + value + "\" ";
	}

	@Override
	@Before
	public void setup() throws ServletException {
		super.setup();
		((TestS3Client) s3Client).deleteBuckets();
	}

}

package org.ihtsdo.buildcloud.controller.helper;

import com.jayway.jsonpath.JsonPath;
import org.ihtsdo.buildcloud.controller.AbstractControllerTest;
import org.ihtsdo.buildcloud.entity.helper.EntityHelper;
import org.ihtsdo.buildcloud.service.ProductService;
import org.ihtsdo.buildcloud.test.StreamTestUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class IntegrationTestHelper {

	public static final String CENTER_URL = "/centers/international";

	private final MockMvc mockMvc;
	private String basicDigestHeaderValue;
	private String productId;
	private final String productBusinessKey;

	public static final String COMPLETION_STATUS = "completed";
	protected static SecureRandom random = new SecureRandom();
	public IntegrationTestHelper(final MockMvc mockMvc, final String testName) {
		this.mockMvc = mockMvc;
		basicDigestHeaderValue = "NOT_YET_AUTHENTICATED"; // initial value only
		
		//We'll work with a product and package that are specific to this test
		this.productBusinessKey = EntityHelper.formatAsBusinessKey(testName + "_Product");
	}

	public void createTestProductStructure() throws Exception {
		this.productId = findOrCreateProduct();
	}
	
	private String findOrCreateProduct() throws Exception {
		// Does the product already exist or do we need to create it?
		MvcResult productResult = mockMvc.perform(
				get(CENTER_URL + "/products/" +  this.productBusinessKey)
						.header("Authorization", getBasicDigestHeaderValue())
		)
				.andDo(print())
				.andReturn();
		
		if (productResult.getResponse().getStatus() == 404) {
			// Create Product
			 productResult = mockMvc.perform(
					post(CENTER_URL + "/products")
							.header("Authorization", getBasicDigestHeaderValue())
							.contentType(MediaType.APPLICATION_JSON)
							.content("{ \"name\" : \"" + productBusinessKey + "\" }")
			)
					.andDo(print())
					.andExpect(status().isCreated())
					.andExpect(content().contentType(AbstractControllerTest.APPLICATION_JSON))
					.andReturn();
		}

		final String createProductResponseString = productResult.getResponse().getContentAsString();
		final int idStartIndex = createProductResponseString.indexOf("\"id\" :") + 8;
		final int idEndIndex = createProductResponseString.indexOf("\"", idStartIndex);
		return createProductResponseString.substring(idStartIndex, idEndIndex);
	}
	
	public void uploadDeltaInputFile(final String deltaFileName, final Class classpathResourceOwner) throws Exception {
		final InputStream resourceAsStream = classpathResourceOwner.getResourceAsStream(deltaFileName);
		Assert.assertNotNull(deltaFileName + " stream is null.", resourceAsStream);
		final MockMultipartFile deltaFile = new MockMultipartFile("file", deltaFileName, "text/plain", resourceAsStream);
		mockMvc.perform(
				fileUpload(getProductUrl() + "/inputfiles")
						.file(deltaFile)
						.header("Authorization", getBasicDigestHeaderValue())
		)
				.andDo(print())
				.andExpect(status().isCreated());
	}

	public void uploadSourceFile(final String sourceFileName, final String sourceName, final Class classpathResourceOwner) throws Exception {
		final InputStream resourceAsStream = classpathResourceOwner.getResourceAsStream(sourceFileName);
		Assert.assertNotNull(sourceFileName + " stream is null.", resourceAsStream);
		final MockMultipartFile deltaFile = new MockMultipartFile("file", sourceFileName, "text/plain", resourceAsStream);
		mockMvc.perform(
				fileUpload(getProductUrl() + "/sourcefiles/" + sourceName)
						.file(deltaFile)
						.header("Authorization", getBasicDigestHeaderValue())
		)
				.andDo(print())
				.andExpect(status().isCreated());
	}

	public void prepareSourceFile() throws Exception {
		mockMvc.perform(
				post(getProductUrl() + "/inputfiles/prepare")
						.header("Authorization", getBasicDigestHeaderValue())
						.contentType(MediaType.APPLICATION_JSON)
		).andDo(print()).andExpect(status().isNoContent());
	}

	public void deleteTxtSourceFiles() throws Exception {
		mockMvc.perform(
				request(HttpMethod.DELETE, getProductUrl() + "/sourcefiles")
						.param("pattern", "*.txt")
						.header("Authorization", getBasicDigestHeaderValue())
		).andDo(print()).andExpect(status().isNoContent());
	}

	public String listSourceFiles() throws Exception {
		MvcResult mvcResult = mockMvc.perform(
				get(getProductUrl() + "/sourcefiles")
		).andDo(print()).andExpect(status().isOk()).andReturn();
		return mvcResult.getResponse().getContentAsString();
	}

	public String getInputPrepareReport() throws Exception {
		MvcResult mvcResult = mockMvc.perform(
				get(getProductUrl() + "/inputfiles/prepareReport")
		).andDo(print()).andExpect(status().isOk()).andReturn();
		return mvcResult.getResponse().getContentAsString();
	}
	
	public void publishFile(final String publishFileName, final Class classpathResourceOwner, final HttpStatus expectedStatus) throws Exception {
		final MockMultipartFile publishFile = new MockMultipartFile("file", publishFileName, "text/plain", classpathResourceOwner.getResourceAsStream(publishFileName));
		mockMvc.perform(
				fileUpload(CENTER_URL + "/published")
						.file(publishFile)
						.header("Authorization", getBasicDigestHeaderValue())
		)
				.andDo(print())
				.andExpect(status().is(expectedStatus.value()));
	}
	
	
	public String getInputFile(String inputFileName) throws Exception {
		String getInputFileUrl = getProductUrl() + "/inputfiles/" + inputFileName;
		System.out.println(getInputFileUrl);
		MvcResult mvcResult = mockMvc.perform(
				request(HttpMethod.GET, getInputFileUrl)
						.header("Authorization", getBasicDigestHeaderValue()))
				.andDo(print())
				.andExpect(status().isOk()).andReturn();
		return mvcResult.getResponse().getContentAsString();
		
	}

	public void deletePreviousTxtInputFiles() throws Exception {
		mockMvc.perform(
				request(HttpMethod.DELETE, getProductUrl() + "/inputfiles/*.txt")
						.header("Authorization", getBasicDigestHeaderValue())
		)
				.andDo(print())
				.andExpect(status().isNoContent());
	}

	public void uploadManifest(final String manifestFileName, final Class classpathResourceOwner) throws Exception {
		final MockMultipartFile manifestFile = new MockMultipartFile("file", manifestFileName, "text/plain", classpathResourceOwner.getResourceAsStream(manifestFileName));
		mockMvc.perform(
				fileUpload(getProductUrl() + "/manifest")
						.file(manifestFile)
						.header("Authorization", getBasicDigestHeaderValue())
		)
				.andDo(print())
				.andExpect(status().isCreated());
	}

	public void setEffectiveTime(final String effectiveDate) throws Exception {
		final String jsonContent = "{ " + jsonPair(ProductService.EFFECTIVE_TIME, getEffectiveDateWithSeparators(effectiveDate)) + " }";
		mockMvc.perform(
				request(HttpMethod.PATCH, getProductUrl())
						.header("Authorization", getBasicDigestHeaderValue())
						.contentType(MediaType.APPLICATION_JSON)
						.content(jsonContent)
		)
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(content().contentType(AbstractControllerTest.APPLICATION_JSON));
	}

	private String getEffectiveDateWithSeparators(final String effectiveDate) {
		return effectiveDate.substring(0, 4) + "-" + effectiveDate.substring(4, 6) + "-" + effectiveDate.substring(6, 8);
	}

	public void setFirstTimeRelease(final boolean isFirstTime) throws Exception {
		setProductProperty("{ " + jsonPair(ProductService.FIRST_TIME_RELEASE, Boolean.toString(isFirstTime)) + " }");
	}

	
	public void setBetaRelease(final boolean isBeta) throws Exception {
		setProductProperty("{ " + jsonPair(ProductService.BETA_RELEASE, Boolean.toString(isBeta)) + " }");
	}
	
	public void setCreateLegacyIds(final boolean createLegacyIds) throws Exception {
		setProductProperty("{ " + jsonPair(ProductService.CREATE_LEGACY_IDS, Boolean.toString(createLegacyIds)) + " }");
	}

	public void setWorkbenchDataFixesRequired(final boolean isWorkbenchDataFixesRequired) throws Exception {
		setProductProperty("{ " + jsonPair(ProductService.WORKBENCH_DATA_FIXES_REQUIRED, Boolean.toString(isWorkbenchDataFixesRequired))
				+ " }");
	}

	public void setJustPackage(final boolean justPackage) throws Exception {
		setProductProperty("{ " + jsonPair(ProductService.JUST_PACKAGE, Boolean.toString(justPackage)) + " }");
	}

	public void setPreviousPublishedPackage(final String previousPublishedFile) throws Exception {
		setProductProperty("{ " + jsonPair(ProductService.PREVIOUS_PUBLISHED_PACKAGE, previousPublishedFile) + " }");
	}

	public void setReadmeHeader(final String readmeHeader) throws Exception {
		setProductProperty("{ \"readmeHeader\" : \"" + readmeHeader + "\" }");
	}

	public void setReadmeEndDate(final String readmeEndDate) throws Exception {
		setProductProperty("{ \"readmeEndDate\" : \"" + readmeEndDate + "\" }");
	}

	public void setCustomRefsetCompositeKeys(final String customRefsetCompositeKeys) throws Exception {
		setProductProperty("{ \"" + ProductService.CUSTOM_REFSET_COMPOSITE_KEYS + "\" : \"" + customRefsetCompositeKeys + "\" }");
	}

	public void setNewRF2InputFiles(final String newRF2InputFiles) throws Exception {
		setProductProperty("{ \"" + ProductService.NEW_RF2_INPUT_FILES + "\" : \"" + newRF2InputFiles + "\" }");
	}
	
	public void setAssertionTestConfigProperty( final String name, final String value) throws Exception {
		setProductProperty("{ \"" + name + "\" : \"" + value + "\" }");
	}

	/*
		 * @return a string formatted for use as a JSON key/value pair eg 	"\"effectiveTime\" : \""+ effectiveDate + "\","
		 * with a trailing comma just in case you want more than one and json is OK with that if there's only one
		 */
	private String jsonPair(final String key, final String value) {
		return "  \"" + key + "\" : \"" + value + "\" ";
	}

	private void setProductProperty(final String jsonContent) throws Exception {
		mockMvc.perform(
				request(HttpMethod.PATCH, getProductUrl())
						.header("Authorization", getBasicDigestHeaderValue())
						.contentType(MediaType.APPLICATION_JSON)
						.content(jsonContent)
		)
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(content().contentType(AbstractControllerTest.APPLICATION_JSON));
	}

	public String createBuild() throws Exception {
		final MvcResult createBuildResult = mockMvc.perform(
				post(getProductUrl() + "/builds")
						.header("Authorization", getBasicDigestHeaderValue())
						.contentType(MediaType.APPLICATION_JSON)
		)
				.andDo(print())
				.andExpect(status().isCreated())
				.andExpect(content().contentType(AbstractControllerTest.APPLICATION_JSON))
				.andReturn();

		final String buildId = JsonPath.read(createBuildResult.getResponse().getContentAsString(), "$.id");
		return getProductUrl() + "/builds/" + buildId;
	}

	public void triggerBuild(final String buildURL) throws Exception {
		final MvcResult triggerResult = mockMvc.perform(
				post(buildURL + "/trigger")
						.header("Authorization", getBasicDigestHeaderValue())
						.contentType(MediaType.APPLICATION_JSON)
		)
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(content().contentType(AbstractControllerTest.APPLICATION_JSON))
				.andReturn();

		final String outputFileListJson = triggerResult.getResponse().getContentAsString();
		final JSONObject jsonObject = new JSONObject(outputFileListJson);
		final JSONObject buildReport = jsonObject.getJSONObject("buildReport");
		final String status = buildReport.getString("Progress Status");
		final String message = buildReport.getString("Message");
		Assert.assertEquals("Build bad status. Message: " + message, COMPLETION_STATUS, status);
	}

	public void triggerBuildAndGotCancelled(final String buildURL) throws Exception {
		final MvcResult triggerResult = mockMvc.perform(
				post(buildURL + "/trigger")
						.header("Authorization", getBasicDigestHeaderValue())
						.contentType(MediaType.APPLICATION_JSON)
		)
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(content().contentType(AbstractControllerTest.APPLICATION_JSON))
				.andReturn();

		final String outputFileListJson = triggerResult.getResponse().getContentAsString();
		final JSONObject jsonObject = new JSONObject(outputFileListJson);
		final JSONObject buildReport = jsonObject.getJSONObject("buildReport");
		final String status = buildReport.getString("Progress Status");
		final String message = buildReport.getString("Message");
		Assert.assertEquals("Build bad status. Message: " + message, "cancelled", status);
	}

	public void publishOutput(final String buildURL) throws Exception {
		mockMvc.perform(
				post(buildURL + "/publish")
						.header("Authorization", getBasicDigestHeaderValue())
						.contentType(MediaType.APPLICATION_JSON)
		)
				.andDo(print())
				.andExpect(status().isOk());
	}

	public List<String> getZipEntryPaths(final ZipFile zipFile) {
		final List<String> entryPaths = new ArrayList<>();
		final Enumeration<? extends ZipEntry> zipEntries = zipFile.entries();
		while (zipEntries.hasMoreElements()) {
			entryPaths.add(zipEntries.nextElement().getName());
		}
		return entryPaths;
	}

	public String getPreviousPublishedPackage() throws Exception {

		//Recover URL of published things from Product
		final MvcResult productResult = mockMvc.perform(
				get(CENTER_URL)
						.header("Authorization", getBasicDigestHeaderValue())
						.contentType(MediaType.APPLICATION_JSON)
		)
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(content().contentType(AbstractControllerTest.APPLICATION_JSON))
				.andReturn();

		final String publishedURL = JsonPath.read(productResult.getResponse().getContentAsString(), "$.published_url");
		final String expectedURL = "http://localhost/centers/international/published";

		Assert.assertEquals(expectedURL, publishedURL);

		//Recover list of published packages
		final MvcResult publishedResult = mockMvc.perform(
				get(publishedURL)
						.header("Authorization", getBasicDigestHeaderValue())
						.contentType(MediaType.APPLICATION_JSON)
		)
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(content().contentType(AbstractControllerTest.APPLICATION_JSON))
				.andReturn();

		return JsonPath.read(publishedResult.getResponse().getContentAsString(), "$.publishedPackages[0]");
	}

	public ZipFile testZipNameAndEntryNames(final String buildURL, final String expectedZipFilename, final String expectedZipEntries, final Class classpathResourceOwner) throws Exception {
		final MvcResult outputFileListResult = mockMvc.perform(
				get(buildURL + "/outputfiles")
						.header("Authorization", getBasicDigestHeaderValue())
						.contentType(MediaType.APPLICATION_JSON)
		)
				.andDo(print())
				.andExpect(status().isOk())
				.andReturn();

		final String outputFileListJson = outputFileListResult.getResponse().getContentAsString();
		final JSONArray jsonArray = new JSONArray(outputFileListJson);

		String zipFilePath = null;
		for (int a = 0; a < jsonArray.length(); a++) {
			final JSONObject file = (JSONObject) jsonArray.get(a);
			final String filePath = file.getString("id");
			if (filePath.endsWith(".zip")) {
				zipFilePath = filePath;
			}
		}

		Assert.assertEquals(expectedZipFilename, zipFilePath);

		final ZipFile zipFile = new ZipFile(downloadToTempFile(buildURL, zipFilePath, classpathResourceOwner));
		final List<String> entryPaths = getZipEntryPaths(zipFile);
		Assert.assertEquals("Zip entries expected.",
				expectedZipEntries,
				entryPaths.toString().replace(", ", "\n").replace("[", "").replace("]", ""));
		return zipFile;
	}

	private File downloadToTempFile(final String buildURL, final String zipFilePath, final Class classpathResourceOwner) throws Exception {
		final MvcResult outputFileResult = mockMvc.perform(
				get(buildURL + "/outputfiles/" + zipFilePath)
						.header("Authorization", getBasicDigestHeaderValue())
						.contentType(MediaType.APPLICATION_JSON)
		)
				.andExpect(status().isOk())
				.andReturn();

		final Path tempFile = Files.createTempFile(classpathResourceOwner.getCanonicalName(), "output-file" );
		try (FileOutputStream fileOutputStream = new FileOutputStream(tempFile.toFile());
			 FileOutputStream tmp = new FileOutputStream("/tmp/zip.zip")) {
			final byte[] contentAsByteArray = outputFileResult.getResponse().getContentAsByteArray();
			fileOutputStream.write(contentAsByteArray);
			tmp.write(contentAsByteArray);
		}
		return tempFile.toFile();
	}

	public void assertZipContents(final String expectedOutputPackageName, final ZipFile zipFile, final Class classpathResourceOwner) throws IOException {
		assertZipContents(expectedOutputPackageName, zipFile, classpathResourceOwner, false);
	}

	private String getFileName(final ZipEntry zipEntry) {
		final String name = zipEntry.getName();
		return name.substring(name.lastIndexOf("/") + 1);
	}

	public String getBasicDigestHeaderValue() {
		return basicDigestHeaderValue;
	}

	public String getProductUrl() {
		return CENTER_URL + "/products/" + productId;
	}

	public void configureAssertionGroups(final Map<String, String> assertionConfig, final String ... groupNames) {
		
	}

	public void assertZipContents(String expectedOutputPackageName, ZipFile zipFile, Class classpathResourceOwner, boolean isBeta) throws IOException {
		final Enumeration<? extends ZipEntry> entries = zipFile.entries();
		while (entries.hasMoreElements()) {
			final ZipEntry zipEntry = entries.nextElement();
			if (!zipEntry.isDirectory()) {
				 String zipEntryName = getFileName(zipEntry);
				if (isBeta) {
					if (zipEntryName.contains("Readme_")) {
						zipEntryName = zipEntryName.replace(".txt", "_beta.txt");
					} else {
						zipEntryName = zipEntryName.substring(1);
					}
				}
				StreamTestUtils.assertStreamsEqualLineByLine(
						zipEntryName,
						classpathResourceOwner.getResourceAsStream(expectedOutputPackageName + "/" + zipEntryName),
						zipFile.getInputStream(zipEntry));
			}
		}
	}

	public void printBuildConfig(String buildURL) throws Exception {
		final MvcResult getBuildConfig = mockMvc.perform(
				get(buildURL + "/configuration")
						.header("Authorization", getBasicDigestHeaderValue())
						.contentType(MediaType.APPLICATION_JSON)
		)
				.andDo(print())
				.andReturn();
	}

	public int cancelBuild(String buildURL) throws Exception {
		final MvcResult cancelBuildResult = mockMvc.perform(
				post(buildURL + "/cancel")
						.header("Authorization", getBasicDigestHeaderValue())
						.contentType(MediaType.APPLICATION_JSON)
		)
				.andDo(print())
				.andReturn();
		return cancelBuildResult.getResponse().getStatus();
	}

}

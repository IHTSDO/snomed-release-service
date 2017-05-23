package org.ihtsdo.buildcloud.service.srs;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.ihtsdo.otf.rest.client.resty.HttpEntityContent;
import org.ihtsdo.otf.rest.client.resty.RestyHelper;
import org.ihtsdo.otf.rest.client.resty.RestyServiceHelper;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.otf.rest.exception.ProcessingException;
import org.ihtsdo.otf.rest.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import us.monoid.web.Content;
import us.monoid.web.JSONResource;
import us.monoid.web.Resty;
import us.monoid.web.mime.MultipartContent;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class SRSRestClient {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	protected static final String CONTENT_TYPE_ANY = "*/*";
	protected static final String CONTENT_TYPE_XML = "text/xml";
	protected static final String CONTENT_TYPE_JSON = "application/json";
	protected static final String CONTENT_TYPE_MULTIPART = "multipart/form-data";
	protected static final String CONTENT_TYPE_TEXT = "text/plain;charset=UTF-8";

	private static final String BLANK_MANIFEST = "/manifest_no_date.xml";
	private static final String DATE_MARKER = "########";
	private static final String MANIFEST_ENDPOINT = "manifest";
	private static final String INPUT_FILES_ENDPOINT = "inputfiles";
	private static final String DELETE_FILTER = "/*.txt";
	private static final String BUILD_ENDPOINT = "builds";
	private static final String AUTHENTICATE_ENDPOINT = "login";
	private static final String TRIGGER_BUILD_ENDPOINT = "/trigger";
	private static final String PRODUCT_CONFIGURATION_ENDPOINT = "/configuration";
	private static final String AUTHENTICATION_TOKEN = "authenticationToken";
	private static final String PRODUCT_PREFIX = "centers/{releaseCenter}/products/";
	private static final String ID = "id";
	private static final char[] BLANK_PASSWORD = "".toCharArray();
	private static final byte[] EMPTY_CONTENT_ARRAY = new byte[0];
	private static final Content EMPTY_CONTENT = new Content(CONTENT_TYPE_TEXT, EMPTY_CONTENT_ARRAY);

	private static final Integer NOT_FOUND = new Integer(HttpStatus.SC_NOT_FOUND);

	@Autowired
	protected SRSFileDAO srsDAO;
	
	@Autowired
	FileManager fileManager;

	private final String srsRootURL;
	private final String username;
	private final String password;

	private final RestyHelper resty;
	private boolean restyInitiated = false;

	public static final String RVF_RESPONSE = "buildReport.rvf_response";
	protected static final String[] ITEMS_OF_INTEREST = { "outputfiles_url", "status", "logs_url", RVF_RESPONSE, "buildReport.Message" };

	public SRSRestClient(String srsRootURL, String username, String password) {
		this.srsRootURL = srsRootURL;
		this.username = username;
		this.password = password;
		this.resty = new RestyHelper(CONTENT_TYPE_ANY);
	}

	/**
	 * Constructor to use for local isolated testing.
	 */
	public SRSRestClient() {
		srsRootURL = null;
		username = null;
		password = null;
		resty = null;
	}

	// Pulled out this section so it can be tested in isolation from Jira Issue
	public void prepareSRSFiles(File exportArchive, SRSProjectConfiguration config, boolean includeExternallyMaintainedFiles)
			throws Exception {
		if (config.getReleaseDate() == null) {
			String releaseDate = srsDAO.recoverReleaseDate(exportArchive);
			config.setReleaseDate(releaseDate);
		}
		File inputFilesDir = srsDAO.readyInputFiles(exportArchive, config.getReleaseCenter(), config.getReleaseDate(),
				includeExternallyMaintainedFiles);
		config.setInputFilesDir(inputFilesDir);
		//Tell the file manager that we have an interest in that directory so it's not deleted until all interested processes have released
		fileManager.addProcess(inputFilesDir);
	}

	private void initiateRestyIfNeeded() throws BusinessServiceException {
		if (!restyInitiated) {
			// Authentication first
			MultipartContent credentials = Resty.form(Resty.data("username", username), Resty.data("password", password));
			Object authToken = null;
			try {
				JSONResource json = resty.json(srsRootURL + AUTHENTICATE_ENDPOINT, credentials);
				authToken = json.get(AUTHENTICATION_TOKEN);
				if (authToken == null) {
					throw new BusinessServiceException("null authentication token received, no further information available.");
				}
			} catch (Exception e) {
				String error = "Failed to recover SRS Authentication at " + srsRootURL + AUTHENTICATE_ENDPOINT + " using "
						+ credentials.toString();
				throw new BusinessServiceException(error, e);
			}

			// Now the token received can be set as the username for all subsequent interactions. Blank password.
			resty.authenticate(srsRootURL, authToken.toString(), BLANK_PASSWORD);
			restyInitiated = true;
		}
	}

	private String getProductUrl(String productName, String releaseCenter) {
		return srsRootURL + PRODUCT_PREFIX.replace("{releaseCenter}", releaseCenter) + formatAsBusinessKey(productName) + "/";

	}

	public void configureBuild(SRSProjectConfiguration config) throws Exception {
		initiateRestyIfNeeded();

		// Lets upload the manifest first
		File configuredManifest = configureManifest(config.getReleaseDate());
		String srsProductURL = getProductUrl(config.getProductName(), config.getReleaseCenter());

		// Check the product exists and perform standard configuration if needed
		checkProductExists(config.getProductName(), config.getReleaseCenter(), true);

		uploadFile(srsProductURL + MANIFEST_ENDPOINT, configuredManifest);
		configuredManifest.delete();

		// Configure the product
		JSONObject productConfig = config.getJson();
		logger.debug("Configuring SRS Product with " + productConfig.toString(2));
		resty.put(srsProductURL + PRODUCT_CONFIGURATION_ENDPOINT, productConfig, CONTENT_TYPE_JSON);
	}

	public Map<String, String> runBuild(SRSProjectConfiguration config) throws Exception {
		Assert.notNull(config.getProductName());
		logger.info("Running {} build for {} with files uploaded from: {}", config.getProductName(), config.getReleaseDate(), config
				.getInputFilesDir().getAbsolutePath());

		initiateRestyIfNeeded();
		String srsProductURL = getProductUrl(config.getProductName(),config.getReleaseCenter());

		// Delete any previously uploaded input files
		logger.debug("Deleting previous input files");
		resty.json(srsProductURL + INPUT_FILES_ENDPOINT + DELETE_FILTER, Resty.delete());

		// Now everything in the target directory
		uploadFiles(config.getInputFilesDir(), srsProductURL + INPUT_FILES_ENDPOINT);
		// And we unregister our interest in that directory
		fileManager.removeProcess(config.getInputFilesDir());

		// Create a build. Pass blank content to encourage Resty to use POST
		JSONResource json = resty.json(srsProductURL + BUILD_ENDPOINT, EMPTY_CONTENT);
		Object buildId = json.get(ID);
		Assert.notNull(buildId, "Failed to recover create build at: " + srsProductURL);

		// We're now telling the RVF (via the SRS) how many failures we want to see for each assertion
		String failureExportMaxStr = "";
		if (config.getFailureExportMax() != null && !config.getFailureExportMax().isEmpty()) {
			failureExportMaxStr = "?failureExportMax=" + config.getFailureExportMax();
		}

		// Trigger Build
		String buildTriggerURL = srsProductURL + BUILD_ENDPOINT + "/" + buildId + TRIGGER_BUILD_ENDPOINT + failureExportMaxStr;
		logger.debug("Triggering Build: {}", buildTriggerURL);
		json = resty.json(buildTriggerURL, EMPTY_CONTENT);
		try {
			logger.debug("Build trigger returned: {}", json.object().toString(2));
		} catch (Exception e) {
			String msg = "Unable to parse response from build trigger.";
			logger.error(msg, e);
			logger.error("Build trigger returned status {}", json.getHTTPStatus());
			throw new BusinessServiceException(msg, e);
		}

		return recoverItemsOfInterest(json);
	}

	public void checkProductExists(String productName, String releaseCenter, boolean createIfRequired) throws IOException,
			JSONException,
            BusinessServiceException {
		String srsProductRootUrl = srsRootURL + PRODUCT_PREFIX.replace("{releaseCenter}", releaseCenter);
		String srsProductURL = getProductUrl(productName, releaseCenter);
		// Try to recover the product
		Integer httpStatus = resty.json(srsProductURL).getHTTPStatus();
		if (httpStatus != null && httpStatus.equals(NOT_FOUND)) {
			if (createIfRequired) {
				// Create the product by POSTing to the root with the name in a json object
				JSONObject obj = new JSONObject();
				obj.put("name", productName);
				JSONResource json = resty.json(srsProductRootUrl, obj, CONTENT_TYPE_JSON);
				RestyServiceHelper.ensureSuccessfull(json);
			} else {
				throw new ResourceNotFoundException("Product " + productName + " does not exist in SRS via URL:" + srsProductRootUrl );
			}
		}
	}

	protected Map<String, String> recoverItemsOfInterest(JSONResource json) throws Exception {
		// Recover some things the users might be interested in, to store in the Jira Ticket
		Map<String, String> response = new HashMap<String, String>();
		int itemsFound = 0;
		for (String item : ITEMS_OF_INTEREST) {
			try {
				Object value = json.get(item);
				response.put(item, value.toString());
				itemsFound++;
			} catch (Exception e) {
				logger.error("Failed to recover item of interest from SRS Trigger Response: {} ", item, e);
			}
		}

		if (itemsFound < ITEMS_OF_INTEREST.length) {
			logger.warn("Items of interest issues encountered with JSON: {}", json.object().toString(2));
		}

		if (itemsFound == 0) {
			response.put("Error", "Failed to recover any items of interest from the SRS Response");
		}
		return response;
	}

	private void uploadFiles(File srsFilesDir, String url) throws ProcessingException {
		Assert.isTrue(srsFilesDir.isDirectory(), srsFilesDir.getAbsolutePath() + " must be a directory in order to use it to upload files.");
		for (File thisFile : srsFilesDir.listFiles()) {
			if (thisFile.exists() && !thisFile.isDirectory()) {
				uploadFile(url, thisFile);
			}
		}
	}

	private void uploadFile(String url, File file) throws ProcessingException {

		Assert.isTrue(file.exists(), "File for upload to " + url + " was found to not exist at location: " + file.getAbsolutePath());
		Assert.isTrue(!file.isDirectory(), "File for upload to " + url + " was found to be a directory: " + file.getAbsolutePath());
		try {
			logger.debug("Uploading file to {} : {} ", url, file.getAbsolutePath());
			MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create();
			multipartEntityBuilder.addBinaryBody("file", file, ContentType.create(CONTENT_TYPE_MULTIPART), file.getName());
			multipartEntityBuilder.setCharset(Charset.forName("UTF-8"));
			// multipartEntityBuilder.addPart("file", new FileBody(file));
			multipartEntityBuilder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
			HttpEntity httpEntity = multipartEntityBuilder.build();
			resty.withHeader("Accept", CONTENT_TYPE_ANY);
			JSONResource response = resty.json(url, new HttpEntityContent(httpEntity));
			RestyServiceHelper.ensureSuccessfull(response);
		} catch (Exception e) {
			throw new ProcessingException("Failed to upload " + file.getAbsolutePath(), e);
		}
	}

	protected File configureManifest(String releaseDate) throws IOException {
		// We need to build a manifest file containing the target release date
		InputStream is = getClass().getResourceAsStream(BLANK_MANIFEST);
		Assert.notNull(is, "Failed to load blank manifest.");
		String content = IOUtils.toString(is, StandardCharsets.UTF_8);
		content = content.replaceAll(DATE_MARKER, releaseDate);
		File configuredManifest = File.createTempFile("manifest_" + releaseDate, ".xml");
		Files.write(configuredManifest.toPath(), content.getBytes(StandardCharsets.UTF_8));
		return configuredManifest;
	}

	// TODO This is pinched from EntityHelper. Discuss moving to OTFCommon
	public static String formatAsBusinessKey(String name) {
		String businessKey = null;
		if (name != null) {
			businessKey = name.toLowerCase().replace(" ", "_").replaceAll("[^a-zA-Z0-9_]", "");
		}
		return businessKey;
	}

}

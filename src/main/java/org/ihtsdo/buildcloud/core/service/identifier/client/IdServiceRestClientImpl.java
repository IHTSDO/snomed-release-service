package org.ihtsdo.buildcloud.core.service.identifier.client;

import com.google.gson.*;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.util.Timeout;
import org.ihtsdo.otf.rest.client.ExpressiveErrorHandler;
import org.ihtsdo.otf.rest.client.RestClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.json.GsonHttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class IdServiceRestClientImpl implements IdServiceRestClient {

	private static final String COMMENT = "comment";
	private static final String FALSE = "false";
	private static final String GENERATE_LEGACY_IDS = "generateLegacyIds";
	private static final String ITEMS = "items";
	private static final String LOG_JOB_WITH_SIZE = "Bulk job id: {} with batch size: {}";
	private static final String LOG_TIME_TAKEN = "Time taken in seconds: {}";
	private static final String MESSAGE = "message";
	private static final String NAMESPACE = "namespace";
	private static final String PARTITION_ID = "partitionId";
	private static final String QUANTITY = "quantity";
	private static final String SCHEME_ID = "schemeId";
	private static final String SCHEME_IDS = "schemeIds";
	private static final String SCTID = "sctid";
	private static final String SCTIDS = "sctids";
	private static final String SOFTWARE = "software";
	private static final String STATUS = "status";
	private static final String SYSTEM_ID = "systemId";
	private static final String SYSTEM_IDS = "systemIds";
	private static final String SRS = "srs";
	private static final String TOKEN_STR = "token";

	private static final String JSON_CONTENT_TYPE = "application/json";

	private final String idServiceUrl;
	private final Gson gson;
	private final IdServiceRestUrlHelper urlHelper;
	private static String token;
	private static final Object LOCK = new Object();
	private static final Logger LOGGER = LoggerFactory.getLogger(IdServiceRestClientImpl.class);

	private final HttpHeaders headers;
	private final RestTemplate restTemplate;
	
	private static final AtomicInteger currentSessions = new AtomicInteger();

	private final String userName;
	private final String password;

	@Value("${cis.timeoutInSeconds:300}")
	private int timeOutInSeconds;

	@Value("${cis.maxTries:3}")
	private int maxTries;

	@Value("${cis.retryDelaySeconds:30}")
	private int retryDelaySeconds;

	@Value("${cis.batchSize:500}")
	private int batchSize;
	
	public IdServiceRestClientImpl(@Value("${cis.url}") final String idServiceUrl,
			@Value("${cis.username}") final String username,
			@Value("${cis.password}") final String password) {
		this.idServiceUrl = idServiceUrl;
		urlHelper = new IdServiceRestUrlHelper(idServiceUrl);
		this.userName = username;
		this.password = password;
		gson = new GsonBuilder().setPrettyPrinting().create();

		headers = new HttpHeaders();
		headers.add("Accept", JSON_CONTENT_TYPE);
		headers.add("Content-Type", JSON_CONTENT_TYPE);

		RequestConfig requestConfig = RequestConfig.custom()
				.setConnectionRequestTimeout(Timeout.ofSeconds(10))
				.setResponseTimeout(Timeout.ofMinutes(5))
				.build();

		CloseableHttpClient httpClient = HttpClients.custom()
				.setDefaultRequestConfig(requestConfig)
				.build();

		HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);

		restTemplate = new RestTemplateBuilder()
				.rootUri(idServiceUrl)
				.additionalMessageConverters(new GsonHttpMessageConverter(gson))
				.additionalMessageConverters(new FormHttpMessageConverter())
				.errorHandler(new ExpressiveErrorHandler())
				.requestFactory(() -> factory)
				.build();
	}

	public int getTimeOutInSeconds() {
		return timeOutInSeconds;
	}

	private boolean isServiceRunning() {
		try {
			ResponseEntity<Void> response =
					restTemplate.exchange(urlHelper.getTestServiceUrl(), HttpMethod.GET, null, Void.class);

			return response.getStatusCode().is2xxSuccessful();
		} catch (Exception e) {
			LOGGER.error("Error when testing service", e);
			return false;
		}
	}

	@Override
	public String logIn() throws RestClientException {
		int attempt = 1;
		while (true) {
			 try {
				 synchronized (LOCK) {
					 if (token != null) {
						 LOGGER.debug("ID service rest client is already logged in.");
					 } 
					 if (!isTokenValid(token) ) {
						 token = accquireToken();
					 }
					 currentSessions.getAndIncrement();
					 break;
				}
			} catch (Exception e) {
				 attempt++;
				 sleepAndRetry("log into the IdService", attempt, e);
			}
		}
		return token;
	}

	private void sleepAndRetry(String action, int attempt, Exception e) throws RestClientException {
		if (attempt <= maxTries) {
			LOGGER.warn("Failed to {} on attempt {}. Waiting {} seconds before retrying.", action, attempt, retryDelaySeconds, e);
			try {
				Thread.sleep(retryDelaySeconds * 1000L);
			} catch (InterruptedException ie) {
				Thread.currentThread().interrupt();
				throw new RestClientException("Sleep interrupted during retry for action: " + action, ie);
			}
		} else {
			String msg = String.format("Still failed to %s after %s attempts", action, attempt);
			throw new RestClientException(msg, e);
		}
	}

	private String accquireToken() throws RestClientException {
		if (!isServiceRunning()) {
			throw new RestClientException("Id service is not currently running at URL:" + idServiceUrl);
		}

		LOGGER.info("Id service rest client logs in to get a new security token.");

		try {
			// Prepare JSON body as a JsonObject
			JsonObject request = new JsonObject();
			request.addProperty("username", this.userName);
			request.addProperty("password", this.password);

			// Send POST request, get response as JsonObject
			HttpEntity<String> httpRequest = new HttpEntity<>(gson.toJson(request), headers);
			String responseBody = restTemplate.postForObject(urlHelper.getLoginUrl(), httpRequest, String.class);

			JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
			if (!jsonResponse.has(TOKEN_STR)) {
				throw new RestClientException("Login response did not contain token");
			}

			String securityToken = jsonResponse.get(TOKEN_STR).getAsString();
			LOGGER.debug("Security token is acquired successfully.");
			return securityToken;

		} catch (Exception e) {
			throw new RestClientException("Failed to login for user: " + this.userName, e);
		}
	}


	private boolean isTokenValid(String token) {
		if (token == null) {
			return false;
		}

		try {
			JsonObject request = new JsonObject();
			request.addProperty(TOKEN_STR, token);

			HttpEntity<String> httpRequest = new HttpEntity<>(gson.toJson(request), headers);
			ResponseEntity<JsonObject> response = restTemplate.exchange(
					urlHelper.getTokenAuthenticationUrl(),
					HttpMethod.POST,
					httpRequest,
					JsonObject.class
			);

			if (response.getStatusCode().is2xxSuccessful()) {
				return true;
			} else {
				JsonObject body = response.getBody();
				String message = (body != null && body.has(MESSAGE)) ? body.get(MESSAGE).getAsString() : "<no message>";
				LOGGER.info("Invalid token with failure reason from id server: {}", message);
			}

		} catch (Exception e) {
			LOGGER.error("Failed to log in", e);
		}

		return false;
	}


	@Override
	public Map<Long, String> getStatusForSctIds(Collection<Long> sctIds) throws RestClientException {
		if (sctIds == null || sctIds.isEmpty()) {
			return new HashMap<>();
		}

		String scdStrList = sctIds.stream()
				.map(Object::toString)
				.collect(Collectors.joining(","));

		Map<Long, String> result = new HashMap<>();
		int attempt = 1;
		while (attempt <= maxTries) {
			try {
				fetchSctIdStatusBatch(scdStrList, result);
				break; // Successfully fetched, exit loop
			} catch (Exception e) {
				attempt++;
				String action = "get sctIds for batch size:" + sctIds.size();
				sleepAndRetry(action, attempt, e);
			}
		}


		return result;
	}

	private void fetchSctIdStatusBatch(String scdStrList, Map<Long, String> result) throws RestClientException {
		try {
			JsonObject requestData = new JsonObject();
			requestData.addProperty(SCTIDS, scdStrList);

			HttpEntity<String> request = new HttpEntity<>(gson.toJson(requestData), headers);
			ResponseEntity<JsonObject> response = restTemplate.postForEntity(urlHelper.getSctIdBulkUrl(token), request, JsonObject.class);

			if (!response.getStatusCode().is2xxSuccessful()) {
				throw new RestClientException(getFailureMessage(response));
			}

			JsonObject body = response.getBody();
			if (body != null && body.has(ITEMS)) {
				for (var element : body.getAsJsonArray(ITEMS)) {
					JsonObject item = element.getAsJsonObject();
					long sctId = Long.parseLong(item.get(SCTID).getAsString());
					String status = item.get(STATUS).getAsString();
					result.put(sctId, status);
				}
			}

		} catch (Exception e) {
			throw new RestClientException("Error fetching SCT ID status batch", e);
		}
	}

	@Override
	public Long getOrCreateSctId(UUID componentUuid,
	                             Integer namespaceId,
	                             String partitionId,
	                             String comment) throws RestClientException {
		Long result = null;
		int attempt = 1;

		while (attempt <= maxTries) {
			try {
				result = executeGetOrCreateSctId(componentUuid, namespaceId, partitionId, comment);
				break; // Explicit end condition
			} catch (Exception e) {
				attempt++;
				String action = "create sctId for uuid: " + componentUuid;
				sleepAndRetry(action, attempt, e);
			}
		}
		return result;
	}

	/**
	 * Executes a single attempt to get or create the SCT ID via the ID service.
	 */
	private Long executeGetOrCreateSctId(UUID componentUuid,
	                                     Integer namespaceId,
	                                     String partitionId,
	                                     String comment) throws RestClientException {

		try {
			// Build request JSON
			JsonObject requestJson = new JsonObject();
			requestJson.addProperty(NAMESPACE, namespaceId);
			requestJson.addProperty(PARTITION_ID, partitionId);
			requestJson.addProperty(SYSTEM_ID, componentUuid.toString());
			requestJson.addProperty(SOFTWARE, SRS);
			requestJson.addProperty(GENERATE_LEGACY_IDS, FALSE);
			requestJson.addProperty(COMMENT, comment);

			HttpEntity<String> requestEntity = new HttpEntity<>(gson.toJson(requestJson), headers);

			// Call the ID service
			ResponseEntity<String> response = restTemplate.postForEntity(
					urlHelper.getSctIdGenerateUrl(token),
					requestEntity,
					String.class
			);

			if (!response.getStatusCode().is2xxSuccessful()) {
				throw new RestClientException("ID service returned status " + response.getStatusCode()
				);
			}

			// Parse JSON response
			JsonObject body = JsonParser.parseString(response.getBody()).getAsJsonObject();
			if (!body.has(SCTID)) {
				throw new RestClientException("ID service response missing SCTID field");
			}

			return Long.valueOf(body.get(SCTID).getAsString());

		} catch (RestClientException e) {
			throw e; // propagate explicitly thrown exceptions
		} catch (Exception e) {
			throw new RestClientException("Error while executing getOrCreateSctId", e);
		}
	}


	@Override
	public HashMap<UUID, Long> getOrCreateSctIds(List<UUID> uuids,
	                                             Integer namespaceId,
	                                             String partitionId,
	                                             String comment) throws RestClientException {
		LOGGER.debug("Start creating sctIds with batch size {} for namespace {} and partitionId {}", uuids.size(), namespaceId, partitionId);

		HashMap<UUID, Long> result = new HashMap<>();
		if (uuids.isEmpty()) {
			LOGGER.warn("Empty UUIDs submitted for requesting sctIds");
			return result;
		}

		long startTime = System.currentTimeMillis();
		List<String> batchJob = new ArrayList<>();
		int counter = 0;

		for (UUID uuid : uuids) {
			batchJob.add(uuid.toString());
			counter++;

			if (counter % batchSize == 0 || counter == uuids.size()) {
				// Execute batch as separate method
				processSctIdBatch(batchJob, namespaceId, partitionId, comment, result);
				batchJob.clear();
			}
		}

		LOGGER.debug("End creating sctIds with batch size {} for namespace {} and partitionId {}", uuids.size(), namespaceId, partitionId);
		LOGGER.info(LOG_TIME_TAKEN, (System.currentTimeMillis() - startTime) / 1000);
		return result;
	}

	/**
	 * Process a single batch of UUIDs and populate the result map with SCT IDs.
	 */
	private void processSctIdBatch(List<String> batchJob,
	                               Integer namespaceId,
	                               String partitionId,
	                               String comment,
	                               Map<UUID, Long> result) throws RestClientException {
		try {
			// Build request JSON
			JsonObject requestJson = new JsonObject();
			requestJson.addProperty(NAMESPACE, namespaceId);
			requestJson.addProperty(PARTITION_ID, partitionId);
			requestJson.addProperty(QUANTITY, batchJob.size());
			requestJson.add(SYSTEM_IDS, gson.toJsonTree(batchJob));
			requestJson.addProperty(SOFTWARE, SRS);
			requestJson.addProperty(GENERATE_LEGACY_IDS, FALSE);
			requestJson.addProperty(COMMENT, comment);

			// Send request
			HttpEntity<String> requestEntity = new HttpEntity<>(gson.toJson(requestJson), headers);
			String url = urlHelper.getSctIdBulkGenerateUrl(token);

			JsonElement jsonElement = makeHttpCall(url, HttpMethod.POST, requestEntity);

			String jobId = jsonElement.getAsJsonObject().get("id").getAsString();
			LOGGER.info(LOG_JOB_WITH_SIZE, jobId, batchJob.size());
			recoverResultsFromBulkJob(jobId, token, result, SCTID, JsonElement::getAsLong);
		} catch (Exception e) {
			throw new RestClientException("Bulk getOrCreateSctIds job failed.", e);
		}
	}


	@Override
	public Map<UUID, String> getOrCreateSchemeIds(List<UUID> uuids,
	                                              SchemeIdType schemeType,
	                                              String comment) throws RestClientException {
		LOGGER.debug("Start creating scheme id {} with batch size {}", schemeType, uuids.size());

		Map<UUID, String> result = new HashMap<>();
		if (uuids.isEmpty()) {
			LOGGER.warn("Empty UUIDs submitted for requesting schemeIdType: {}", schemeType);
			return result;
		}

		long startTime = System.currentTimeMillis();
		List<String> batchJob = new ArrayList<>();
		int counter = 0;

		for (UUID uuid : uuids) {
			batchJob.add(uuid.toString());
			counter++;

			if (counter % batchSize == 0 || counter == uuids.size()) {
				processSchemeIdBatch(batchJob, schemeType, comment, result);
				batchJob.clear();
			}
		}

		LOGGER.debug("End creating scheme id {} with batch size {}", schemeType, uuids.size());
		LOGGER.info(LOG_TIME_TAKEN, (System.currentTimeMillis() - startTime) / 1000);
		return result;
	}

	/**
	 * Processes a single batch of UUIDs for scheme ID creation.
	 */
	private void processSchemeIdBatch(List<String> batchJob,
	                                  SchemeIdType schemeType,
	                                  String comment,
	                                  Map<UUID, String> result) throws RestClientException {
		try {
			// Build request JSON
			JsonObject requestJson = new JsonObject();
			requestJson.addProperty(QUANTITY, batchJob.size());
			requestJson.add(SYSTEM_IDS, gson.toJsonTree(batchJob));
			requestJson.addProperty(SOFTWARE, SRS);
			requestJson.addProperty(COMMENT, comment);

			HttpEntity<String> requestEntity = new HttpEntity<>(gson.toJson(requestJson), headers);
			String url = urlHelper.getSchemeIdBulkGenerateUrl(token, schemeType);
			JsonElement jsonElement = makeHttpCall(url, HttpMethod.POST, requestEntity);
			String jobId = jsonElement.getAsJsonObject().get("id").getAsString();
			LOGGER.info("Scheme ids bulk job id: {} with batch size: {}", jobId, batchJob.size());
			recoverResultsFromBulkJob(jobId, token, result, SCHEME_ID, JsonElement::getAsString);
		} catch (Exception e) {
			throw new RestClientException("Bulk job getOrCreateSchemeIds failed for schemeType: " + schemeType, e);
		}
	}

	private <T> void recoverResultsFromBulkJob(String jobId, String token, Map<UUID, T> result, String itemToRecover, Function<JsonElement, T> valueMapper) throws RestClientException {
		// Wait for bulk job completion
		int statusCode = waitForCompleteStatus(jobId, getTimeOutInSeconds());
		if (BULK_JOB_STATUS.COMPLETED_WITH_SUCCESS.getCode() != statusCode) {
			throw new RestClientException("Bulk job did not complete successfully: " + jobId);
		}

		// Retrieve bulk job results
		ResponseEntity<String> resultResponse = restTemplate.getForEntity(
				urlHelper.getBulkJobResultUrl(jobId, token),
				String.class
		);

		if (!resultResponse.getStatusCode().is2xxSuccessful()) {
			throw new RestClientException("Failed to retrieve bulk job results for job: " + jobId);
		}

		JsonArray items = JsonParser.parseString(resultResponse.getBody())
				.getAsJsonObject()
				.getAsJsonArray(ITEMS);

		for (JsonElement element : items) {
			JsonObject item = element.getAsJsonObject();
			UUID systemId = UUID.fromString(item.get(SYSTEM_ID).getAsString());
			T value = valueMapper.apply(item.get(itemToRecover));
			result.put(systemId, value);
		}
	}


	private String getFailureMessage(ResponseEntity<JsonObject> response) {
		String msg = "Message Unknown";
		try {
			JsonObject json =  response.getBody();
			if (json != null) {
				msg = json.get(MESSAGE).toString();
			}
		} catch (Exception e) {
			//Welp, we tried
		}
		return "Received Http status from id service:" + response.getStatusCode() + " message:" + msg;
	}


	private int waitForCompleteStatus(String jobId, int timeoutInSeconds) throws RestClientException {

		String url = urlHelper.getBulkJobStatusUrl(token, jobId);
		long startTime = System.currentTimeMillis();
		int status = 0;
		boolean isCompleted = false;
		String logMsg = null;

		while (!isCompleted) {
			// Fetch latest status and optional log
			StatusLog statusLog = fetchBulkJobStatus(url);
			status = statusLog.status();
			logMsg = statusLog.log();

			isCompleted = (BULK_JOB_STATUS.PENDING.getCode() != status &&
					BULK_JOB_STATUS.RUNNING.getCode() != status);

			if (!isCompleted && (System.currentTimeMillis() - startTime) > timeoutInSeconds * 1000L) {
				String message = "Client timeout after waiting " + timeoutInSeconds +
						" seconds for bulk job to complete: " + url;
				LOGGER.warn(message);
				throw new RestClientException(message);
			}

			if (!isCompleted) {
				try {
					Thread.sleep(10_000L); // 10 seconds
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					throw new RestClientException("Sleep interrupted which waiting for jobId " + jobId + " to complete");
				}
			}
		}

		if (BULK_JOB_STATUS.COMPLETED_WITH_SUCCESS.getCode() != status) {
			LOGGER.error("Bulk job id {} finished with non-successful status {} failureReason: {}", jobId, status, logMsg);
			throw new RestClientException("Bulk job: " + jobId + " did not complete successfully with status code: " + status);
		}

		return status;
	}

	/**
	 * Fetches the current status and log message for a bulk job.
	 */
	private StatusLog fetchBulkJobStatus(String url) throws RestClientException {
		try {
			ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
			if (!response.getStatusCode().is2xxSuccessful()) {
				throw new RestClientException("Failed to get bulk job status: " + response.getStatusCode());
			}

			JsonObject body = JsonParser.parseString(response.getBody()).getAsJsonObject();
			int status = body.get(STATUS).getAsInt();
			String log = null;

			if (body.has("log") && !body.get("log").isJsonNull()) {
				log = body.get("log").getAsString();
			}

			return new StatusLog(status, log);
		} catch (Exception e) {
			throw new RestClientException("Rest client error while checking bulk job status: " + url, e);
		}
	}

	@Override
	public void logOut() throws RestClientException {
		currentSessions.getAndDecrement();
		synchronized (LOCK) {
			if (token != null) {
				LOGGER.debug("Total current sessions: {}", currentSessions.get());
				if (currentSessions.get() == 0) {
					try {
						// Build request JSON using Gson
						JsonObject requestJson = new JsonObject();
						requestJson.addProperty(TOKEN_STR, token);
						HttpEntity<String> requestEntity = new HttpEntity<>(gson.toJson(requestJson), headers);
						makeHttpCall(urlHelper.getLogoutUrl(), HttpMethod.POST, requestEntity);
						LOGGER.info("Id service rest client logs out successfully.");
						token = null;
					} catch (Exception e) {
						throw new RestClientException("Failed to log out user " + this.userName, e);
					}
				}
			}
		}
	}

	@Override
	public boolean publishSctIds(List<Long> sctIds, Integer namespaceId, String comment) throws RestClientException {
		LOGGER.debug("Start publishing sctIds with batch size {} for namespace {}", sctIds.size(), namespaceId);
		if (sctIds.isEmpty()) {
			return true;
		}

		boolean isPublished = false;
		long startTime = System.currentTimeMillis();
		List<String> batchJob = new ArrayList<>();
		int counter = 0;

		for (Long sctId : sctIds) {
			batchJob.add(String.valueOf(sctId));
			counter++;

			if (counter % batchSize == 0 || counter == sctIds.size()) {
				isPublished = processPublishSctIdBatch(batchJob, namespaceId, comment) || isPublished;
				batchJob.clear();
			}
		}

		LOGGER.debug("End publishing sctIds with batch size {} for namespace {}", sctIds.size(), namespaceId);
		LOGGER.info(LOG_TIME_TAKEN, (System.currentTimeMillis() - startTime) / 1000);
		return isPublished;
	}

	/**
	 * Processes a single batch of sctIds for publishing.
	 */
	private boolean processPublishSctIdBatch(List<String> batchJob, Integer namespaceId, String comment) throws RestClientException {
		try {
			// Build request JSON
			JsonObject requestJson = new JsonObject();
			requestJson.add(NAMESPACE, gson.toJsonTree(namespaceId));
			requestJson.add(SCTIDS, gson.toJsonTree(batchJob));
			requestJson.addProperty(SOFTWARE, SRS);
			requestJson.addProperty(COMMENT, comment);
			HttpEntity<String> requestEntity = new HttpEntity<>(gson.toJson(requestJson), headers);
			String url = urlHelper.getSctIdBulkPublishingUrl(token);

			JsonElement jsonElement = makeHttpCall(url, HttpMethod.PUT, requestEntity);
			String jobId = jsonElement.getAsJsonObject().get("id").getAsString();
			LOGGER.info("Bulk job id: {} for publishing sctIds with batch size: {}", jobId, batchJob.size());
			return BULK_JOB_STATUS.COMPLETED_WITH_SUCCESS.getCode() ==
					waitForCompleteStatus(jobId, getTimeOutInSeconds());
		} catch (Exception e) {
			throw new RestClientException("Bulk publishSctIds job failed.", e);
		}
	}


	@Override
	public boolean publishSchemeIds(List<String> schemeIds, SchemeIdType schemeType, String comment) throws RestClientException {
		LOGGER.debug("Start publishing scheme ids with batch size {} for scheme type {}", schemeIds.size(), schemeType);

		if (schemeIds.isEmpty()) {
			return true;
		}

		boolean isPublished = false;
		long startTime = System.currentTimeMillis();
		List<String> batchJob = new ArrayList<>();
		int counter = 0;

		for (String schemeId : schemeIds) {
			batchJob.add(schemeId);
			counter++;

			if (counter % batchSize == 0 || counter == schemeIds.size()) {
				isPublished = processPublishSchemeIdBatch(batchJob, schemeType, comment) || isPublished;
				batchJob.clear();
			}
		}

		LOGGER.debug("End publishing scheme ids with batch size {}", schemeIds.size());
		LOGGER.info(LOG_TIME_TAKEN, (System.currentTimeMillis() - startTime) / 1000);
		return isPublished;
	}

	/**
	 * Processes a single batch of scheme ids for publishing.
	 */
	private boolean processPublishSchemeIdBatch(List<String> batchJob, SchemeIdType schemeType, String comment) throws RestClientException {
		try {
			// Build request JSON
			JsonObject requestJson = new JsonObject();
			requestJson.add(SCHEME_IDS, gson.toJsonTree(batchJob));
			requestJson.addProperty(SOFTWARE, SRS);
			requestJson.addProperty(COMMENT, comment);

			HttpEntity<String> requestEntity = new HttpEntity<>(gson.toJson(requestJson), headers);
			String url = urlHelper.getSchemeIdBulkPublishingUrl(schemeType, token);
			JsonElement jsonElement = makeHttpCall(url, HttpMethod.PUT, requestEntity);

			String jobId = jsonElement.getAsJsonObject().get("id").getAsString();
			LOGGER.info("Bulk job id: {} for publishing scheme ids with batch size: {}", jobId, batchJob.size());
			return BULK_JOB_STATUS.COMPLETED_WITH_SUCCESS.getCode() ==
					waitForCompleteStatus(jobId, getTimeOutInSeconds());
		} catch (Exception e) {
			throw new RestClientException("Bulk publish scheme ids job failed.", e);
		}
	}

	@Override
	public Map<String, String> getStatusForSchemeIds(SchemeIdType schemeType, Collection<String> legacyIds) throws RestClientException {
		Map<String, String> result = new HashMap<>();
		if (legacyIds == null || legacyIds.isEmpty()) {
			return result;
		}

		int attempt = 1;
		boolean isDone = false;

		while (!isDone) {
			try {
				result.putAll(fetchSchemeIdStatusBatch(schemeType, legacyIds));
				isDone = true;
			} catch (Exception e) {
				attempt++;
				String action = "get scheme Ids for batch size: " + legacyIds.size();
				sleepAndRetry(action, attempt, e);
			}
		}

		return result;
	}

	/**
	 * Fetches a single batch of schemeId statuses from the ID service.
	 */
	private Map<String, String> fetchSchemeIdStatusBatch(SchemeIdType schemeType, Collection<String> legacyIds) throws RestClientException {
		try {
			// Build URL with query params
			String url = urlHelper.getSchemeIdBulkUrl(token, schemeType, legacyIds);
			JsonElement responseElement = makeHttpCall(url, HttpMethod.GET, HttpEntity.EMPTY);

			Map<String, String> statusMap = new HashMap<>();
			JsonArray items = responseElement.getAsJsonArray();
			for (JsonElement itemElement : items) {
				JsonObject item = itemElement.getAsJsonObject();
				String schemeId = item.get(SCHEME_ID).getAsString();
				String status = item.get(STATUS).getAsString();
				statusMap.put(schemeId, status);
			}

			return statusMap;
		} catch (Exception e) {
			throw new RestClientException("Error fetching scheme id statuses", e);
		}
	}

	private JsonElement makeHttpCall(String url, HttpMethod method, HttpEntity<?> requestEntity) throws RestClientException {
		ResponseEntity<String> response = restTemplate.exchange(
				url,
				method,
				requestEntity,
				String.class
		);

		if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
			String errorMsg = "HTTP status code: " + response.getStatusCode() + " for URL: " + url;
			if (response.getBody() == null) {
				errorMsg += ", but response body is null";
			}
			throw new RestClientException(errorMsg);
		}

		return JsonParser.parseString(response.getBody());
	}


	public Map<Long, JsonObject> getSctIdRecords(Collection<Long> sctIds) throws RestClientException {
		Map<Long, JsonObject> result = new HashMap<>();
		if (sctIds == null || sctIds.isEmpty()) {
			return result;
		}

		int attempt = 1;
		while (true) {
			try {
				result.putAll(fetchSctIdRecordsBatch(sctIds));
				break;
			} catch (Exception e) {
				attempt++;
				String action = "get sctIds for batch size: " + sctIds.size();
				sleepAndRetry(action, attempt, e);
			}
		}
		return result;
	}

	/**
	 * Fetches a single batch of SCT ID records.
	 */
	private Map<Long, JsonObject> fetchSctIdRecordsBatch(Collection<Long> sctIds) throws RestClientException {
		try {
			// Build URL with query parameters
			String url = urlHelper.getSctIdBulkUrl(token, sctIds);
			JsonElement responseElement = makeHttpCall(url, HttpMethod.GET, HttpEntity.EMPTY);

			Map<Long, JsonObject> resultMap = new HashMap<>();
			JsonArray items = responseElement.getAsJsonArray();
			for (JsonElement itemElement : items) {
				JsonObject item = itemElement.getAsJsonObject();
				Long sctId = item.get(SCTID).getAsLong();
				resultMap.put(sctId, item);
			}

			return resultMap;
		} catch (Exception e) {
			throw new RestClientException("Error fetching SCT ID records", e);
		}
	}

	@Override
	public Map<Long, UUID> getUuidsForSctIds(Collection<Long> sctIds) throws RestClientException {
		Map<Long, UUID> sctIdUuidMap = new HashMap<>();
		if (sctIds == null || sctIds.isEmpty()) {
			return sctIdUuidMap;
		}

		List<Long> batchJob = new ArrayList<>();
		int counter = 0;

		for (Long sctId : sctIds) {
			batchJob.add(sctId);
			counter++;
			if (counter % batchSize == 0 || counter == sctIds.size()) {
				Map<Long, JsonObject> sctIdRecords = fetchSctIdRecordsBatchForUUID(batchJob);
				for (Map.Entry<Long, JsonObject> entry : sctIdRecords.entrySet()) {
					Long id = entry.getKey();
					JsonObject json = entry.getValue();
					try {
						String uuidStr = json.get(SYSTEM_ID).getAsString();
						sctIdUuidMap.put(id, UUID.fromString(uuidStr));
					} catch (IllegalArgumentException | NullPointerException e) {
						throw new RestClientException("Error fetching system ID for sctId: " + id
								+ " from JSON: " + json.toString(), e);
					}
				}
				batchJob = new ArrayList<>();
			}
		}

		return sctIdUuidMap;
	}

	/**
	 * Fetches a batch of SCT ID records from the ID service.
	 */
	private Map<Long, JsonObject> fetchSctIdRecordsBatchForUUID(Collection<Long> sctIds) throws RestClientException {
		try {
			String url = urlHelper.getSctIdBulkUrl(token, sctIds);
			JsonElement responseElement = makeHttpCall(url, HttpMethod.GET, HttpEntity.EMPTY);

			Map<Long, JsonObject> resultMap = new HashMap<>();
			JsonArray items = responseElement.getAsJsonArray();
			for (JsonElement element : items) {
				JsonObject item = element.getAsJsonObject();
				Long sctId = item.get(SCTID).getAsLong();
				resultMap.put(sctId, item);
			}
			return resultMap;
		} catch (Exception e) {
			throw new RestClientException("Error fetching SCT ID records batch", e);
		}
	}

	@Override
	public List<Long> registerSctIds(List<Long> sctIdsToRegister, Map<Long, UUID> sctIdSystemIdMap,
	                                 Integer namespaceId, String comment) throws RestClientException {
		LOGGER.debug("Start registering sctIds with batch size {} for namespace {}", sctIdsToRegister.size(), namespaceId);
		List<Long> result = new ArrayList<>();
		if (sctIdsToRegister.isEmpty()) {
			LOGGER.warn("No sctIds submitted for registration");
			return result;
		}

		long startTime = new Date().getTime();

		JsonArray records = prepareSctIdRegistrationRecords(sctIdsToRegister, sctIdSystemIdMap);

		try {
			JsonObject requestData = new JsonObject();
			requestData.addProperty(NAMESPACE, namespaceId);
			requestData.add("records", records);
			requestData.addProperty(SOFTWARE, SRS);
			requestData.addProperty(COMMENT, comment);
			HttpEntity<String> requestEntity = new HttpEntity<>(gson.toJson(requestData), headers);
			String url = urlHelper.getSctIdBulkRegisterUrl(token);
			JsonElement responseElement = makeHttpCall(url, HttpMethod.POST, requestEntity);

			// Wait for completion
			JsonObject responseObject = responseElement.getAsJsonObject();
			String jobId = responseObject.get("id").getAsString();
			LOGGER.info(LOG_JOB_WITH_SIZE, jobId, sctIdsToRegister.size());

			if (BULK_JOB_STATUS.COMPLETED_WITH_SUCCESS.getCode() == waitForCompleteStatus(jobId, getTimeOutInSeconds())) {
				String resultJson = restTemplate.getForObject(urlHelper.getBulkJobResultUrl(jobId, token), String.class);
				JsonArray items = JsonParser.parseString(resultJson).getAsJsonArray();
				for (JsonElement itemEl : items) {
					JsonObject item = itemEl.getAsJsonObject();
					result.add(item.get(SCTID).getAsLong());
				}
			}
		} catch (Exception e) {
			throw new RestClientException("Bulk register sctIds job failed.", e);
		}

		LOGGER.debug("End registering sctIds with batch size {} for namespace {}", sctIdsToRegister.size(), namespaceId);
		LOGGER.info(LOG_TIME_TAKEN, (new Date().getTime() - startTime) / 1000);
		return result;
	}

	/** Helper to convert SCT IDs + system IDs into a JSON array for registration */
	private JsonArray prepareSctIdRegistrationRecords(List<Long> sctIdsToRegister, Map<Long, UUID> sctIdSystemIdMap) {
		JsonArray records = new JsonArray();
		for (Long sctId : sctIdsToRegister) {
			JsonObject regRecord = new JsonObject();
			regRecord.addProperty(SCTID, sctId);
			UUID systemId = (sctIdSystemIdMap != null && sctIdSystemIdMap.containsKey(sctId))
					? sctIdSystemIdMap.get(sctId)
					: UUID.randomUUID();
			regRecord.addProperty(SYSTEM_ID, systemId.toString().toLowerCase());
			records.add(regRecord);
		}
		return records;
	}


	@Override
	public List<Long> reserveSctIds(Integer namespaceId, int totalToReserve, String partitionId, String comment) throws RestClientException {
		long startTime = new Date().getTime();
		List<Long> result = new ArrayList<>();

		try {
			// Prepare request JSON
			JsonObject requestData = new JsonObject();
			requestData.addProperty(NAMESPACE, namespaceId);
			requestData.addProperty(SOFTWARE, SRS);
			requestData.addProperty(QUANTITY, totalToReserve);
			requestData.addProperty(PARTITION_ID, partitionId);
			requestData.addProperty(COMMENT, comment);
			HttpEntity<String> requestEntity = new HttpEntity<>(gson.toJson(requestData), headers);
			String url = urlHelper.getSctIdBulkReserveUrl(token);
			JsonElement responseElement = makeHttpCall(url, HttpMethod.POST, requestEntity);

			// Get jobId from response
			String jobId = responseElement.getAsJsonObject().get("id").getAsString();
			LOGGER.info(LOG_JOB_WITH_SIZE, jobId, totalToReserve);

			// Wait for job completion
			if (BULK_JOB_STATUS.COMPLETED_WITH_SUCCESS.getCode() == waitForCompleteStatus(jobId, getTimeOutInSeconds())) {
				String resultJson = restTemplate.getForObject(urlHelper.getBulkJobResultUrl(jobId, token), String.class);
				JsonArray items = JsonParser.parseString(resultJson).getAsJsonArray();
				for (JsonElement itemEl : items) {
					JsonObject item = itemEl.getAsJsonObject();
					result.add(item.get(SCTID).getAsLong());
				}
			}
		} catch (Exception e) {
			throw new RestClientException("Bulk reserving sctIds job failed.", e);
		}

		LOGGER.debug("End reserving sctIds with batch size {} for namespace {}", totalToReserve, namespaceId);
		LOGGER.info(LOG_TIME_TAKEN, (new Date().getTime() - startTime) / 1000);
		return result;
	}


	@Override
	public List<Long> generateSctIds(Integer namespaceId, int totalToGenerate, String partitionId, String comment) throws RestClientException {
		long startTime = new Date().getTime();
		List<Long> result = new ArrayList<>();

		try {
			// Prepare request JSON
			JsonObject requestData = new JsonObject();
			requestData.addProperty(NAMESPACE, namespaceId);
			requestData.addProperty(SOFTWARE, SRS);
			requestData.addProperty(QUANTITY, totalToGenerate);
			requestData.addProperty(PARTITION_ID, partitionId);
			requestData.addProperty(GENERATE_LEGACY_IDS, FALSE);
			requestData.addProperty(COMMENT, comment);
			HttpEntity<String> requestEntity = new HttpEntity<>(gson.toJson(requestData), headers);
			String url = urlHelper.getSctIdBulkGenerateUrl(token);
			JsonElement responseElement = makeHttpCall(url, HttpMethod.POST, requestEntity);

			String jobId = responseElement.getAsJsonObject().get("id").getAsString();
			LOGGER.info(LOG_JOB_WITH_SIZE, jobId, totalToGenerate);

			// Wait for job completion
			if (BULK_JOB_STATUS.COMPLETED_WITH_SUCCESS.getCode() == waitForCompleteStatus(jobId, getTimeOutInSeconds())) {
				String resultJson = restTemplate.getForObject(urlHelper.getBulkJobResultUrl(jobId, token), String.class);
				JsonArray items = JsonParser.parseString(resultJson).getAsJsonArray();
				for (JsonElement itemEl : items) {
					JsonObject item = itemEl.getAsJsonObject();
					result.add(item.get(SCTID).getAsLong());
				}
			}
		} catch (Exception e) {
			throw new RestClientException("Bulk generating sctIds job failed.", e);
		}

		LOGGER.debug("End generating sctIds with batch size {} for namespace {}", totalToGenerate, namespaceId);
		LOGGER.info(LOG_TIME_TAKEN, (new Date().getTime() - startTime) / 1000);

		return result;
	}

	/**
	 * Simple holder class for status and log.
	 */
	private record StatusLog(int status, String log) {}

}

package org.ihtsdo.buildcloud.core.service.identifier.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.http.HttpStatus;
import org.ihtsdo.otf.rest.client.RestClientException;
import org.ihtsdo.otf.rest.client.resty.RestyHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import us.monoid.json.JSONArray;
import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import us.monoid.web.JSONResource;

@Service
public class IdServiceRestClientImpl implements IdServiceRestClient {

	private static final String TOKEN = "token";
	private static final String MESSAGE = "message";
	private static final String STATUS = "status";
	private static final String SCHEME_ID = "schemeId";
	private static final String SCHEME_IDS = "schemeIds";
	private static final String SCTIDS = "sctids";
	private static final String SRS = "srs";
	private static final String SYSTEM_IDS = "systemIds";
	private static final String QUANTITY = "quantity";
	private static final String COMMENT = "comment";
	private static final String GENERATE_LEGACY_IDS = "generateLegacyIds";
	private static final String SOFTWARE = "software";
	private static final String PARTITION_ID = "partitionId";
	private static final String NAMESPACE = "namespace";
	private static final String SYSTEM_ID = "systemId";
	private static final String SCTID = "sctid";
	private static final String APPLICATION_JSON = "application/json";
	public static final String ANY_CONTENT_TYPE = "*/*";
	private final String idServiceUrl;
	private final RestyHelper resty;
	private final IdServiceRestUrlHelper urlHelper;
	private static String token;
	private static final Object LOCK = new Object();
	private static final Logger LOGGER = LoggerFactory.getLogger(IdServiceRestClientImpl.class);
	
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
			@Value("${cis.userName}") final String username,
			@Value("${cis.password}") final String password) {
		this.idServiceUrl = idServiceUrl;
		urlHelper = new IdServiceRestUrlHelper(idServiceUrl);
		this.resty = new RestyHelper(ANY_CONTENT_TYPE);
		this.userName = username;
		this.password = password;
		
	}
	
	private boolean isServiceRunning() {
		JSONResource response;
		try {
			response = resty.json(urlHelper.getTestServiceUrl());
			if (response != null && HttpStatus.SC_OK == response.getHTTPStatus()) {
				return true;
			}
		} catch (IOException e) {
			LOGGER.error("Error when testing service", e);
		}
		return false;
	}
	
	@Override
	public String logIn() throws RestClientException {
		int attempt = 1;
		boolean isDone = false;
			while (!isDone) {
			 try {
				 synchronized (LOCK) {
					 if (token != null) {
						 LOGGER.debug("ID service rest client is already logged in.");
					 } 
					 //validate token

					 if ( !isTokenValid(token) ) {
						 //get a new token;
						 token = accquireToken();
					 }
					 currentSessions.getAndIncrement();
					 isDone = true;
				}
			} catch (Exception e) {
				if (attempt < maxTries) {
					LOGGER.warn("Failed to log into the IdService on attempt {}. Waiting {} seconds before retrying.", attempt, retryDelaySeconds, e);
					attempt++;
					try {
						Thread.sleep(retryDelaySeconds * 1000);
					} catch (InterruptedException ie) {
						LOGGER.warn("Retry delay interrupted.",e);
					}
				} else {
					throw new RestClientException("Still failed to log into the IdService after " + attempt + " attempts", e);
				}
			}
		}
		return token;
	}

	private String accquireToken() throws RestClientException {
		String securityToken = null;
		if (!isServiceRunning()) {
			throw new RestClientException("Id service is not currently running at URL:" + idServiceUrl);
		}
		LOGGER.info("Id service rest client logs in to get a new security token." );
		try {
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("username", this.userName);
			jsonObject.put("password", this.password);
			securityToken = (String) resty.json(urlHelper.getLoginUrl(), RestyHelper.content(jsonObject)).get(TOKEN);
			LOGGER.debug("Security token is acquired successfully.");
		} catch (Exception e) {
			throw new RestClientException("Failed to login for user name:" + this.userName, e);
		}
		return securityToken;
	}
	
	private boolean isTokenValid(String token) {
		if (token == null) {
			return false;
		}
		boolean isValid = false;
		try {
			JSONObject jsonObject = new JSONObject();
			jsonObject.put(TOKEN, token);
			JSONResource response = resty.json(urlHelper.getTokenAuthenticationUrl(), RestyHelper.content(jsonObject,APPLICATION_JSON));
			if (response != null) {
				if (HttpStatus.SC_OK == (response.getHTTPStatus())) {
					isValid = true;
				} else {
					LOGGER.info("Invalid token with failure reason from id server:" + response.get(MESSAGE));
				}
			}
		} catch (Exception e) {
			LOGGER.error("Failed to log in", e);
		}
		return isValid;
	}

	@Override
	public Map<Long,String> getStatusForSctIds(Collection<Long> sctIds) throws RestClientException {
		Map<Long,String> result = new HashMap<>();
		if (sctIds == null || sctIds.isEmpty()) {
			return result;
		}
		StringBuilder scdStrList = new StringBuilder();
		boolean isFirst = true;
		for (Long id : sctIds) {
			if (!isFirst) {
				scdStrList.append(",");
			}
			if (isFirst) {
				isFirst = false;
			}
			scdStrList.append(id.toString());
		}
		int attempt = 1;
		boolean isDone = false;
		while (!isDone) {
				try {
					JSONObject requestData = new JSONObject();
					requestData.put(SCTIDS, scdStrList.toString());
					JSONResource response = resty.json(urlHelper.getSctIdBulkUrl(token),RestyHelper.content(requestData, APPLICATION_JSON));
					if ( response != null && HttpStatus.SC_OK == (response.getHTTPStatus()) ){
						JSONArray items = response.array();
						for (int i =0; i < items.length();i++) {
							result.put(new Long((String)items.getJSONObject(i).get(SCTID)), (String)items.getJSONObject(i).get(STATUS));
						}
					} else {
						throw new RestClientException(getFailureMessage(response));
					}
					isDone = true;
				} catch (Exception e) {
					
					if (attempt < maxTries) {
						LOGGER.warn("Id service failed on attempt {}. Waiting {} seconds before retrying.", attempt, retryDelaySeconds, e);
						attempt++;
						try {
							Thread.sleep(retryDelaySeconds * 1000);
						} catch (InterruptedException ie) {
							LOGGER.warn("Retry delay interrupted.",e);
						}
					} else {
						throw new RestClientException("Failed to get sctIds for batch size:" + sctIds.size(), e);
					}
				}
		}
		return result;
	}
	
	@Override
	public Long getOrCreateSctId(UUID componentUuid, Integer namespaceId, String partitionId, String comment) throws RestClientException {
		Long result = null;
		int attempt = 1;
		while (result == null) {
				try {
					JSONObject requestData = new JSONObject();
					requestData.put(NAMESPACE, namespaceId.intValue());
					requestData.put(PARTITION_ID, partitionId);
					requestData.put(SYSTEM_ID, componentUuid.toString());
					requestData.put(SOFTWARE, SRS);
					requestData.put(GENERATE_LEGACY_IDS, "false");
					requestData.put(COMMENT, comment);
					JSONResource response = resty.json(urlHelper.getSctIdGenerateUrl(token), RestyHelper.content((requestData),APPLICATION_JSON));
					if ( response != null && HttpStatus.SC_OK == (response.getHTTPStatus()) ){
						 result = new Long((String)response.get(SCTID));
					} else {
						throw new RestClientException(getFailureMessage(response));
					}
				} catch (Exception e) {
					
					if (attempt < maxTries) {
						LOGGER.warn("Id service failed on attempt {}. Waiting {} seconds before retrying.", attempt, retryDelaySeconds, e);
						attempt++;
						try {
							Thread.sleep(retryDelaySeconds * 1000);
						} catch (InterruptedException ie) {
							LOGGER.warn("Retry delay interrupted.",e);
						}
					} else {
						throw new RestClientException("Failed to create sctId for uuid:" + componentUuid.toString(), e);
					}
				}
		}
		
		return result;
	}
	
	@Override
	public HashMap<UUID,Long> getOrCreateSctIds(List<UUID> uuids,Integer namespaceId,String partitionId, String comment) throws RestClientException {
		LOGGER.debug("Start creating sctIds with batch size {} for namespace {} and partitionId {}", uuids.size(), namespaceId, partitionId);
		HashMap<UUID, Long> result = new HashMap<>();
		if (uuids == null || uuids.isEmpty()) {
			LOGGER.warn("Empty UUIDs submitted for requesting sctIds");
			return result;
		}
		long startTime = new Date().getTime();
		List<String> batchJob = null;
		int counter=0;
		for (UUID uuid : uuids) {
			if (batchJob == null) {
				batchJob = new ArrayList<>();
			}
			batchJob.add(uuid.toString());
			counter++;
			if (counter % batchSize == 0 || counter == uuids.size()) {
				try {
					JSONObject requestData = new JSONObject();
					requestData.put(NAMESPACE, namespaceId.intValue());
					requestData.put(PARTITION_ID, partitionId);
					requestData.put(QUANTITY,batchJob.size());
					requestData.put(SYSTEM_IDS, batchJob.toArray());
					requestData.put(SOFTWARE, SRS);
					requestData.put(GENERATE_LEGACY_IDS, "false");
					requestData.put(COMMENT, comment);
					JSONResource response = resty.json(urlHelper.getSctIdBulkGenerateUrl(token), RestyHelper.content((requestData),APPLICATION_JSON));
					if ( HttpStatus.SC_OK == response.getHTTPStatus()) {
						String jobId =  response.get("id").toString();
						LOGGER.info("Bulk job id:" + jobId + " with batch size:" + batchJob.size());
						if (BULK_JOB_STATUS.COMPLETED_WITH_SUCCESS.getCode() == waitForCompleteStatus(jobId, getTimeOutInSeconds())) {
							JSONArray items = resty.json(urlHelper.getBulkJobResultUrl(jobId, token)).array();
							for (int i =0;i < items.length();i++) {
								result.put(UUID.fromString((String)items.getJSONObject(i).get(SYSTEM_ID)), new Long((String)items.getJSONObject(i).get(SCTID)));
							}
						}
					} else {
						String statusMsg = getFailureMessage(response);
						LOGGER.error(statusMsg);
						throw new RestClientException(statusMsg);
					}
				} catch (Exception e) {
					String message = "Bulk getOrCreateSctIds job failed.";
					LOGGER.error(message, e);
					throw new RestClientException(message,e);
				}
				batchJob = null;
			}
		}
		LOGGER.debug("End creating sctIds with batch size {} for namespace {} and partitionId {}", uuids.size(), namespaceId, partitionId);
		LOGGER.info("Time taken in seconds:" + (new Date().getTime() - startTime) /1000);
		return result;
	}

	@Override
	public Map<UUID, String> getOrCreateSchemeIds(List<UUID> uuids, SchemeIdType schemeType, String comment) throws RestClientException {
		LOGGER.debug("Start creating scheme id {} with batch size {} ", schemeType, uuids.size());
		HashMap<UUID, String> result = new HashMap<>();
		if (uuids == null || uuids.isEmpty()) {
			LOGGER.warn("Empty UUIDs submitted for requesting schemeIdType:" + schemeType);
			return result;
		}
		long startTime = new Date().getTime();
		List<String> batchJob = null;
		int counter=0;
		for (UUID uuid : uuids) {
			if (batchJob == null) {
				batchJob = new ArrayList<>();
			}
			batchJob.add(uuid.toString());
			counter++;
			if (counter % batchSize == 0 || counter == uuids.size()) {
				//processing batch
				try {
					JSONObject requestData = new JSONObject();
					requestData.put(QUANTITY,batchJob.size());
					requestData.put(SYSTEM_IDS, batchJob.toArray());
					requestData.put(SOFTWARE, SRS);
					requestData.put(COMMENT, comment);
					JSONResource response = resty.json(urlHelper.getSchemeIdBulkGenerateUrl(token, schemeType), RestyHelper.content((requestData),APPLICATION_JSON));
					if ( HttpStatus.SC_OK == response.getHTTPStatus()) {
						String jobId =  response.get("id").toString();
						LOGGER.info("Scheme ids bulk job id:" + jobId + " with batch size:" + batchJob.size());
						if (BULK_JOB_STATUS.COMPLETED_WITH_SUCCESS.getCode() == waitForCompleteStatus(jobId, getTimeOutInSeconds())) {
							JSONArray items = resty.json(urlHelper.getBulkJobResultUrl(jobId, token)).array();
							for (int i =0;i < items.length();i++) {
								result.put(UUID.fromString((String)items.getJSONObject(i).get(SYSTEM_ID)), (String)items.getJSONObject(i).get(SCHEME_ID));
							}
						}
					} else {
						throw new RestClientException(getFailureMessage(response));
					}
				} catch (Exception e) {
					String message = "Bulk job getOrCreateSchemeIds failed for schemeType:" + schemeType;
					LOGGER.error(message, e);
					throw new RestClientException(message, e);
				}
				batchJob = null;
			}
		}
		LOGGER.debug("End creating scheme id {} with batch size {} ", schemeType, uuids.size());
		LOGGER.info("Time taken in seconds:" + (new Date().getTime() - startTime) /1000);
		return result;
	}


	private String getFailureMessage(JSONResource response) throws Exception {
		return "Received Http status from id service:" + response.getHTTPStatus() + " message:" + response.get(MESSAGE);
	}
	
	
	private int waitForCompleteStatus(String jobId, int timeoutInSeconds)
			throws RestClientException, InterruptedException {
		String url = urlHelper.getBulkJobStatusUrl(token, jobId);
		long startTime = new Date().getTime();
		int status = 0;
		boolean isCompleted = false;
		String logMsg = null;
		while (!isCompleted) {
			try {
				JSONResource response = resty.json(url);
				Object statusObj = response.get(STATUS);
				status = Integer.parseInt(statusObj.toString()) ;
				Object log = response.get("log");
				if (log != null) {
					logMsg = log.toString();
				}
				
			} catch (Exception e) {
				String msg = "Rest client error while checking bulk job status:" + url;
				LOGGER.error(msg, e);
				throw new RestClientException(msg, e);
			}
			isCompleted = (BULK_JOB_STATUS.PENDING.getCode() != status && BULK_JOB_STATUS.RUNNING.getCode() != status);
			if (!isCompleted && ((new Date().getTime() - startTime) > timeoutInSeconds *1000)) {
				String message = "Client timeout after waiting " + timeoutInSeconds + " seconds for bulk job to complete:" + url;
				LOGGER.warn(message);
				throw new RestClientException(message);
			}
			if (!isCompleted) {
				Thread.sleep(1000 * 10);
			}
		}
		if (BULK_JOB_STATUS.COMPLETED_WITH_SUCCESS.getCode() != status) {
			LOGGER.error("Bulk job id {} finsihed with non successful status {} failureReason: {}", jobId, status, logMsg);
			throw new RestClientException("Bulk job :" + jobId + " did not complete successfully with status code:" + status);
		}
		return status;
	}

	public int getMaxTries() {
		return maxTries;
	}

	public void setMaxTries(int maxTries) {
		this.maxTries = maxTries;
	}

	public int getRetryDelaySeconds() {
		return retryDelaySeconds;
	}

	public void setRetryDelaySeconds(int retryDelaySeconds) {
		this.retryDelaySeconds = retryDelaySeconds;
	}

	@Override
	public void logOut() throws RestClientException {
		currentSessions.getAndDecrement();
		synchronized (LOCK) {
			if (token != null) {
				LOGGER.debug("Total current sessions:" + currentSessions.get());
				if (currentSessions.get() == 0) {
					try {
						JSONObject jsonObject = new JSONObject();
						jsonObject.put(TOKEN, token);
						resty.json(urlHelper.getLogoutUrl(), RestyHelper.content((jsonObject)));
						LOGGER.info("Id service rest client logs out successfully.");
						token = null;
					} catch (Exception e) {
						throw new RestClientException("Failed to login out " + this.userName, e);
					}
				}
			}
		}
	}

	public int getTimeOutInSeconds() {
		return timeOutInSeconds;
	}

	public void setTimeOutInSeconds(int timeOutInSeconds) {
		this.timeOutInSeconds = timeOutInSeconds;
	}
	
	public int getBatchSize() {
		return batchSize;
	}

	public void setBatchSize(int batchSize) {
		this.batchSize = batchSize;
	}

	@Override
	public boolean publishSctIds(List<Long> sctIds, Integer namespaceId, String comment) throws RestClientException {
		LOGGER.debug("Start publishing sctIds with batch size {} for namespace {}", sctIds.size(), namespaceId);
		if (sctIds == null || sctIds.isEmpty()) {
			return true;
		}
		boolean isPublished = false;
		long startTime = new Date().getTime();
		List<String> batchJob = null;
		int counter=0;
		for (Long sctId : sctIds) {
			if (batchJob == null) {
				batchJob = new ArrayList<>();
			}
			batchJob.add(String.valueOf(sctId));
			counter++;
			if (counter % batchSize == 0 || counter == sctIds.size()) {
				//processing batch
				try {
					JSONObject requestData = new JSONObject();
					requestData.put(SCTIDS, batchJob.toArray());
					requestData.put(NAMESPACE, namespaceId.intValue());
					requestData.put(SOFTWARE, SRS);
					requestData.put(COMMENT, comment);
					JSONResource response = resty.put(urlHelper.getSctIdBulkPublishingUrl(token), requestData, APPLICATION_JSON);
					if ( HttpStatus.SC_OK == response.getHTTPStatus()) {
						String jobId =  response.get("id").toString();
						LOGGER.info("Bulk job id:" + jobId + " for publishing sctIds with batch size:" + batchJob.size());
						if (BULK_JOB_STATUS.COMPLETED_WITH_SUCCESS.getCode() == waitForCompleteStatus(jobId, getTimeOutInSeconds())) {
							isPublished = true;
						}
					} else {
						String statusMsg = "Received http status code from id service:" + response.getHTTPStatus();
						LOGGER.error(statusMsg);
						throw new RestClientException(statusMsg);
					}
				} catch (Exception e) {
					String message = "Bulk publishSctIds job failed.";
					LOGGER.error(message, e);
					throw new RestClientException(message, e);
				}
				batchJob = null;
			}
		}
		LOGGER.debug("End publishing sctIds with batch size {} for namespace {}", sctIds.size(), namespaceId);
		LOGGER.info("Time taken in seconds:" + (new Date().getTime() - startTime) /1000);
		return isPublished;
	}

	@Override
	public boolean publishSchemeIds(List<String> schemeIds, SchemeIdType schemeType, String comment) throws RestClientException {
		LOGGER.debug("Start publishing scheme ids with batch size {} for scheme type {}", schemeIds.size(), schemeType);
		if (schemeIds == null || schemeIds.isEmpty()) {
			return true;
		}
		boolean isPublished = false;
		long startTime = new Date().getTime();
		List<String> batchJob = null;
		int counter=0;
		for (String schemeId : schemeIds) {
			if (batchJob == null) {
				batchJob = new ArrayList<>();
			}
			batchJob.add(schemeId);
			counter++;
			if (counter % batchSize == 0 || counter == schemeIds.size()) {
				//processing batch
				try {
					JSONObject requestData = new JSONObject();
					requestData.put(SCHEME_IDS, batchJob.toArray());
					requestData.put(SOFTWARE, SRS);
					requestData.put(COMMENT, comment);
					JSONResource response = resty.put(urlHelper.getSchemeIdBulkPublishingUrl(schemeType,token), requestData, APPLICATION_JSON);
					if ( HttpStatus.SC_OK == response.getHTTPStatus()) {
						String jobId =  response.get("id").toString();
						LOGGER.info("Bulk job id:" + jobId + " for publishing scheme ids with batch size:" + batchJob.size());
						if (BULK_JOB_STATUS.COMPLETED_WITH_SUCCESS.getCode() == waitForCompleteStatus(jobId, getTimeOutInSeconds())){
							isPublished = true;
						}
					} else {
						String statusMsg = "Received http status code from id service:" + response.getHTTPStatus() + " message:" + response.get(MESSAGE);
						LOGGER.error(statusMsg);
						throw new RestClientException(statusMsg);
					}
				} catch (Exception e) {
					String message = "Bulk publish scheme ids job failed.";
					LOGGER.error(message, e);
					throw new RestClientException(message, e);
				}
				batchJob = null;
			}
		}
		LOGGER.debug("End publishing scheme ids with batch size {}", schemeIds.size());
		LOGGER.info("Time taken in seconds:" + (new Date().getTime() - startTime) /1000);
		return isPublished;
	}

	
	@Override
	public Map<String, String> getStatusForSchemeIds(SchemeIdType schemeType, Collection<String> legacyIds) throws RestClientException {
		Map<String,String> result = new HashMap<>();
		if (legacyIds == null || legacyIds.isEmpty()) {
			return result;
		}
		int attempt = 1;
		boolean isDone = false;
		while (!isDone) {
				JSONResource response = null;
				try {
					response = resty.json(urlHelper.getSchemeIdBulkUrl(token, schemeType, legacyIds));
					if ( response != null && HttpStatus.SC_OK == (response.getHTTPStatus()) ){
						JSONArray items = response.array();
						for (int i =0;i < items.length();i++) {
							result.put((String)items.getJSONObject(i).get(SCHEME_ID), (String)items.getJSONObject(i).get(STATUS));
						}
					} else {
						String errorMsg = (response != null) ? ("http status code is:" + response.getHTTPStatus() + " message:" + response.get(MESSAGE)) : "No response received!";
						throw new RestClientException(errorMsg);
					}
					isDone = true;
				} catch (Exception e) {
					
					if (attempt < maxTries) {
						LOGGER.warn("Id service failed on attempt {}. Waiting {} seconds before retrying.", attempt, retryDelaySeconds, e);
						attempt++;
						try {
							Thread.sleep(retryDelaySeconds * 1000);
						} catch (InterruptedException ie) {
							LOGGER.warn("Retry delay interrupted.",e);
						}
					} else {
						throw new RestClientException("Failed to get scheme Ids for batch size:" + legacyIds.size(), e);
					}
				}
		}
		return result;
	}
	
	public Map<Long,JSONObject> getSctIdRecords(Collection<Long> sctIds) throws RestClientException {
		Map<Long,JSONObject> result = new HashMap<>();
		if (sctIds == null || sctIds.isEmpty()) {
			return result;
		}
		int attempt = 1;
		boolean isDone = false;
		while (!isDone) {
				JSONResource response = null;
				try {
					response = resty.json(urlHelper.getSctIdBulkUrl(token, sctIds));
					if ( response != null && HttpStatus.SC_OK == (response.getHTTPStatus()) ){
						JSONArray items = response.array();
						for (int i =0;i < items.length();i++) {
							result.put(new Long((String)items.getJSONObject(i).get(SCTID)), items.getJSONObject(i));
						}
					} else {
						String errorMsg = (response != null) ? "http status code is:" + response.getHTTPStatus() : "No response received.";
						throw new RestClientException(errorMsg);
					}
					isDone = true;
				} catch (Exception e) {
					if (attempt < maxTries) {
						LOGGER.warn("Id service failed on attempt {}. Waiting {} seconds before retrying.", attempt, retryDelaySeconds, e);
						attempt++;
						try {
							Thread.sleep(retryDelaySeconds * 1000);
						} catch (InterruptedException ie) {
							LOGGER.warn("Retry delay interrupted.",e);
						}
					} else {
						throw new RestClientException("Failed to get sctIds for batch size:" + sctIds.size(), e);
					}
				}
		}
		return result;
	}
	
	public Map<String, JSONObject> getSchemeIds(SchemeIdType schemeType, Collection<String> legacyIds) throws RestClientException {
		Map<String,JSONObject> result = new HashMap<>();
		if (legacyIds == null || legacyIds.isEmpty()) {
			return result;
		}
		int attempt = 1;
		boolean isDone = false;
		while (!isDone) {
				JSONResource response = null;
				try {
					response = resty.json(urlHelper.getSchemeIdBulkUrl(token, schemeType, legacyIds));
					if ( response != null && HttpStatus.SC_OK == (response.getHTTPStatus()) ){
						JSONArray items = response.array();
						for (int i =0;i < items.length();i++) {
							result.put((String)items.getJSONObject(i).get(SCHEME_ID), items.getJSONObject(i));
						}
					} else {
						throw new RestClientException("http status code is:" + response.getHTTPStatus());
					}
					isDone = true;
				} catch (Exception e) {
					
					if (attempt < maxTries) {
						LOGGER.warn("Id service failed on attempt {}. Waiting {} seconds before retrying.", attempt, retryDelaySeconds, e);
						attempt++;
						try {
							Thread.sleep(retryDelaySeconds * 1000);
						} catch (InterruptedException ie) {
							LOGGER.warn("Retry delay interrupted.",e);
						}
					} else {
						throw new RestClientException("Failed to get sctIds for batch size:" + legacyIds.size(), e);
					}
				}
		}
		return result;
	}

	@Override
	public Map<Long, UUID> getUuidsForSctIds(Collection<Long> sctIds) throws RestClientException {
		Map<Long, UUID> sctIdUuidMap = new HashMap<>();
		List<Long> batchJob = null;
		int counter=0;
		for (Long sctId : sctIds) {
			if (batchJob == null) {
				batchJob = new ArrayList<>();
			}
			batchJob.add(sctId);
			counter++;
			if (counter % batchSize == 0 || counter == sctIds.size()) {
				Map<Long,JSONObject> sctIdRecords = getSctIdRecords(batchJob);
				String uuidStr = "";
				String jsonStr = "";
				for (Long id : sctIdRecords.keySet()) {
					try {
						jsonStr = sctIdRecords.get(id).toString();
						uuidStr = (String)sctIdRecords.get(id).get(SYSTEM_ID);
						sctIdUuidMap.put(id, UUID.fromString(uuidStr));
					} catch (IllegalArgumentException|JSONException e) {
						throw new RestClientException("Error when fetching system id for sctId: " + id + " using UUID '" + uuidStr + "'.  Received JSON: " + jsonStr, e);
					}
				}
				batchJob = null;
			}
		}
		return sctIdUuidMap;
	}


	@Override
	public List<Long> registerSctIds(List<Long> sctIdsToRegister, Map<Long,UUID> sctIdSystemIdMap, Integer namespaceId, String comment) throws RestClientException {
		LOGGER.debug("Start registering sctIds with batch size {} for namespace {} and partitionId {}", sctIdsToRegister.size(), namespaceId);
		List<Long> result = new ArrayList<>();
		if (sctIdsToRegister == null || sctIdsToRegister.isEmpty()) {
			LOGGER.warn("No sctIds submitted for requesting sctIds");
			return result;
		}
		long startTime = new Date().getTime();
		List<JSONObject> records = new ArrayList<>();
		for (Long sctId : sctIdsToRegister) {
			JSONObject jsonObj = new JSONObject();
			try {
				jsonObj.put(SCTID, sctId.toString());
				UUID systemId = null;
				if (sctIdSystemIdMap != null ) {
					systemId = sctIdSystemIdMap.get(sctId);
				}
				systemId = (systemId == null) ? UUID.randomUUID() : systemId;
				jsonObj.put(SYSTEM_ID, systemId.toString().toLowerCase());
				records.add(jsonObj);
			} catch (JSONException e) {
				String msg = "Failed to create json object";
				LOGGER.error(msg,e);
				throw new RestClientException(msg,e);
			}
		}
		try {
			JSONObject requestData = new JSONObject();
			requestData.put(NAMESPACE, namespaceId.intValue());
			requestData.put("records", records.toArray());
			requestData.put(SOFTWARE, SRS);
			requestData.put(COMMENT, comment);
			JSONResource response = resty.json(urlHelper.getSctIdBulkRegisterUrl(token), RestyHelper.content((requestData),APPLICATION_JSON));
			if ( HttpStatus.SC_OK == response.getHTTPStatus()) {
				String jobId =  response.get("id").toString();
				LOGGER.info("Bulk job id:" + jobId + " with batch size:" + sctIdsToRegister.size());
				if (BULK_JOB_STATUS.COMPLETED_WITH_SUCCESS.getCode() == waitForCompleteStatus(jobId, getTimeOutInSeconds())) {
					JSONArray items = resty.json(urlHelper.getBulkJobResultUrl(jobId, token)).array();
					for (int i =0;i < items.length();i++) {
						result.add(new Long((String)items.getJSONObject(i).get(SCTID)));
					}
				}
			} else {
				String statusMsg = getFailureMessage(response);
				LOGGER.error(statusMsg);
				throw new RestClientException(statusMsg);
			}
		} catch (Exception e) {
			String message = "Bulk register sctIds job failed.";
			LOGGER.error(message, e);
			throw new RestClientException(message,e);
		}
		LOGGER.debug("End registering sctIds with batch size {} for namespace {}", sctIdsToRegister.size(), namespaceId);
		LOGGER.info("Time taken in seconds:" + (new Date().getTime() - startTime) /1000);
		return result;
		
	}

	@Override
	public List<Long> reserveSctIds(Integer namespaceId, int totalToReserve, String partitionId, String comment) throws RestClientException {
		long startTime = new Date().getTime();
		List<Long> result = new ArrayList<>();
		try {
			JSONObject requestData = new JSONObject();
			requestData.put(NAMESPACE, namespaceId.intValue());
			requestData.put(SOFTWARE, SRS);
			requestData.put(QUANTITY, totalToReserve);
			requestData.put(PARTITION_ID, partitionId);
			requestData.put(COMMENT, comment);
			JSONResource response = resty.json(urlHelper.getSctIdBulkReserveUrl(token), RestyHelper.content((requestData),APPLICATION_JSON));
			if ( HttpStatus.SC_OK == response.getHTTPStatus()) {
				String jobId =  response.get("id").toString();
				LOGGER.info("Bulk job id:" + jobId + " with batch size:" + totalToReserve);
				if (BULK_JOB_STATUS.COMPLETED_WITH_SUCCESS.getCode() == waitForCompleteStatus(jobId, getTimeOutInSeconds())) {
					JSONArray items = resty.json(urlHelper.getBulkJobResultUrl(jobId, token)).array();
					for (int i =0;i < items.length();i++) {
						
						result.add(new Long((String)items.getJSONObject(i).get(SCTID)));
					}
				}
			} else {
				String statusMsg = getFailureMessage(response);
				LOGGER.error(statusMsg);
				throw new RestClientException(statusMsg);
			}
		} catch (Exception e) {
			String message = "Bulk reserving sctIds job failed.";
			LOGGER.error(message, e);
			throw new RestClientException(message,e);
		}
		LOGGER.debug("End reserving sctIds with batch size {} for namespace {}", totalToReserve, namespaceId);
		LOGGER.info("Time taken in seconds:" + (new Date().getTime() - startTime) /1000);
		
		return result;
	}

	@Override
	public List<Long> generateSctIds(Integer namespaceId, int totalToGenerate, String partitionId, String comment) throws RestClientException {
		long startTime = new Date().getTime();
		List<Long> result = new ArrayList<>();
		try {
			JSONObject requestData = new JSONObject();
			requestData.put(NAMESPACE, namespaceId.intValue());
			requestData.put(SOFTWARE, SRS);
			requestData.put(QUANTITY, totalToGenerate);
			requestData.put(PARTITION_ID, partitionId);
			requestData.put(GENERATE_LEGACY_IDS, "false");
			requestData.put(COMMENT, comment);
			JSONResource response = resty.json(urlHelper.getSctIdBulkGenerateUrl(token), RestyHelper.content((requestData),APPLICATION_JSON));
			if ( HttpStatus.SC_OK == response.getHTTPStatus()) {
				String jobId =  response.get("id").toString();
				LOGGER.info("Bulk job id:" + jobId + " with batch size:" + totalToGenerate);
				if (BULK_JOB_STATUS.COMPLETED_WITH_SUCCESS.getCode() == waitForCompleteStatus(jobId, getTimeOutInSeconds())) {
					JSONArray items = resty.json(urlHelper.getBulkJobResultUrl(jobId, token)).array();
					for (int i =0;i < items.length();i++) {
						result.add(new Long((String)items.getJSONObject(i).get(SCTID)));
					}
				}
			} else {
				String statusMsg = getFailureMessage(response);
				LOGGER.error(statusMsg);
				throw new RestClientException(statusMsg);
			}
		} catch (Exception e) {
			String message = "Bulk generating sctIds job failed.";
			LOGGER.error(message, e);
			throw new RestClientException(message,e);
		}
		
		LOGGER.debug("End generating sctIds with batch size {} for namespace {}", totalToGenerate, namespaceId);
		LOGGER.info("Time taken in seconds:" + (new Date().getTime() - startTime) /1000);
		return result;
	}
}

package org.ihtsdo.buildcloud.service.identifier.client;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.http.HttpStatus;
import org.ihtsdo.otf.rest.client.RestClientException;
import org.ihtsdo.otf.rest.client.resty.RestyHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import us.monoid.json.JSONArray;
import us.monoid.json.JSONObject;
import us.monoid.web.JSONResource;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;


public class IdServiceRestClientImpl implements IdServiceRestClient {
	private static final String SRS = "srs";
	private static final String SYSTEM_IDS = "systemIds";
	private static final String QUANTITY = "quantity";
	private static final String COMMENT2 = "comment";
	private static final String GENERATE_LEGACY_IDS = "generateLegacyIds";
	private static final String SOFTWARE = "software";
	private static final String PARTITION_ID = "partitionId";
	private static final String NAMESPACE = "namespace";
	private static final String SYSTEM_ID = "systemId";
	private static final String SCTID = "sctid";
	private static final String APPLICATION_JSON = "application/json";
	public static final String ANY_CONTENT_TYPE = "*/*";
	private String idServiceUrl;
	private RestyHelper resty;
	private IdServiceRestUrlHelper urlHelper;
	private Gson gson;
	private String token;
	private static final Logger LOGGER = LoggerFactory.getLogger(IdServiceRestClientImpl.class);
	private static final String BULK_JOB_COMPLETE_STATUS = "2";
	private static final int timeOutInSeconds = 60;
	private int maxTries;
	private int retryDelaySeconds;
	
	public IdServiceRestClientImpl(String idServiceUrl, String username, String password) throws RestClientException {
		this.idServiceUrl = idServiceUrl;
		urlHelper = new IdServiceRestUrlHelper(idServiceUrl);
		this.resty = new RestyHelper(ANY_CONTENT_TYPE);
		gson = new GsonBuilder().setPrettyPrinting().create();
		resty.authenticate(idServiceUrl, username, password.toCharArray());
		token = logIn(username,password);
	}

	@Override
	public String logIn(String username, String password) throws RestClientException {
		JSONResource response = null;
		String token = null;
		try {
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("username", username);
			jsonObject.put("password", password);
			response = resty.json(urlHelper.getLoginUrl(), RestyHelper.content((jsonObject)));
			token = (String) response.get("token");
		} catch (Exception e) {
			throw new RestClientException("Failed to login for user name:" + username, e);
		}
		this.token = token;
		return token;
	}
	
	@Override
	public Long getOrCreateSctId(UUID componentUuid, Integer namespaceId, String partitionId, String comment) throws RestClientException {
		Long result = null;
		int attempt = 1;
		while (result == null) {
				JSONResource response = null;
				try {
					JSONObject requestData = new JSONObject();
					requestData.put(NAMESPACE, namespaceId.intValue());
					requestData.put(PARTITION_ID, partitionId);
					requestData.put(SYSTEM_ID, componentUuid.toString());
					requestData.put(SOFTWARE, SRS);
					requestData.put(GENERATE_LEGACY_IDS, "false");
					requestData.put(COMMENT2, comment);
					response = resty.json(urlHelper.getSctIdGenerateUrl(token), RestyHelper.content((requestData),APPLICATION_JSON));
					if ( HttpStatus.SC_OK == (response.getHTTPStatus()) ){
						 result = new Long((String)response.get(SCTID));
					} else {
						throw new RestClientException("http status code is:" + response.getHTTPStatus());
					}
				} catch (Exception e) {
					
					if (attempt < maxTries) {
						LOGGER.warn("Id service failed on attempt {}. Waiting {} seconds before retrying.", attempt, retryDelaySeconds, e);
						attempt++;
						try {
							Thread.sleep(retryDelaySeconds * 1000);
						} catch (InterruptedException ie) {
							LOGGER.warn("Retry dealy interrupted.",e);
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
		LOGGER.debug("Start creating sctIds with batch size %d for namespace %s and partitionId %s", uuids.size(), namespaceId, partitionId);
		HashMap<UUID, Long> result = new HashMap<>();
		List<String> uuidStrings = new ArrayList<>();
		for (UUID uuid : uuids) {
			uuidStrings.add(uuid.toString());
		}
		try {
			JSONObject requestData = new JSONObject();
			requestData.put(NAMESPACE, namespaceId.intValue());
			requestData.put(PARTITION_ID, partitionId);
			requestData.put(QUANTITY,uuids.size());
			requestData.put(SYSTEM_IDS, uuidStrings.toArray());
			requestData.put(SOFTWARE, SRS);
			requestData.put(GENERATE_LEGACY_IDS, "false");
			requestData.put(COMMENT2, comment);
			JSONResource response = resty.json(urlHelper.getSctIdBulkGenerateUrl(token), RestyHelper.content((requestData),APPLICATION_JSON));
			if ( HttpStatus.SC_OK == response.getHTTPStatus()) {
				String jobId =  response.get("id").toString();
				LOGGER.debug("Bulk job id:" + jobId);
				if (waitForCompleteStatus(urlHelper.getBulkJobStatusUrl(token, jobId), timeOutInSeconds)) {
					JSONArray items = resty.json(urlHelper.getBulkJobResultUrl(jobId, token)).array();
					for (int i =0;i < items.length();i++) {
						result.put(UUID.fromString((String)items.getJSONObject(i).get(SYSTEM_ID)), new Long((String)items.getJSONObject(i).get(SCTID)));
					}
				}
			} else {
				throw new RestClientException("Status is:" + response.getHTTPStatus());
			}
		} catch (Exception e) {
			throw new RestClientException("Failed to bulk getOrCreateSctds",e);
		}
		LOGGER.debug("End creating sctIds with batch size %d for namespace %s and partitionId %s", uuids.size(), namespaceId, partitionId);
		return result;
	}

	@Override
	public Map<UUID, String> getOrCreateSchemeIds(List<UUID> uuids, SchemeIdType schemeType, String comment) throws RestClientException {
		LOGGER.debug("Start creating scheme id {} with batch size {} ", schemeType, uuids.size());
		HashMap<UUID, String> result = new HashMap<>();
		List<String> uuidStrings = new ArrayList<>();
		for (UUID uuid : uuids) {
			uuidStrings.add(uuid.toString());
		}
		try {
			JSONObject requestData = new JSONObject();
			requestData.put(QUANTITY,uuids.size());
			requestData.put(SYSTEM_IDS, uuidStrings.toArray());
			requestData.put(SOFTWARE, SRS);
			requestData.put(COMMENT2, comment);
			JSONResource response = resty.json(urlHelper.getSchemeIdBulkGenerateUrl(token, schemeType), RestyHelper.content((requestData),APPLICATION_JSON));
			if ( HttpStatus.SC_OK == response.getHTTPStatus()) {
				String jobId =  response.get("id").toString();
				LOGGER.debug("Scheme ids bulk job id:" + jobId);
				if (waitForCompleteStatus(urlHelper.getBulkJobStatusUrl(token, jobId), timeOutInSeconds)) {
					JSONArray items = resty.json(urlHelper.getBulkJobResultUrl(jobId, token)).array();
					for (int i =0;i < items.length();i++) {
						result.put(UUID.fromString((String)items.getJSONObject(i).get(SYSTEM_ID)), (String)items.getJSONObject(i).get("schemeId"));
					}
				}
			} else {
				throw new RestClientException("Status is:" + response.getHTTPStatus());
			}
		} catch (Exception e) {
			throw new RestClientException("Failed to bulk getOrCreateSctds",e);
		}
		LOGGER.debug("End creating scheme id {} with batch size {} ", schemeType, uuids.size());
		return result;
	}
	
	
	private boolean waitForCompleteStatus(String url, int timeoutInSeconds)
			throws RestClientException, InterruptedException {
		long startTime = new Date().getTime();
		String status = "";
		boolean isCompleted = false;
		while (!isCompleted) {
			try {
				Object statusObj = resty.json(url).get("status");
				status = statusObj.toString() ;
			} catch (Exception e) {
				throw new RestClientException("Rest client error while checking bulk job status:" + url, e);
			}
			isCompleted = (BULK_JOB_COMPLETE_STATUS.equals(status));
			if (!isCompleted && ((new Date().getTime() - startTime) > timeoutInSeconds *1000)) {
				throw new RestClientException("Client timeout waiting for bulk job complete status:" + url );
			}
			if (!isCompleted) {
				Thread.sleep(1000 * 10);
			}
		}
		if (!isCompleted) {
			LOGGER.warn("ID service bulk job has non-complete status {} from URL {}", status, url);
		}
		return isCompleted;
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
	
	
}

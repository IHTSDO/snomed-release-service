package org.ihtsdo.buildcloud.core.service.identifier.client;

import java.util.Collection;

public class IdServiceRestUrlHelper {
	private static final String TOKEN_PARAMETER = "?token=";
	private final String rootApiUrl;
	public IdServiceRestUrlHelper(String idServiceUrl) {
		rootApiUrl = idServiceUrl;
	}

	public String getLoginUrl() {
		return  rootApiUrl + "/login";
	}
	
	public String getLogoutUrl() {
		return  rootApiUrl + "/logout";
	}

	public String getSctIdGenerateUrl(String token) {
		return rootApiUrl + "/sct/generate" + TOKEN_PARAMETER + token;
	}

	public String getSctIdBulkGenerateUrl(String token) {
		return rootApiUrl + "/sct/bulk/generate" + TOKEN_PARAMETER + token;
	}
	
	public String getSctIdBulkUrl(String token, Collection<Long> sctIds) {
		StringBuilder urlBuilder =  new StringBuilder(rootApiUrl + "/sct/bulk/ids" + TOKEN_PARAMETER + token + "&sctids=");
		boolean isFirstOne = true;
		for ( Long sctId : sctIds) {
			if (!isFirstOne) {
				urlBuilder.append(",");
			}
			if ( isFirstOne) {
				isFirstOne = false;
			}
			urlBuilder.append(sctId.toString());
		}
		return urlBuilder.toString();
	}
	
	
	/** This is for post api to retrieve a bigger list of sctids
	 * @param token
	 * @return url
	 */
	public String getSctIdBulkUrl(String token) {
		return rootApiUrl + "/sct/bulk/ids" + TOKEN_PARAMETER + token;
		
	}
	
	public String getBulkJobResultUrl(String jobId, String token) {
		return rootApiUrl + "/bulk/jobs/" + jobId +"/records" + TOKEN_PARAMETER + token;
	}
	
	public String getBulkJobStatusUrl(String token, String jobId) {
		return rootApiUrl + "/bulk/jobs/" + jobId + TOKEN_PARAMETER + token;
	}
	
	public String getSchemeIdBulkGenerateUrl(String token, SchemeIdType schemeType) {
		return rootApiUrl + "/scheme/" + schemeType + "/bulk/generate" + TOKEN_PARAMETER + token;
	}

	public String getSctIdBulkPublishingUrl(String token) {
		return rootApiUrl + "/sct/bulk/publish" + TOKEN_PARAMETER + token;
	}

	public String getSchemeIdBulkPublishingUrl(SchemeIdType schemeType, String token) {
		return rootApiUrl + "/scheme/" + schemeType + "/bulk/publish" + TOKEN_PARAMETER + token;
	}

	public String getSchemeIdBulkUrl(String token, SchemeIdType schemeType,Collection<String> legacyIds) {
		
		StringBuilder urlBuilder =  new StringBuilder(rootApiUrl + "/scheme/" + schemeType + "/bulk" + TOKEN_PARAMETER + token + "&schemeIds=");
		boolean isFirstOne = true;
		for ( String legacyId : legacyIds) {
			if (!isFirstOne) {
				urlBuilder.append(",");
			}
			if ( isFirstOne) {
				isFirstOne = false;
			}
			urlBuilder.append(legacyId);
		}
		return urlBuilder.toString();
	}

	public String getTestServiceUrl() {
		return rootApiUrl + "/testService";
	}

	public String getTokenAuthenticationUrl() {
		return rootApiUrl + "/authenticate";
	}

	public String getSctIdBulkRegisterUrl(String token) {
		return rootApiUrl + "/sct/bulk/register" + TOKEN_PARAMETER + token;
	}
	
	public String getSctIdBulkReserveUrl(String token) {
		return rootApiUrl + "/sct/bulk/reserve" + TOKEN_PARAMETER + token;
	}
	
}

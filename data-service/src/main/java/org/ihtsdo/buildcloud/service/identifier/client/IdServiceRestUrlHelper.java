package org.ihtsdo.buildcloud.service.identifier.client;



public class IdServiceRestUrlHelper {
	private static final String TOKEN_PARAMETER = "?token=";
	private String rootApiUrl;
	public IdServiceRestUrlHelper(String idServiceUrl) {
		rootApiUrl = idServiceUrl;
	}

	public String getLoginUrl() {
		return  rootApiUrl + "/login";
	}

	public String getSctIdGenerateUrl(String token) {
		return rootApiUrl + "/sct/generate" + TOKEN_PARAMETER + token;
	}

	public String getSctIdBulkGenerateUrl(String token) {
		return rootApiUrl + "/sct/bulk/generate" + TOKEN_PARAMETER + token;
	}
	public String getBulkJobResultUrl(String jobId, String token) {
		return rootApiUrl + "/bulk/jobs/" + jobId +"/records" + TOKEN_PARAMETER + token;
	}
	
	public String getBulkJobStatusUrl(String token, String jobId) {
		return rootApiUrl + "/bulk/jobs/" + jobId + TOKEN_PARAMETER + token;
	}
	
	public String getSchemeIdBulkGenerateUrl(String token, SchemeIdType schemeType) {
		return rootApiUrl + "/scheme/"+ schemeType + "/bulk/generate" + TOKEN_PARAMETER + token;
	}
}

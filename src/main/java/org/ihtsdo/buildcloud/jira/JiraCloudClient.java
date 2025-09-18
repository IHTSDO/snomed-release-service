package org.ihtsdo.buildcloud.jira;

import net.sf.json.JSONObject;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Simple Jira Cloud REST client for basic operations.
 */
public class JiraCloudClient {

    public static final String REST_API_LATEST_ISSUE = "rest/api/latest/issue/";
    public static final String APPLICATION_JSON = "application/json";
    private final String baseUrl;
    private final String authHeader;

    /**
     * @param baseUrl  Jira Cloud instance URL
     * @param email    Jira user email
     * @param apiToken Jira API token
     */
    public JiraCloudClient(String baseUrl, String email, String apiToken) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
        String auth = email + ":" + apiToken;
        this.authHeader = "Basic " + Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
    }


    public String getBaseUrl() {
        return baseUrl;
    }

    /**
     * Create a Jira issue (fields must follow Jira Cloud API format).
     */
    public JSONObject createIssue(String projectKey, String summary, String description, String issueType, String reporter) throws IOException, BusinessServiceException {
        String url = baseUrl + "rest/api/latest/issue";
        HttpPost request = new HttpPost(url);
        request.setHeader(HttpHeaders.AUTHORIZATION, authHeader);
        request.setHeader(HttpHeaders.ACCEPT, APPLICATION_JSON);
        request.setHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON);

        // Construct JSON payload
        JSONObject issueFields = new JSONObject();
        JSONObject projectField = new JSONObject();
        projectField.put("key", projectKey);
        JSONObject issueTypeField = new JSONObject();
        issueTypeField.put("name", issueType);
        JSONObject reporterField = new JSONObject();
        reporterField.put("accountId", reporter);

        issueFields.put("project", projectField);
        issueFields.put("summary", summary);
        issueFields.put("description", description);
        issueFields.put("issuetype", issueTypeField);
        issueFields.put("reporter", reporterField);

        JSONObject payload = new JSONObject();
        payload.put("fields", issueFields);
        request.setEntity(new StringEntity(payload.toString(), StandardCharsets.UTF_8));
        try (CloseableHttpClient client = HttpClients.createDefault();
             CloseableHttpResponse response = client.execute(request)) {
            int statusCode = response.getStatusLine().getStatusCode();
            String json = EntityUtils.toString(response.getEntity());
            if (statusCode >= 200 && statusCode < 300) {
                return JSONObject.fromObject(json);
            }
            // Return an exception with the error message from Jira response
            JSONObject errorObj = JSONObject.fromObject(json);
            String errorMsg = errorObj.has("errorMessages") ? errorObj.getJSONArray("errorMessages").toString() : json;
            throw new BusinessServiceException("Failed to create issue: HTTP " + statusCode + " - " + errorMsg);
        }
    }


    /**
     * Update a Jira issue by key. Fields must follow Jira Cloud API format.
     *
     * @param issueKey Jira issue key (e.g., "PROJ-123")
     * @param fields   Fields to update (as per Jira Cloud API)
     */
    public void updateIssue(String issueKey, JSONObject fields) throws IOException, BusinessServiceException {
        String url = baseUrl + REST_API_LATEST_ISSUE + issueKey;
        org.apache.http.client.methods.HttpPut request = new org.apache.http.client.methods.HttpPut(url);
        request.setHeader(HttpHeaders.AUTHORIZATION, authHeader);
        request.setHeader(HttpHeaders.ACCEPT, APPLICATION_JSON);
        request.setHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON);
        JSONObject payload = new JSONObject();
        payload.put("fields", fields);
        request.setEntity(new StringEntity(payload.toString(), StandardCharsets.UTF_8));
        try (CloseableHttpClient client = HttpClients.createDefault();
             CloseableHttpResponse response = client.execute(request)) {
            int statusCode = response.getStatusLine().getStatusCode();
            String json = response.getEntity() != null ? EntityUtils.toString(response.getEntity()) : "";
            JSONObject result = new JSONObject();
            result.put("status", statusCode);
            if (statusCode >= 200 && statusCode < 300) {
                // Jira Cloud returns 204 No Content for successful update
                result.put("message", json.isEmpty() ? "No Content" : json);
            } else {
                throw new BusinessServiceException("Failed to update issue: HTTP " + statusCode + " - " + (json.isEmpty() ? "Unknown error" : json));
            }
        }
    }

    /**
     * Add an attachment to a Jira issue.
     *
     * @param issueKey  Jira issue key (e.g., "PROJ-123")
     * @param fileName  Name of the file to attach
     * @param fileBytes File content as byte array
     */
    public void addAttachment(String issueKey, String fileName, byte[] fileBytes) throws IOException, BusinessServiceException {
        String url = baseUrl + REST_API_LATEST_ISSUE + issueKey + "/attachments";
        org.apache.http.client.methods.HttpPost request = new org.apache.http.client.methods.HttpPost(url);
        request.setHeader(HttpHeaders.AUTHORIZATION, authHeader);
        request.setHeader(HttpHeaders.ACCEPT, APPLICATION_JSON);
        request.setHeader("X-Atlassian-Token", "no-check");

        org.apache.http.entity.mime.MultipartEntityBuilder builder = org.apache.http.entity.mime.MultipartEntityBuilder.create();
        builder.addBinaryBody("file", fileBytes, org.apache.http.entity.ContentType.DEFAULT_BINARY, fileName);
        request.setEntity(builder.build());

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            try (CloseableHttpResponse response = client.execute(request)) {
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode < 200 || statusCode >= 300) {
                    String errorMsg = response.getEntity() != null ? EntityUtils.toString(response.getEntity()) : "";
                    throw new BusinessServiceException("Failed to add attachment: HTTP " + statusCode + " - " + errorMsg);
                }
            }
        }
    }

    public void addWatcher(String issueKey, String accountId) throws IOException, BusinessServiceException {
        String url = baseUrl + REST_API_LATEST_ISSUE + issueKey + "/watchers";
        HttpPost request = new HttpPost(url);
        request.setHeader(HttpHeaders.AUTHORIZATION, authHeader);
        request.setHeader(HttpHeaders.ACCEPT, APPLICATION_JSON);
        request.setHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON);
        request.setEntity(new StringEntity("\"" + accountId + "\"", StandardCharsets.UTF_8));
        try (CloseableHttpClient client = HttpClients.createDefault();
             CloseableHttpResponse response = client.execute(request)) {
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode < 200 || statusCode >= 300) {
                String errorMsg = EntityUtils.toString(response.getEntity());
                throw new BusinessServiceException("Failed to add watcher: HTTP " + statusCode + " - " + errorMsg);
            }
        }
    }
}

package org.ihtsdo.buildcloud.core.service.browser.update;

import org.apache.commons.io.FileUtils;
import org.ihtsdo.otf.rest.client.RestClientException;
import org.ihtsdo.otf.rest.client.terminologyserver.pojo.CodeSystem;
import org.ihtsdo.otf.rest.client.terminologyserver.pojo.CodeSystemUpgradeJob;
import org.ihtsdo.otf.rest.client.terminologyserver.pojo.CodeSystemVersion;
import org.ihtsdo.otf.rest.client.terminologyserver.pojo.ItemsPage;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.otf.rest.exception.ProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BrowserUpdateClient {

    private static final Logger logger = LoggerFactory.getLogger(BrowserUpdateClient.class);

    private static final String CODE_SYSTEM_ENDPOINT = "/codesystems/";

    private static final ParameterizedTypeReference<ItemsPage<CodeSystemVersion>> CODESYSTEM_VERSION_PAGE_TYPE_REFERENCE = new ParameterizedTypeReference<>() {
    };
    private String browserUpdateTermServerUrl;
    private RestTemplate restTemplate;
    private HttpHeaders headers;

    private String singleSignOnCookie;

    public BrowserUpdateClient(String browserUpdateTermServerUrl, String authToken) {
        this.browserUpdateTermServerUrl = browserUpdateTermServerUrl;
        this.singleSignOnCookie = authToken;
        headers = new HttpHeaders();
        headers.add("Cookie", authToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        restTemplate = new RestTemplate();

        //Add a ClientHttpRequestInterceptor to the RestTemplate to add cookies as required
        restTemplate.getInterceptors().add((request, body, execution) -> {
            request.getHeaders().addAll(headers);
            return execution.execute(request, body);
        });
    }

    public void checkVersion() throws BusinessServiceException {
        ResponseEntity<Void> response = restTemplate.exchange(this.browserUpdateTermServerUrl + "/version", HttpMethod.GET, null, Void.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new BusinessServiceException("Could not connect to the browser update box");
        }
    }

    public CodeSystem getCodeSystem(String codeSystemShortname) {
        ResponseEntity<CodeSystem> response = restTemplate.getForEntity(this.browserUpdateTermServerUrl + CODE_SYSTEM_ENDPOINT + codeSystemShortname, CodeSystem.class);
        return response.getBody();
    }

    public List<CodeSystemVersion> getCodeSystemVersions(String codeSystemShortname, boolean showFutureVersions) {
        ResponseEntity<ItemsPage<CodeSystemVersion>> response = restTemplate.exchange(this.browserUpdateTermServerUrl + CODE_SYSTEM_ENDPOINT + codeSystemShortname + "/versions?showFutureVersions=" + showFutureVersions, HttpMethod.GET, null, CODESYSTEM_VERSION_PAGE_TYPE_REFERENCE);
        ItemsPage<CodeSystemVersion> page = response.getBody();
        return page != null ? page.getItems() : Collections.emptyList();
    }

    public void rollBackDailyBuild(String codeSystemShortName) throws RestClientException {
        try {
            restTemplate.postForObject(this.browserUpdateTermServerUrl + CODE_SYSTEM_ENDPOINT + codeSystemShortName + "/daily-build/rollback", null, Void.class);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            String errorMessage = String.format("Failed to rollback the daily build content for code system %s. Error message: %s", codeSystemShortName, e.getMessage());
            logger.error(errorMessage);
            throw new RestClientException(errorMessage);
        }
    }

    public String createImportJob(String type, String branchPath) throws BusinessServiceException {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("type", type);
        requestBody.put("branchPath", branchPath);
        requestBody.put("createCodeSystemVersion", true);

        URI uri = restTemplate.postForLocation(this.browserUpdateTermServerUrl + "/imports", requestBody);
        if (uri == null) {
            throw new BusinessServiceException("Failed to create import job");
        }
        String uriStr = uri.toString();
        return uriStr.substring(uriStr.lastIndexOf("/") + 1);
    }

    public ImportJob getImportJob(String importId) {
        ResponseEntity<ImportJob> response = restTemplate.getForEntity(this.browserUpdateTermServerUrl + "/imports/" + importId, ImportJob.class);
        return response.getBody();
    }

    public void uploadImportRf2Archive(String importId, File file) throws IOException {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        MultiValueMap<String, String> fileMap = new LinkedMultiValueMap<>();
        ContentDisposition contentDisposition = ContentDisposition.builder("form-data").name("file").filename(file.getName()).build();
        fileMap.add(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString());
        fileMap.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE);
        HttpEntity<byte[]> fileEntity = new HttpEntity<>(FileUtils.readFileToByteArray(file), fileMap);
        body.add("file", fileEntity);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, httpHeaders);

        restTemplate.exchange(this.browserUpdateTermServerUrl + "/imports/" + importId + "/archive", HttpMethod.POST, requestEntity, Void.class);
    }

    public String upgradeCodeSystem(String codeSystemShortName, Integer newDependantVersion) throws ProcessingException {
        Map<String, Object> request = new HashMap<>();
        request.put("newDependantVersion", newDependantVersion);
        request.put("contentAutomations", false);

        String uri = this.browserUpdateTermServerUrl + CODE_SYSTEM_ENDPOINT + codeSystemShortName + "/upgrade";
        RequestEntity<?> post = RequestEntity.post(uri)
                .header("Cookie", singleSignOnCookie)
                .accept(MediaType.APPLICATION_JSON)
                .body(request);

        URI location = restTemplate.postForLocation(uri, post);
        if (location == null) {
            throw new ProcessingException("Failed to obtain location of code system upgrade");
        }
        return location.toString();
    }

    public CodeSystemUpgradeJob getCodeSystemUpgradeJob(String jobId) {
        ResponseEntity<CodeSystemUpgradeJob> response = restTemplate.getForEntity(this.browserUpdateTermServerUrl + CODE_SYSTEM_ENDPOINT + "upgrade/" + jobId, CodeSystemUpgradeJob.class);
        return response.getBody();
    }
}


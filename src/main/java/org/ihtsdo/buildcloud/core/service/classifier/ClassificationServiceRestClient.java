package org.ihtsdo.buildcloud.core.service.classifier;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Collections;
import java.util.Date;
import java.util.Optional;

import org.apache.tomcat.util.http.fileupload.util.Streams;
import org.ihtsdo.otf.rest.client.RestClientException;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestTemplate;

@Service
public class ClassificationServiceRestClient {

	private static final Logger LOGGER = LoggerFactory.getLogger(ClassificationServiceRestClient.class);
	private final RestTemplate restTemplate;

	@Value("${classification-service.timeoutInSeconds:300}")
	private int timeoutInSeconds = 300;

	private static final HttpHeaders MULTIPART_HEADERS = new HttpHeaders();
	static {
		MULTIPART_HEADERS.setContentType(MediaType.MULTIPART_FORM_DATA);
	}
	
	public ClassificationServiceRestClient(@Value("${classification-service.url}") final String serviceUrl,
			@Value("${classification-service.username}") final String username, @Value("${classification-service.password}") final String password) {
		restTemplate = new RestTemplateBuilder()
				.rootUri(serviceUrl)
				.basicAuthentication(username, password)
				.build();
	}

	public File classify( File rf2DeltaZipFile, String previousPackage, String dependencyPackage) throws BusinessServiceException {
		MultiValueMap<String, Object> params = new LinkedMultiValueMap<>();
		if (StringUtils.hasLength(previousPackage)) {
			params.put("previousPackage", Collections.singletonList(previousPackage));
		}
		if (StringUtils.hasLength(dependencyPackage)) {
			params.put("dependencyPackage", Collections.singletonList(dependencyPackage));
		}
		params.put("rf2Delta", Collections.singletonList(new FileSystemResource(rf2DeltaZipFile)));
		ResponseEntity<Void> response = restTemplate.postForEntity("/classifications", new HttpEntity<>(params, MULTIPART_HEADERS), Void.class);
		String statusUrl = response.getHeaders().getLocation().toString();
		String classificationId = statusUrl.substring(statusUrl.lastIndexOf("/") + 1);
		try {
			//wait for the classification to finish
			waitForCompleteStatus(statusUrl, classificationId, timeoutInSeconds);
		} catch(Exception e) {
			throw new BusinessServiceException("Error occurred when polling classification status:" + statusUrl, e);
		}

		// Download RF2 results
		try {
			File archive = File.createTempFile("result_", classificationId + ".zip");
			ResponseExtractor<Void> responseExtractor = rf2ResultsRresponse -> {
				try (FileOutputStream outputStream = new FileOutputStream(archive)) {
					Streams.copy(rf2ResultsRresponse.getBody(), outputStream, true);
					return null;
				}
			};
			restTemplate.execute("/classifications/{classificationId}/results/rf2", HttpMethod.GET, clientHttpRequest -> {}, responseExtractor, classificationId);
			return archive;
		} catch (Exception e) {
			throw new BusinessServiceException("Failed to download classification result for id " + classificationId, e);
		}
	}

	private void waitForCompleteStatus(String statusUrl, String classificationId, int timeoutInSeconds)
			throws RestClientException, InterruptedException {
		long startTime = new Date().getTime();
		String status = null;
		boolean isDone = false;
		String errorMsg = null;
		String developerMsg = null;
		while (!isDone) {
			try {
				Optional<ClassificationStatusResponse> response = getStatusChange(classificationId);
				if (response.isPresent()) {
					ClassificationStatusResponse classificationStatusResponse = response.get();
					status = classificationStatusResponse.getStatus() != null ? classificationStatusResponse.getStatus() : null;
					if ("FAILED".equalsIgnoreCase(status)) {
						errorMsg = classificationStatusResponse.getErrorMessage();
						developerMsg = classificationStatusResponse.getDeveloperMessage();
					}
				}
			} catch (Exception e) {
				String msg = "Error occurred when checking the classification status:" + statusUrl;
				LOGGER.error(msg, e);
				throw new RestClientException(msg, e);
			}
			isDone = (!"SCHEDULED".equalsIgnoreCase(status) && !"RUNNING".equalsIgnoreCase(status));
			if (!isDone && ((new Date().getTime() - startTime) > (timeoutInSeconds * 1000L))) {
				String message = "Timeout after waiting " + timeoutInSeconds + " seconds for classification to finish:" + statusUrl;
				LOGGER.warn(message);
				throw new RestClientException(message);
			}
			if (!isDone) {
				Thread.sleep(1000 * 10L);
			}
		}

		if ("FAILED".equalsIgnoreCase(status)) {
			throw new RestClientException("Classification failed with error message:" + errorMsg + " developer message:" + developerMsg);
		}
	}

	public Optional<ClassificationStatusResponse> getStatusChange(String classificationId) {
		return Optional.ofNullable(restTemplate.getForObject("/classifications/{classificationId}", ClassificationStatusResponse.class, classificationId));
	}

	public int getTimeoutInSeconds() {
		return timeoutInSeconds;
	}

	public void setTimeoutInSeconds(int timeoutInSeconds) {
		this.timeoutInSeconds = timeoutInSeconds;
	}
}

package org.ihtsdo.buildcloud.service.classifier;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Date;

import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.ihtsdo.otf.rest.client.RestClientException;
import org.ihtsdo.otf.rest.client.resty.HttpEntityContent;
import org.ihtsdo.otf.rest.client.resty.RestyHelper;
import org.ihtsdo.otf.rest.client.resty.RestyServiceHelper;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import com.google.common.base.Strings;

import us.monoid.json.JSONException;
import us.monoid.web.BinaryResource;
import us.monoid.web.JSONResource;

@Service
public class ClassificationServiceRestClient {

	private static final Logger LOGGER = LoggerFactory.getLogger(ClassificationServiceRestClient.class);

	private final String classificationServiceUrl;
	private final String username;
	private final String password;
	public static final String ANY_CONTENT_TYPE = "*/*";
	protected static final String CONTENT_TYPE_MULTIPART = "multipart/form-data";
	private final RestyHelper resty;
	private static final String STATUS = "status";

	@Value("${external.classifier.timeoutInSeconds:300}")
	private int timeoutInSeconds = 300;
	
	public ClassificationServiceRestClient(@Value("${external.classifier.url}") final String serviceUrl,
			@Value("${external.classifier.username}") final String username, @Value("${external.classifier.password}") final String password) {
		this.resty = new RestyHelper(ANY_CONTENT_TYPE);
		this.classificationServiceUrl = serviceUrl;
		this.username = username;
		this.password = password;
	}
	
	
	public File classify( File rf2DeltaZipFile, String previousPackage, String dependencyPackage) throws BusinessServiceException {
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(classificationServiceUrl + "/classifications");
		if (!Strings.isNullOrEmpty(previousPackage)) {
			builder.queryParam("previousPackage", previousPackage);
		}
		if (!Strings.isNullOrEmpty(dependencyPackage)) {
			builder.queryParam("dependencyPackage", dependencyPackage);
		}
		URI uri = builder.build().toUri();
		LOGGER.info("External classifier request url=" + uri.toString());
		MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create();
		multipartEntityBuilder.addBinaryBody("rf2Delta", rf2DeltaZipFile, ContentType.create(CONTENT_TYPE_MULTIPART), rf2DeltaZipFile.getName());
		multipartEntityBuilder.setCharset(StandardCharsets.UTF_8);
		multipartEntityBuilder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
		HttpEntity httpEntity = multipartEntityBuilder.build();
		resty.authenticate(classificationServiceUrl, username, password.toCharArray());
		resty.withHeader("Accept", ANY_CONTENT_TYPE);
		
		String statusUrl = null;
		try {
			JSONResource response = resty.json(uri, new HttpEntityContent(httpEntity));
			RestyServiceHelper.ensureSuccessfull(response);
			statusUrl = response.http().getHeaderField("location");
			LOGGER.info("classification request is submitted." + statusUrl );
		} catch (IOException | JSONException e) {
			throw new BusinessServiceException("Failed to send classification request.", e);
		}
	
		try {
			//wait for the classification to finish
			waitForCompleteStatus(statusUrl, timeoutInSeconds);
		} catch(Exception e) {
			throw new BusinessServiceException("Error occured when polling classification status:" + statusUrl, e);
		}
		
		try {
			// retrieve results when status is completed
			String classificationId = getClassificationId(statusUrl);
			String resultUrl = classificationServiceUrl + "/classifications/" + classificationId + "/results/rf2";
			LOGGER.info("Classification result:" + resultUrl);
			File archive = File.createTempFile("result_", classificationId + ".zip");
			BinaryResource archiveResults = resty.bytes(resultUrl);
			archiveResults.save(archive);
			LOGGER.info("Result is archived " + archive.getAbsolutePath());
			return archive;
		} catch (Exception e) {
			throw new BusinessServiceException("Failed to download classification result via " + uri, e);
		}
	}

	private String getClassificationId(String locationUrl) throws RestClientException {
		if (locationUrl != null) {
			try {
				URL url = new URL(locationUrl);
				return Paths.get(url.getPath()).getFileName().toString();
			} catch (MalformedURLException e) {
				throw new RestClientException("Not a valid URL:" + locationUrl, e);
			}
		}
		return null;
	}

	private String waitForCompleteStatus(String classificationStatusUrl, int timeoutInSeconds)
			throws RestClientException, InterruptedException {
		long startTime = new Date().getTime();
		String status = null;
		boolean isDone = false;
		String errorMsg = null;
		String developerMsg = null;
		while (!isDone) {
			try {
				JSONResource response = resty.json(classificationStatusUrl);
				status = response.get(STATUS) != null ? response.get(STATUS).toString() : null;
				if ("FAILED".equalsIgnoreCase(status)) {
					errorMsg = response.get("errorMessage") != null ? response.get("errorMessage").toString() : null;
					developerMsg = response.get("developerMessage") != null ? response.get("developerMessage").toString() : null;
				}
			} catch (Exception e) {
				String msg = "Error occurred when checking the classification status:" + classificationStatusUrl;
				LOGGER.error(msg, e);
				throw new RestClientException(msg, e);
			}
			isDone = (!"SCHEDULED".equalsIgnoreCase(status) && !"RUNNING".equalsIgnoreCase(status));
			if (!isDone && ((new Date().getTime() - startTime) > timeoutInSeconds *1000)) {
				String message = "Timeout after waiting " + timeoutInSeconds + " seconds for classification to finish:" + classificationStatusUrl;
				LOGGER.warn(message);
				throw new RestClientException(message);
			}
			if (!isDone) {
				Thread.sleep(1000 * 10);
			}
		}

		if (isDone && "FAILED".equalsIgnoreCase(status)) {
			throw new RestClientException("Classification failed with error message:" + errorMsg + " developer message:" + developerMsg);
		}
		return status;
	}

	public int getTimeoutInSeconds() {
		return timeoutInSeconds;
	}

	public void setTimeoutInSeconds(int timeoutInSeconds) {
		this.timeoutInSeconds = timeoutInSeconds;
	}
}

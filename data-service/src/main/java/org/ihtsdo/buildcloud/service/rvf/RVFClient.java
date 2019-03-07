package org.ihtsdo.buildcloud.service.rvf;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.concurrent.ExecutionException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.ihtsdo.buildcloud.dao.io.AsyncPipedStreamBean;
import org.ihtsdo.buildcloud.entity.QATestConfig;
import org.ihtsdo.buildcloud.service.build.RF2Constants;
import org.ihtsdo.otf.rest.exception.ApplicationWiringException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StreamUtils;

public class RVFClient implements Closeable {

	private static final String ENABLE_MRCM_VALIDATION = "enableMRCMValidation";

	private static final String CREATE_JIRA_ISSUE = "jiraIssueCreationFlag";

	private static final String PRODUCT_NAME = "productName";

	private static final String REPORTING_STAGE = "reportingStage";

	private static final String DROOLS_RULES_GROUPS = "droolsRulesGroups";

	private static final String INCLUDED_MODULES = "includedModules";

	private static final String EFFECTIVE_TIME = "effectiveTime";

	private static final String STORAGE_LOCATION = "storageLocation";

	private static final String FAILURE_EXPORT_MAX = "failureExportMax";

	private static final String RUN_ID = "runId";

	private static final String DEPENDENCY_RELEASE = "dependencyRelease";

	private static final String PREVIOUS_RELEASE = "previousRelease";

	private static final String RELEASE_AS_AN_EDITION = "releaseAsAnEdition";

	private static final String GROUPS = "groups";

	private static final String ENABLE_DROOLS = "enableDrools";

	private static final String MANIFEST_FILE_S3_PATH = "manifestFileS3Path";

	private static final String RELEASE_FILE_S3_PATH = "releaseFileS3Path";

	private static final String BUCKET_NAME = "bucketName";

	private static final String RUN_POST_VIA_S3 = "/run-post-via-s3";

	private static final String LOCATION = "location";

	public static final String TOTAL_NUMBER_OF_FAILURES = "Total number of failures: ";

	private static final String ERROR_NO_LINES_RECEIVED_FROM_RVF = "Error - No lines received from RVF!";

	private static final Logger LOGGER = LoggerFactory.getLogger(RVFClient.class);

	private final String releaseValidationFrameworkUrl;

	private final CloseableHttpClient httpClient;

	public RVFClient(final String releaseValidationFrameworkUrl) {
		if (releaseValidationFrameworkUrl == null) {
			throw new ApplicationWiringException("Null RVF host URL.");
		}
		this.releaseValidationFrameworkUrl = releaseValidationFrameworkUrl;
		httpClient = HttpClients.createDefault();
	}

	public String checkInputFile(final InputStream inputFileStream, final String inputFileName, final AsyncPipedStreamBean logFileOutputStream) {
		return checkFile(inputFileStream, inputFileName, logFileOutputStream, true);
	}

	private String checkFile(final InputStream inputFileStream, final String inputFileName, final AsyncPipedStreamBean logFileOutputStream, final boolean preCheck) {
		String errorMessage = "Check not complete.";

		String fileType;
		String checkType;
		String targetUrl;

		if (preCheck) {
			fileType = "input";
			checkType = "precondition";
			targetUrl = "/test-pre";
		} else {
			fileType = "output";
			checkType = "postcondition";
			targetUrl = "/test-post";
		}

		LOGGER.info("Adding {} to RVF Request.", inputFileName);

		final HttpPost post = new HttpPost(releaseValidationFrameworkUrl + targetUrl);
		post.setEntity(MultipartEntityBuilder.create().addPart("file", new InputStreamBody(inputFileStream, inputFileName)).build());

		LOGGER.info("Posting file {} to RVF for {} check, using {}", inputFileName, checkType, targetUrl);
		
		String debugMsg = "Logged results to " + logFileOutputStream.getOutputFilePath();

		try (CloseableHttpResponse response = httpClient.execute(post)) {
			final int statusCode = response.getStatusLine().getStatusCode();
			long failureCount = 0;

			try (InputStream content = response.getEntity().getContent();
				 BufferedReader responseReader = new BufferedReader(new InputStreamReader(content, RF2Constants.UTF_8));
				 BufferedWriter logWriter = new BufferedWriter(new OutputStreamWriter(logFileOutputStream.getOutputStream(), RF2Constants.UTF_8))) {

				failureCount = processResponse(responseReader, logWriter, debugMsg);
			} finally {
				logFileOutputStream.waitForFinish();
			}

			if (200 == statusCode) {
				if (failureCount == 0) {
					errorMessage = null;
				} else {
					errorMessage = "There were " + failureCount + " RVF " + checkType + " test failures.";
				}
			} else {
				errorMessage = "RVF response HTTP status code " + statusCode;
				LOGGER.info("RVF Service failure: {}", errorMessage);
			}
		} catch (InterruptedException | ExecutionException | IOException | RVFClientException e) {
			errorMessage = "Failed to check " + fileType + " file against RVF: " + inputFileName + " due to " + e.getMessage();
			LOGGER.error(errorMessage, e);
			try (OutputStream logOutputStream = logFileOutputStream.getOutputStream()) {
				StreamUtils.copy(errorMessage.getBytes(), logOutputStream);
				logFileOutputStream.waitForFinish();
			} catch (final Exception e2) {
				LOGGER.error("Failed to write exception to log with message: " + errorMessage, e);
			}
		} finally {
			LOGGER.info("RVF {} check of {} complete.", checkType, inputFileName);
		}

		return errorMessage;
	}

	protected long processResponse(final BufferedReader responseReader, final BufferedWriter logWriter, String debugMsg) throws IOException, RVFClientException {
		long failureCount = 0;
		boolean foundFailureCount = false;
		boolean noLinesReceived = false;

		String line = responseReader.readLine(); // read header
		if (line != null) {
			// write header
			logWriter.write(line);
			logWriter.write(RF2Constants.LINE_ENDING);

			// read all other lines
			boolean endOfValuesReached = false; // Optimisation so we don't inspect every line.
			while ((line = responseReader.readLine()) != null) {
				if (endOfValuesReached) {
					if (line.startsWith(TOTAL_NUMBER_OF_FAILURES)) {
						failureCount = Integer.parseInt(line.substring(TOTAL_NUMBER_OF_FAILURES.length()));
						foundFailureCount = true;
					}
				}
				if(line.isEmpty()) {
					endOfValuesReached = true;
				}
				logWriter.write(line);
				logWriter.write(RF2Constants.LINE_ENDING);
			}
		} else {
			logWriter.write(ERROR_NO_LINES_RECEIVED_FROM_RVF);
			logWriter.write(RF2Constants.LINE_ENDING);
			noLinesReceived = true;
		}

		if (foundFailureCount) {
			return failureCount;
		} else {
			throw new RVFClientException("Failure count not found in RVF response. " + (noLinesReceived?"No data received. ":"") + debugMsg);
		}
	}

	@Override
	public void close() throws IOException {
		httpClient.close();
	}
	private HttpPost createHttpPostRequest(QATestConfig qaTestConfig, ValidationRequest request, String targetUrl) throws FileNotFoundException {
		final HttpPost post = new HttpPost(releaseValidationFrameworkUrl + targetUrl);
		final MultipartEntityBuilder multiPartBuilder = MultipartEntityBuilder.create();
		multiPartBuilder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
		multiPartBuilder.addTextBody(BUCKET_NAME, request.getBuildBucketName());
		multiPartBuilder.addTextBody(RELEASE_FILE_S3_PATH, request.getReleaseZipFileS3Path());
		multiPartBuilder.addTextBody(MANIFEST_FILE_S3_PATH, request.getManifestFileS3Path());
		multiPartBuilder.addTextBody(ENABLE_DROOLS, Boolean.toString(qaTestConfig.isEnableDrools()));
		multiPartBuilder.addTextBody(ENABLE_MRCM_VALIDATION, Boolean.toString(qaTestConfig.isEnableMRCMValidation()));
		multiPartBuilder.addTextBody(CREATE_JIRA_ISSUE, Boolean.toString(qaTestConfig.isJiraIssueCreationFlag()));
		multiPartBuilder.addTextBody(GROUPS, qaTestConfig.getAssertionGroupNames());
		multiPartBuilder.addTextBody(RELEASE_AS_AN_EDITION, Boolean.toString(request.isReleaseAsAnEdition()));
		String previousIntRelease = qaTestConfig.getPreviousInternationalRelease();
		if ( previousIntRelease != null && !previousIntRelease.isEmpty() ) {
			multiPartBuilder.addTextBody(PREVIOUS_RELEASE,qaTestConfig.getPreviousInternationalRelease());
		}
		String extensionDependencyRelease = qaTestConfig.getExtensionDependencyRelease();
		if (extensionDependencyRelease != null && !extensionDependencyRelease.isEmpty()) {
				multiPartBuilder.addTextBody(DEPENDENCY_RELEASE, extensionDependencyRelease);
			String previousExtensionRelease = qaTestConfig.getPreviousExtensionRelease();
			if (previousExtensionRelease != null && !previousExtensionRelease.isEmpty()) {
				multiPartBuilder.addTextBody(PREVIOUS_RELEASE, previousExtensionRelease);
			}
		}
		multiPartBuilder.addTextBody(RUN_ID, request.getRunId() );
		Integer failureExportMax = request.getFailureExportMax();
		if (failureExportMax != null && failureExportMax.intValue() != 0) {
			multiPartBuilder.addTextBody(FAILURE_EXPORT_MAX, String.valueOf(failureExportMax));
		}

		multiPartBuilder.addTextBody(STORAGE_LOCATION, qaTestConfig.getStorageLocation());

		if (StringUtils.isNotBlank(request.getEffectiveTime())) {
			multiPartBuilder.addTextBody(EFFECTIVE_TIME, request.getEffectiveTime());
		}

		if (StringUtils.isNotBlank(request.getIncludedModuleId())) {
			multiPartBuilder.addTextBody(INCLUDED_MODULES, request.getIncludedModuleId());
		}
		
		if (StringUtils.isNotBlank(qaTestConfig.getDroolsRulesGroupNames())) {
			multiPartBuilder.addTextBody(DROOLS_RULES_GROUPS, qaTestConfig.getDroolsRulesGroupNames());
		}

		if (StringUtils.isNotBlank(qaTestConfig.getProductName())) {
			multiPartBuilder.addTextBody(PRODUCT_NAME, qaTestConfig.getProductName());
		}

		if (StringUtils.isNotBlank(qaTestConfig.getReportingStage())) {
			multiPartBuilder.addTextBody(REPORTING_STAGE, qaTestConfig.getReportingStage());
		}
		post.setEntity(multiPartBuilder.build());
		return post;
	}

	public String validateOutputPackageFromS3(QATestConfig qaTestConfig, ValidationRequest validationRequest) throws FileNotFoundException {
		HttpPost post = createHttpPostRequest(qaTestConfig, validationRequest, RUN_POST_VIA_S3);
		LOGGER.info("Posting file {} to RVF at {} with run id {}.", validationRequest.getReleaseZipFileS3Path(), post.getURI(), validationRequest.getRunId());
		String rvfResponse = "No result recovered from RVF";
		try (CloseableHttpResponse response = httpClient.execute(post)) {
			LOGGER.debug(response.toString());
			final int statusCode = response.getStatusLine().getStatusCode();
			if (200 == statusCode || 201 == statusCode) {
				if (response.containsHeader(LOCATION)) {
					rvfResponse = response.getFirstHeader(LOCATION).getValue().toString();
					LOGGER.info("RVF result url:" + rvfResponse);
					return rvfResponse;
				}
			} 
			try (InputStream content = response.getEntity().getContent()) {
				rvfResponse = IOUtils.toString(content);
				rvfResponse = StringEscapeUtils.unescapeJava(rvfResponse);
				if (200 == statusCode) {
					// If all is good, expecting to find URL in the response
					int urlStart = rvfResponse.indexOf("http");
					if (urlStart != -1) {
						int urlEnd = rvfResponse.indexOf("\"", urlStart);
						rvfResponse = rvfResponse.substring(urlStart, urlEnd);
					}
					LOGGER.info("Asynchronous RVF post-condition check of {} initiated.  Clients should check for results at {}.",
							validationRequest.getReleaseZipFileS3Path(), rvfResponse);
				} else {
					rvfResponse = " Received RVF response HTTP status code: " + statusCode + " with body: " + rvfResponse;
					LOGGER.info("RVF Service failure: {}", rvfResponse);
				}
			}
		} catch (Exception e) {
			rvfResponse = "Exception detected while initiating RVF at: " + RUN_POST_VIA_S3 + 
					" to test: " + validationRequest.getReleaseZipFileS3Path() + " which said: " + e.getMessage();
			LOGGER.error(rvfResponse, e);
		}
		return rvfResponse;
	}
}

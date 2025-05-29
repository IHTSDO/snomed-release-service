package org.ihtsdo.buildcloud.core.service.validation.rvf;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.concurrent.ExecutionException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.ihtsdo.buildcloud.core.service.build.RF2Constants;
import org.ihtsdo.buildcloud.core.dao.io.AsyncPipedStreamBean;
import org.ihtsdo.buildcloud.core.entity.QATestConfig;
import org.ihtsdo.otf.rest.exception.ApplicationWiringException;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StreamUtils;

public class RVFClient implements Closeable {

	private static final String ENABLE_MRCM_VALIDATION = "enableMRCMValidation";

	private static final String ENABLE_TRACEABILITY_VALIDATION = "enableTraceabilityValidation";

	private static final String ENABLE_CHANGE_NOT_AT_TASK_LEVEL_VALIDATION = "enableChangeNotAtTaskLevelValidation";

	private static final String BRANCH_PATH = "branchPath";

	private static final String BRANCH_HEAD_TIMESTAMP = "contentHeadTimestamp";

	private static final String DROOLS_RULES_GROUPS = "droolsRulesGroups";

	private static final String EXCLUDED_RF2_FILES = "excludedRF2Files";

	private static final String INCLUDED_MODULES = "includedModules";

	private static final String DEFAULT_MODULE_ID = "defaultModuleId";

	private static final String RESPONSE_QUEUE = "responseQueue";

	private static final String EFFECTIVE_TIME = "effectiveTime";

	private static final String PREVIOUS_DEPENDENCY_EFFECTIVE_TIME = "previousDependencyEffectiveTime";

	private static final String EXCLUDED_REFSET_DESCRIPTOR_MEMBERS = "excludedRefsetDescriptorMembers";

	private static final String STORAGE_LOCATION = "storageLocation";

	private static final String FAILURE_EXPORT_MAX = "failureExportMax";

	private static final String RUN_ID = "runId";

	private static final String DEPENDENCY_RELEASE = "dependencyRelease";

	private static final String PREVIOUS_RELEASE = "previousRelease";

	private static final String RELEASE_AS_AN_EDITION = "releaseAsAnEdition";

	private static final String STAND_ALONE_PRODUCT = "standAloneProduct";

	private static final String GROUPS = "groups";

	private static final String ENABLE_DROOLS = "enableDrools";

	private static final String MANIFEST_FILE_S3_PATH = "manifestFileS3Path";

	private static final String RELEASE_FILE_S3_PATH = "releaseFileS3Path";

	private static final String BUCKET_NAME = "bucketName";

	private static final String RUN_POST_VIA_S3 = "/run-post-via-s3";

	private static final String LOCATION = "location";

	public static final String TOTAL_NUMBER_OF_FAILURES = "Total number of failures: ";

	public static final String FAILED = "Failed";

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

	public String checkInputFile(final InputStream inputFileStream, final String inputFileName, final AsyncPipedStreamBean logFileOutputStream, final String authToken) {
		return checkFile(inputFileStream, inputFileName, logFileOutputStream, true, authToken);
	}

	private String checkFile(final InputStream inputFileStream, final String inputFileName, final AsyncPipedStreamBean logFileOutputStream, final boolean preCheck, final String authToken) {
		String errorMessage;

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
		post.addHeader("Cookie", authToken);
		post.setEntity(MultipartEntityBuilder.create().addPart("file", new InputStreamBody(inputFileStream, inputFileName)).build());

		LOGGER.info("Posting file {} to RVF for {} check, using {}", inputFileName, checkType, targetUrl);
		
		String debugMsg = "Logged results to " + logFileOutputStream.getOutputFilePath();

		try (CloseableHttpResponse response = httpClient.execute(post)) {
			final int statusCode = response.getStatusLine().getStatusCode();
			RVFFailDetail failDetail;

			try (InputStream content = response.getEntity().getContent();
			     BufferedReader responseReader = new BufferedReader(new InputStreamReader(content, RF2Constants.UTF_8));
			     BufferedWriter logWriter = new BufferedWriter(new OutputStreamWriter(logFileOutputStream.getOutputStream(), RF2Constants.UTF_8))) {
				 failDetail = processResponse(responseReader, logWriter, debugMsg);
			} finally {
				logFileOutputStream.waitForFinish();
			}

			if (200 == statusCode) {
				if (failDetail.getFailedCount() == 0) {
					errorMessage = null;
				} else {
					errorMessage = "There were " + failDetail.getFailedCount() + " RVF " + checkType + " test failures for file "+ inputFileName + ": " + failDetail.getDetails();
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

	protected RVFFailDetail processResponse(final BufferedReader responseReader, final BufferedWriter logWriter, String debugMsg) throws IOException, RVFClientException {
		RVFFailDetail failDetail = new RVFFailDetail();
		long failureCount = 0;
		boolean foundFailureCount = false;
		boolean noLinesReceived = false;
		String failedDetails = null;

		String line = responseReader.readLine(); // read header
		if (line != null) {
			// write header
			logWriter.write(line);
			logWriter.write(RF2Constants.LINE_ENDING);

			// read all other lines
			boolean endOfValuesReached = false; // Optimisation so we don't inspect every line.
			while ((line = responseReader.readLine()) != null) {
				if (line.startsWith(FAILED)) {
					failedDetails = failedDetails == null ? line.replaceAll("\t", " ") : failedDetails + ", " + line.replaceAll("\t", " ");
				}
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
			failDetail.setFailedCount(failureCount);
			failDetail.setDetails(failedDetails);
			return failDetail;
		} else {
			throw new RVFClientException("Failure count not found in RVF response. " + (noLinesReceived?"No data received. ":"") + debugMsg);
		}
	}

	@Override
	public void close() throws IOException {
		httpClient.close();
	}
	private HttpPost createHttpPostRequest(QATestConfig qaTestConfig, ValidationRequest request, String targetUrl) {
		final HttpPost post = new HttpPost(releaseValidationFrameworkUrl + targetUrl);
		final MultipartEntityBuilder multiPartBuilder = MultipartEntityBuilder.create();
		multiPartBuilder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
		multiPartBuilder.addTextBody(BUCKET_NAME, request.getBuildBucketName());
		multiPartBuilder.addTextBody(RELEASE_FILE_S3_PATH, request.getReleaseZipFileS3Path());
		multiPartBuilder.addTextBody(MANIFEST_FILE_S3_PATH, request.getManifestFileS3Path());
		multiPartBuilder.addTextBody(ENABLE_DROOLS, Boolean.toString(qaTestConfig.isEnableDrools()));
		multiPartBuilder.addTextBody(GROUPS, qaTestConfig.getAssertionGroupNames());
		multiPartBuilder.addTextBody(RELEASE_AS_AN_EDITION, Boolean.toString(request.isReleaseAsAnEdition()));
		multiPartBuilder.addTextBody(STAND_ALONE_PRODUCT, Boolean.toString(request.isStandAloneProduct()));
		multiPartBuilder.addTextBody(ENABLE_MRCM_VALIDATION, Boolean.toString(qaTestConfig.isEnableMRCMValidation()));

		String previousPublishedPackage = request.getPreviousPublishedPackage();
		if (previousPublishedPackage != null && !previousPublishedPackage.isEmpty()) {
			multiPartBuilder.addTextBody(PREVIOUS_RELEASE, previousPublishedPackage);
		}
		String extensionDependencyRelease = request.getExtensionDependencyRelease();
		if (extensionDependencyRelease != null && !extensionDependencyRelease.isEmpty()) {
			multiPartBuilder.addTextBody(DEPENDENCY_RELEASE, extensionDependencyRelease);
		}
		if (qaTestConfig.isEnableTraceabilityValidation() && !StringUtils.isEmpty(request.getBranchPath())) {
			multiPartBuilder.addTextBody(ENABLE_TRACEABILITY_VALIDATION, Boolean.toString(qaTestConfig.isEnableTraceabilityValidation()));
			multiPartBuilder.addTextBody(ENABLE_CHANGE_NOT_AT_TASK_LEVEL_VALIDATION, Boolean.toString(request.isDailyBuild()));

			multiPartBuilder.addTextBody(BRANCH_PATH, request.getBranchPath());
		}

		if (qaTestConfig.getContentHeadTimestamp() != null) {
			multiPartBuilder.addTextBody(BRANCH_HEAD_TIMESTAMP, qaTestConfig.getContentHeadTimestamp().toString());
		}

		multiPartBuilder.addTextBody(RUN_ID, request.getRunId() );
		Integer failureExportMax = request.getFailureExportMax();
		if (failureExportMax != null && failureExportMax != 0) {
			multiPartBuilder.addTextBody(FAILURE_EXPORT_MAX, String.valueOf(failureExportMax));
		}

		multiPartBuilder.addTextBody(STORAGE_LOCATION, qaTestConfig.getStorageLocation());

		if (StringUtils.isNotBlank(request.getEffectiveTime())) {
			multiPartBuilder.addTextBody(EFFECTIVE_TIME, request.getEffectiveTime());
		}

		if (StringUtils.isNotBlank(request.getDefaultModuleId())) {
			multiPartBuilder.addTextBody(DEFAULT_MODULE_ID, request.getDefaultModuleId());
		}

		if (StringUtils.isNotBlank(request.getIncludedModuleIds())) {
			multiPartBuilder.addTextBody(INCLUDED_MODULES, request.getIncludedModuleIds());
		}

		final String responseQueue = request.getResponseQueue();
		if (StringUtils.isNoneBlank(responseQueue)) {
			multiPartBuilder.addTextBody(RESPONSE_QUEUE, responseQueue);
		}
		
		if (StringUtils.isNotBlank(qaTestConfig.getDroolsRulesGroupNames())) {
			multiPartBuilder.addTextBody(DROOLS_RULES_GROUPS, qaTestConfig.getDroolsRulesGroupNames());
		}

		if (request.getRemoveRF2Files() != null) {
			multiPartBuilder.addTextBody(EXCLUDED_RF2_FILES, request.getRemoveRF2Files().replace("|", ","));
		}

		if (StringUtils.isNotBlank(request.getPreviousExtensionDependencyEffectiveTime())) {
			multiPartBuilder.addTextBody(PREVIOUS_DEPENDENCY_EFFECTIVE_TIME, request.getPreviousExtensionDependencyEffectiveTime());
		}

		if (StringUtils.isNotBlank(request.getExcludedRefsetDescriptorMembers())) {
			multiPartBuilder.addTextBody(EXCLUDED_REFSET_DESCRIPTOR_MEMBERS, request.getExcludedRefsetDescriptorMembers());
		}

		post.setEntity(multiPartBuilder.build());
		return post;
	}

	public String validateOutputPackageFromS3(QATestConfig qaTestConfig, ValidationRequest validationRequest, final String authToken) throws BusinessServiceException {
		HttpPost post = createHttpPostRequest(qaTestConfig, validationRequest, RUN_POST_VIA_S3);
		post.addHeader("Cookie", authToken);

		LOGGER.info("Posting file {} to RVF at {} with run id {}.", validationRequest.getReleaseZipFileS3Path(), post.getURI(), validationRequest.getRunId());
		String rvfResponse;
		try (CloseableHttpResponse response = httpClient.execute(post)) {
			LOGGER.debug(response.toString());
			final int statusCode = response.getStatusLine().getStatusCode();
			if (200 == statusCode || 201 == statusCode) {
				if (response.containsHeader(LOCATION)) {
					rvfResponse = response.getFirstHeader(LOCATION).getValue();
					LOGGER.info("RVF result url:" + rvfResponse);
					return rvfResponse;
				}
			}

			try (InputStream content = response.getEntity().getContent()) {
				rvfResponse = IOUtils.toString(content,"UTF-8" );
				rvfResponse = StringEscapeUtils.unescapeJava(rvfResponse);
				rvfResponse = " Received RVF response HTTP status code: " + statusCode + " with body: " + rvfResponse;
				LOGGER.error("RVF Service failure: {}", rvfResponse);
				throw new BusinessServiceException(rvfResponse);
			}
		} catch (IOException e) {
			rvfResponse = "Exception detected while initiating RVF at: " + RUN_POST_VIA_S3 +
					" to test: " + validationRequest.getReleaseZipFileS3Path() + " which said: " + e.getMessage();
			LOGGER.error(rvfResponse, e);
			throw new BusinessServiceException(rvfResponse);
		}
	}
}

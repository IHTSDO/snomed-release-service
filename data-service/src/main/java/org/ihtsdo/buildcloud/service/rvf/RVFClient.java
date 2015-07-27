package org.ihtsdo.buildcloud.service.rvf;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringEscapeUtils;
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
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StreamUtils;

public class RVFClient implements Closeable {

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
				if (line.isEmpty()) {
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

	public String checkOutputPackage(final File zipPackage, final QATestConfig qaTestConfig) throws FileNotFoundException {

		final String runId = Long.toString(System.currentTimeMillis());
		final String zipFileName = zipPackage.getName();
		final String targetUrl = "/run-post";
		
		final HttpPost post = createHttpPostRequest(zipPackage, qaTestConfig, runId, zipFileName, targetUrl);
		LOGGER.info("Posting input file {} to RVF at {} with run id {}.", zipFileName, post.getURI(), runId);
		String rvfResponse = "No result recovered from RVF";
		try (CloseableHttpResponse response = httpClient.execute(post)) {
			final int statusCode = response.getStatusLine().getStatusCode();
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
							zipFileName, rvfResponse);
				} else {
					rvfResponse = " Received RVF response HTTP status code: " + statusCode + 
							" with body: " + rvfResponse;
					LOGGER.info("RVF Service failure: {}", rvfResponse);
				}
			}
		} catch (Exception e) {
			rvfResponse = "Exception detected while initiating RVF at: " + targetUrl + 
					" to test: " + zipFileName + " which said: " + e.getMessage();
			LOGGER.error (rvfResponse, e);
		}
		return rvfResponse;
	}

	private HttpPost createHttpPostRequest(final File zipPackage,
			final QATestConfig qaTestConfig, final String runId,
			final String zipFileName, final String targetUrl)
			throws FileNotFoundException {
		final HttpPost post = new HttpPost(releaseValidationFrameworkUrl + targetUrl);
		final MultipartEntityBuilder multiPartBuilder = MultipartEntityBuilder.create();
		multiPartBuilder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
		multiPartBuilder.addPart("file", new InputStreamBody(new FileInputStream(zipPackage), zipFileName));

		// Currently getting knocked back with HTTP400 so making this call more like the RVF Menu client which is working fine.
		/*
		 * if(qaTestConfig.getAssertionGroupNames() != null) { for(final String groupName :
		 * qaTestConfig.getAssertionGroupNames().split(",")) { multiPartBuilder.addTextBody("groups", groupName); } }
		 */
		multiPartBuilder.addTextBody("groups", qaTestConfig.getAssertionGroupNames());
		final String previousIntRelease = qaTestConfig.getPreviousInternationalRelease();
		if ( previousIntRelease != null ) {
			multiPartBuilder.addTextBody("previousIntReleaseVersion",qaTestConfig.getPreviousInternationalRelease());
		}
		final String previousExtensionRelease = qaTestConfig.getPreviousExtensionRelease();
		if (previousExtensionRelease != null) {
			multiPartBuilder.addTextBody("previousExtensionReleaseVersion", previousExtensionRelease);
		}

		final String extensionDependencyRelease = qaTestConfig.getExtensionDependencyRelease();
		if (extensionDependencyRelease != null) 
		{
			multiPartBuilder.addTextBody("extensionDependencyReleaseVersion", extensionDependencyRelease);
		}

		multiPartBuilder.addTextBody("runId",runId);

		multiPartBuilder.addTextBody("storageLocation", qaTestConfig.getStorageLocation());

		post.setEntity(multiPartBuilder.build());
		return post;
	}

	private String parseRvfJsonResponse( final File tmpJson) {
		long failureCount = -1L;
		final Map<String,Object> msg = new HashMap<>();
		try (final Reader tmpJsonReader =  new InputStreamReader(new FileInputStream(tmpJson))) {
			final JSONParser jsonParser = new JSONParser();
			final JSONObject jsonObject = (JSONObject) jsonParser.parse(tmpJsonReader);
			if (jsonObject.containsKey("type")) {
				msg.put("type",jsonObject.get("type"));
			}
			if (jsonObject.containsKey("reportUrl")) {
				msg.put("reportUrl", jsonObject.get("reportUrl"));
			}
			if( jsonObject.containsKey("assertionsFailed")) {
				final Long assertionsFailed = (Long)jsonObject.get("assertionsFailed");
				msg.put("assertionsFailed", assertionsFailed);
				failureCount = assertionsFailed != null ? assertionsFailed.longValue() : failureCount ;
			}
			if (jsonObject.containsKey("assertionsRun")) {
				final Long assertionsRun =  (Long) jsonObject.get("assertionsRun");
				msg.put("assertionsRun", assertionsRun);
			}
			if (jsonObject.containsKey("failureMessage")) {
				msg.put("failureMessage", jsonObject.get("failureMessage"));
			}
		} catch (final ParseException | IOException e) {
			LOGGER.error("Failed to parse response in JSON." + e.fillInStackTrace());
		}
		if (failureCount != 0) {
			return JSONObject.toJSONString(msg);
		}
		return null;
	}
}

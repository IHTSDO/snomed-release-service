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

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.ihtsdo.buildcloud.dao.io.AsyncPipedStreamBean;
import org.ihtsdo.buildcloud.entity.QATestConfig;
import org.ihtsdo.buildcloud.service.build.RF2Constants;
import org.ihtsdo.buildcloud.service.exception.ApplicationWiringException;
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

	public String checkOutputPackage(final File zipPackage, final AsyncPipedStreamBean logFileOutputStream) throws IOException {
		return checkFile(new FileInputStream(zipPackage), zipPackage.getName(), logFileOutputStream, false);
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

		final HttpPost post = new HttpPost(releaseValidationFrameworkUrl + targetUrl);
		post.setEntity(MultipartEntityBuilder.create().addPart("file", new InputStreamBody(inputFileStream, inputFileName)).build());

		LOGGER.info("Posting input file {} to RVF for {} check.", inputFileName, checkType);
		LOGGER.debug("Using {}.", targetUrl);

		try (CloseableHttpResponse response = httpClient.execute(post)) {
			final int statusCode = response.getStatusLine().getStatusCode();
			long failureCount = 0;

			try (InputStream content = response.getEntity().getContent();
				 BufferedReader responseReader = new BufferedReader(new InputStreamReader(content, RF2Constants.UTF_8));
				 BufferedWriter logWriter = new BufferedWriter(new OutputStreamWriter(logFileOutputStream.getOutputStream(), RF2Constants.UTF_8))) {

				failureCount = processResponse(responseReader, logWriter);
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
				LOGGER.error("Failed to write exception to log.", e);
			}
		} finally {
			LOGGER.info("RVF {} check of {} complete.", checkType, inputFileName);
		}

		return errorMessage;
	}

	protected long processResponse(final BufferedReader responseReader, final BufferedWriter logWriter) throws IOException, RVFClientException {
		long failureCount = 0;
		boolean foundFailureCount = false;

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
		}

		if (foundFailureCount) {
			return failureCount;
		} else {
			throw new RVFClientException("Failure count not found in RVF response.");
		}
	}

	@Override
	public void close() throws IOException {
		httpClient.close();
	}

	public String checkOutputPackage(final File zipPackage, final AsyncPipedStreamBean logFileOutputStream, final QATestConfig qaTestConfig) throws FileNotFoundException {
		final StringBuilder msgBuilder = new StringBuilder();
		final String fileType = "output";
		final String checkType = "postcondition";
		final String zipFileName = zipPackage.getName();
		final String targetUrl = "/run-post";
		final HttpPost post = new HttpPost(releaseValidationFrameworkUrl + targetUrl);
		final MultipartEntityBuilder multiPartBuilder = MultipartEntityBuilder.create();
		multiPartBuilder.addPart("file", new InputStreamBody(new FileInputStream(zipPackage), zipFileName));
//		multiPartBuilder.addTextBody("manifest");
		if(qaTestConfig.getAssertionGroupNames() != null) {
			for(final String groupName : qaTestConfig.getAssertionGroupNames().split(",")) {
				multiPartBuilder.addTextBody("groups", groupName);
			}
		}
		multiPartBuilder.addTextBody("prospectiveReleaseVersion",zipFileName);
		multiPartBuilder.addTextBody("previousReleaseVersions",qaTestConfig.getPreviousReleaseVersions());
		final String runId = Long.toString(System.currentTimeMillis());
		multiPartBuilder.addTextBody("runId",runId);
		post.setEntity(multiPartBuilder.build());
		LOGGER.info("Posting input file {} to RVF for {} check with run id {}.", zipFileName, checkType, runId);
		LOGGER.debug("Using {}.", targetUrl);
		final File tmpJson = new File(FileUtils.getTempDirectory(),"tmpResp_" + runId + ".json");
		try (CloseableHttpResponse response = httpClient.execute(post)) {
			final int statusCode = response.getStatusLine().getStatusCode();
			if (200 != statusCode) {
				msgBuilder.append(" Received RVF response HTTP status code " + statusCode);
				LOGGER.info("RVF Service failure: {}", msgBuilder.toString());
				return msgBuilder.toString();
			} 
			try (InputStream content = response.getEntity().getContent()) {
				//write response locally
				FileUtils.copyInputStreamToFile(content, tmpJson);
				LOGGER.info("Response JSON is wrttien to temp file:" + tmpJson.getAbsolutePath());
			}
			//parse json before upload to S3
			final String responseMsg = parseRvfJsonResponse(tmpJson);
			if (responseMsg != null ) {
				msgBuilder.append(responseMsg);
				LOGGER.info(responseMsg);
			}
			
		} catch (final IOException e) {
			msgBuilder.append( "Failed to check " + fileType + " file against RVF: " + zipFileName + " due to " + e.getMessage());
			LOGGER.error(msgBuilder.toString(), e);
		}
		//load JSON file to s3
		try(InputStream tempJsonInput = new FileInputStream(tmpJson);
			OutputStream output = logFileOutputStream.getOutputStream()) {
			IOUtils.copy(tempJsonInput, output);
		} catch (final IOException e) {
			LOGGER.error("Failed to load response JSON to S3." + e.fillInStackTrace());
		} finally {
			try {
				logFileOutputStream.waitForFinish();
			} catch (final Exception e) {
				msgBuilder.append("Response JSON has not been loaded to S3 successfully due to:" + e.getMessage());
				LOGGER.error(msgBuilder.toString(), e);
			}
			FileUtils.deleteQuietly(tmpJson);
			LOGGER.info("RVF {} check of {} complete.", checkType, zipFileName);
		}
		return msgBuilder.toString();
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

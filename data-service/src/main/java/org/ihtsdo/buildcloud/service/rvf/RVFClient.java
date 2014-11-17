package org.ihtsdo.buildcloud.service.rvf;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.ihtsdo.buildcloud.dao.io.AsyncPipedStreamBean;
import org.ihtsdo.buildcloud.service.execution.RF2Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StreamUtils;

import java.io.*;
import java.util.concurrent.ExecutionException;

public class RVFClient implements Closeable {

	public static final String TOTAL_NUMBER_OF_FAILURES = "Total number of failures: ";
	private final String releaseValidationFrameworkUrl;
	private final CloseableHttpClient httpClient;
	private static final String ERROR_NO_LINES_RECEIVED_FROM_RVF = "Error - No lines received from RVF!";
	private static final String SUCCESS = "Success";

	private static final Logger LOGGER = LoggerFactory.getLogger(RVFClient.class);

	public RVFClient(String releaseValidationFrameworkUrl) {
		if (releaseValidationFrameworkUrl == null) {
			throw new RuntimeException("Null RVF host URL.");
		}
		this.releaseValidationFrameworkUrl = releaseValidationFrameworkUrl;
		httpClient = HttpClients.createDefault();
	}

	public String checkInputFile(InputStream inputFileStream, String inputFileName, AsyncPipedStreamBean logFileOutputStream) {
		return checkFile(inputFileStream, inputFileName, logFileOutputStream, true);
	}

	public String checkOutputPackage(File zipPackage, AsyncPipedStreamBean logFileOutputStream) throws IOException {
		return checkFile(new FileInputStream(zipPackage), zipPackage.getName(), logFileOutputStream, false);
	}

	private String checkFile(InputStream inputFileStream, String inputFileName, AsyncPipedStreamBean logFileOutputStream, boolean preCheck) {
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

		HttpPost post = new HttpPost(releaseValidationFrameworkUrl + targetUrl);
		post.setEntity(MultipartEntityBuilder.create().addPart("file", new InputStreamBody(inputFileStream, inputFileName)).build());

		LOGGER.info("Posting input file {} to RVF for {} check.", inputFileName, checkType);
		LOGGER.debug("Using {}.", targetUrl);

		try (CloseableHttpResponse response = httpClient.execute(post)) {
			int statusCode = response.getStatusLine().getStatusCode();
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
			} catch (Exception e2) {
				LOGGER.error("Failed to write exception to log.", e);
			}
		} finally {
			LOGGER.info("RVF {} check of {} complete.", checkType, inputFileName);
		}

		return errorMessage;
	}

	protected long processResponse(BufferedReader responseReader, BufferedWriter logWriter) throws IOException, RVFClientException {
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
}

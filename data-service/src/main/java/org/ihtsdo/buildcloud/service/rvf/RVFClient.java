package org.ihtsdo.buildcloud.service.rvf;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.ihtsdo.buildcloud.dao.io.AsyncPipedStreamBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StreamUtils;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutionException;

public class RVFClient implements Closeable {

	private final String releaseValidationFrameworkUrl;
	private final CloseableHttpClient httpClient;

	private static final int SENSIBLE_AMOUNT_OF_DATA = 50;
	private static final Logger LOGGER = LoggerFactory.getLogger(RVFClient.class);

	public RVFClient(String releaseValidationFrameworkUrl) {
		this.releaseValidationFrameworkUrl = releaseValidationFrameworkUrl;
		httpClient = HttpClients.createDefault();
	}

	public String checkInputFile(InputStream inputFileStream, String inputFileName, AsyncPipedStreamBean logFileOutputStream) {
		HttpPost post = new HttpPost(releaseValidationFrameworkUrl + "/test-file");
		post.setEntity(MultipartEntityBuilder.create().addPart("file", new InputStreamBody(inputFileStream, inputFileName)).build());

		LOGGER.info("Posting input file {} to RVF for precondition check.", inputFileName);

		try (CloseableHttpResponse response = httpClient.execute(post)) {
			int statusCode = response.getStatusLine().getStatusCode();

			try (InputStream content = response.getEntity().getContent();
				 OutputStream logOutputStream = logFileOutputStream.getOutputStream()) {
				// Did we get a sensible amount of data back from the RVF?
				if (content.available() < SENSIBLE_AMOUNT_OF_DATA) {
					StreamUtils.copy("Suspiciously small content warning.\n".getBytes(), logOutputStream);
				}
				StreamUtils.copy(content, logOutputStream);
			}
			logFileOutputStream.waitForFinish();

			if (200 == statusCode) {
				return null;
			} else {
				LOGGER.info("RVF Service failure: HTTP {}", statusCode);
				return "" + statusCode;
			}
		} catch (InterruptedException | ExecutionException | IOException e) {
			String errorMessage = "Failed to check input file against RVF: " + inputFileName + " due to " + e.getMessage();
			LOGGER.error(errorMessage, e);
			try (OutputStream logOutputStream = logFileOutputStream.getOutputStream()) {
				StreamUtils.copy(errorMessage.getBytes(), logOutputStream);
				logFileOutputStream.waitForFinish();
			} catch (Exception e2) {
				LOGGER.error("Failed to write exception to log.", e);
			}
			return "RVF Client error. See logs for details.";
		} finally {
			LOGGER.info("RVF precondition check of {} complete.", inputFileName);
		}
	}

	@Override
	public void close() throws IOException {
		httpClient.close();
	}

}

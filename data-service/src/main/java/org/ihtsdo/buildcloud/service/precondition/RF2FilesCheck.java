package org.ihtsdo.buildcloud.service.precondition;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.ihtsdo.buildcloud.dao.ExecutionDAO;
import org.ihtsdo.buildcloud.dao.io.AsyncPipedStreamBean;
import org.ihtsdo.buildcloud.entity.Execution;
import org.ihtsdo.buildcloud.entity.Package;
import org.ihtsdo.buildcloud.service.execution.RF2Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class RF2FilesCheck extends PreconditionCheck {

	@Autowired
	private ExecutionDAO executionDAO;

	@Autowired
	private String releaseValidationFrameworkUrl;

	private static final Logger LOGGER = LoggerFactory.getLogger(RF2FilesCheck.class);

	public void runCheck(Package pkg, Execution execution) {

		String pkgBusinessKey = pkg.getBusinessKey();
		List<String> inputFiles = executionDAO.listInputFileNames(execution, pkgBusinessKey);

		try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
			for (String inputFile : inputFiles) {

				if (inputFile.endsWith(RF2Constants.TXT_FILE_EXTENSION)) {

					InputStream inputFileStream = executionDAO.getInputFileStream(execution, pkgBusinessKey, inputFile);

					HttpPost post = new HttpPost(releaseValidationFrameworkUrl + "/test-file");
					post.setEntity(MultipartEntityBuilder.create().addPart("file", new InputStreamBody(inputFileStream, inputFile)).build());

					LOGGER.info("Posting input file {} to RVF for precondition check.", inputFile);

					try (CloseableHttpResponse response = httpClient.execute(post)) {
						int statusCode = response.getStatusLine().getStatusCode();

						AsyncPipedStreamBean logFileOutputStream = executionDAO.getLogFileOutputStream(execution, pkgBusinessKey, "precheck-rvf-" + inputFile + ".log");
						try (InputStream content = response.getEntity().getContent();
							OutputStream logOutputStream = logFileOutputStream.getOutputStream()) {
							StreamUtils.copy(content, logOutputStream);
						}
						logFileOutputStream.waitForFinish();

						if (200 == statusCode) {
							pass();
						} else {
							fail("Invalid RF2 file.");
						}
					} catch (InterruptedException | ExecutionException | IOException e) {
						LOGGER.error("Failed to check input file {} against RVF.", inputFile, e);
					}
					LOGGER.info("RVF precondition check of {} complete.", inputFile);
				}
			}
		} catch (IOException e) {
			LOGGER.error("Failed to check any input files against RVF.", e);
		}
	}

}

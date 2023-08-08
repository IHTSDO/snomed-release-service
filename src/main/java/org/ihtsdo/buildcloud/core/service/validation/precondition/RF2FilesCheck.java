package org.ihtsdo.buildcloud.core.service.validation.precondition;

import java.io.IOException;
import java.io.InputStream;

import com.google.common.base.Strings;
import org.ihtsdo.buildcloud.core.dao.BuildDAO;
import org.ihtsdo.buildcloud.core.dao.io.AsyncPipedStreamBean;
import org.ihtsdo.buildcloud.core.entity.Build;
import org.ihtsdo.buildcloud.core.service.NetworkRequired;
import org.ihtsdo.buildcloud.core.service.build.RF2Constants;
import org.ihtsdo.buildcloud.core.service.validation.rvf.RVFClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class RF2FilesCheck extends PreconditionCheck implements NetworkRequired {

	@Autowired
	private BuildDAO buildDAO;

	@Value("${rvf.url}")
	private String rvfUrl;

	private static final Logger LOGGER = LoggerFactory.getLogger(RF2FilesCheck.class);

	@Override
	public void runCheck(final Build build) {
		if (Strings.isNullOrEmpty(rvfUrl)) {
			LOGGER.warn("No rvf url is specified and RF2FilesCheck will be skipped");
			return;
		}
		try (RVFClient rvfClient = new RVFClient(rvfUrl)) {
			StringBuilder errorMessage = new StringBuilder();
			for (String inputFile : buildDAO.listInputFileNames(build)) {
				if (inputFile.startsWith(RF2Constants.INPUT_FILE_PREFIX) && inputFile.endsWith(RF2Constants.TXT_FILE_EXTENSION)) {
					InputStream inputFileStream = buildDAO.getInputFileStream(build, inputFile);
					AsyncPipedStreamBean logFileOutputStream = buildDAO.getLogFileOutputStream(build, "precheck-rvf-" + inputFile + ".log");
					String error = rvfClient.checkInputFile(inputFileStream, inputFile, logFileOutputStream);
					if (error != null) {
						errorMessage.append(error).append(".");
					}
				}
			}
			if (StringUtils.hasLength(errorMessage.toString())) {
				fail(errorMessage.toString());
			} else {
				pass();
			}
		} catch (IOException e) {
			LOGGER.error("Failed to check any input files against RVF.", e);
		}
	}

}

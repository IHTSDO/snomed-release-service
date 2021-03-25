package org.ihtsdo.buildcloud.service.precondition;

import java.io.IOException;
import java.io.InputStream;

import org.ihtsdo.buildcloud.dao.BuildDAO;
import org.ihtsdo.buildcloud.dao.io.AsyncPipedStreamBean;
import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.service.NetworkRequired;
import org.ihtsdo.buildcloud.service.build.RF2Constants;
import org.ihtsdo.buildcloud.service.rvf.RVFClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RF2FilesCheck extends PreconditionCheck implements NetworkRequired {

	@Autowired
	private BuildDAO buildDAO;

	@Value("${releaseValidationFramework.url}")
	private String releaseValidationFrameworkUrl;

	private static final Logger LOGGER = LoggerFactory.getLogger(RF2FilesCheck.class);

	@Override
	public void runCheck(final Build build) {
		try (RVFClient rvfClient = new RVFClient(releaseValidationFrameworkUrl)) {
			for (String inputFile : buildDAO.listInputFileNames(build)) {
				if (inputFile.startsWith(RF2Constants.INPUT_FILE_PREFIX) && inputFile.endsWith(RF2Constants.TXT_FILE_EXTENSION)) {
				    	LOGGER.info("Run pre-condition RF2FilesCheck for input file:{}", inputFile);
					InputStream inputFileStream = buildDAO.getInputFileStream(build, inputFile);
					AsyncPipedStreamBean logFileOutputStream = buildDAO.getLogFileOutputStream(build, "precheck-rvf-" + inputFile + ".log");
					String errorMessage = rvfClient.checkInputFile(inputFileStream, inputFile, logFileOutputStream);
					if (errorMessage == null) {
						pass();
					} else {
						fail(errorMessage);
					}
				}
			}
		} catch (IOException e) {
			LOGGER.error("Failed to check any input files against RVF.", e);
		}
	}

}

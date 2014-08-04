package org.ihtsdo.buildcloud.service.precondition;

import org.ihtsdo.buildcloud.dao.ExecutionDAO;
import org.ihtsdo.buildcloud.dao.io.AsyncPipedStreamBean;
import org.ihtsdo.buildcloud.entity.Execution;
import org.ihtsdo.buildcloud.entity.Package;
import org.ihtsdo.buildcloud.service.NetworkRequired;
import org.ihtsdo.buildcloud.service.execution.RF2Constants;
import org.ihtsdo.buildcloud.service.rvf.RVFClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class RF2FilesCheck extends PreconditionCheck implements NetworkRequired {

	@Autowired
	private ExecutionDAO executionDAO;

	@Autowired
	private String releaseValidationFrameworkUrl;

	private static final Logger LOGGER = LoggerFactory.getLogger(RF2FilesCheck.class);

	public void runCheck(Package pkg, Execution execution) {
		String pkgBusinessKey = pkg.getBusinessKey();
		List<String> inputFiles = executionDAO.listInputFileNames(execution, pkgBusinessKey);

		try (RVFClient rvfClient = new RVFClient(releaseValidationFrameworkUrl)) {
			for (String inputFile : inputFiles) {
				if (inputFile.endsWith(RF2Constants.TXT_FILE_EXTENSION)) {
					InputStream inputFileStream = executionDAO.getInputFileStream(execution, pkgBusinessKey, inputFile);
					AsyncPipedStreamBean logFileOutputStream = executionDAO.getLogFileOutputStream(execution, pkgBusinessKey, "precheck-rvf-" + inputFile + ".log");
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

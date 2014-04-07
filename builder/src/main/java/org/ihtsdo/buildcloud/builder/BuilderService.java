package org.ihtsdo.buildcloud.builder;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class BuilderService {

	@Autowired
	private ReleaseApiClient apiClient;

	@Autowired
	private MavenExecutor mavenExecutor;

	private static final Logger LOGGER = LoggerFactory.getLogger(BuilderService.class);

	public void buildExecution(String executionUrl) throws IOException, InterruptedException, ZipException {
		apiClient.setExecutionStatus("BUILDING", executionUrl);

		// Download build scripts as temp file
		File buildScriptsZip = apiClient.downloadBuildScriptsZip(executionUrl);

		// Create build directory
		File buildDirectory = Files.createTempDirectory("build-directory").toFile();
		LOGGER.info("Build directory:{}", buildDirectory.getAbsolutePath());

		// Extract build scripts
		new ZipFile(buildScriptsZip).extractAll(buildDirectory.getAbsolutePath());

		// Run maven deploy goal
		mavenExecutor.runMavenProcess(buildDirectory);

		// Upload output
		apiClient.uploadTargetDirectories(buildDirectory, executionUrl + "output");
		apiClient.setExecutionStatus("BUILT", executionUrl);
	}

}

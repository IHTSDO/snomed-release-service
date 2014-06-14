package org.ihtsdo.buildcloud.builder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class MavenExecutor {

	private static final Logger LOGGER = LoggerFactory.getLogger(MavenExecutor.class);

	public int runMavenProcess(File buildDirectory) throws IOException, InterruptedException {
		String command = "mvn test";

		// Create target directory for log
		new File(buildDirectory, "target").mkdirs();
		File mavenLog = new File(buildDirectory, "target" + File.separator + "maven.log");

		ProcessBuilder processBuilder = new ProcessBuilder(command.split(" "));
		processBuilder.directory(buildDirectory);
		processBuilder.redirectOutput(mavenLog);
		processBuilder.redirectError(mavenLog);
		LOGGER.info("Running command:{}", command);
		Process process = processBuilder.start();
		int exitValue = process.waitFor();
		LOGGER.info("Command complete. Exit value:{}", exitValue);
		return exitValue;
	}

}

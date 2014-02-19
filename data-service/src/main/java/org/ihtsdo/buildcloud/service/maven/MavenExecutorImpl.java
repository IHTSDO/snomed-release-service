package org.ihtsdo.buildcloud.service.maven;

import org.ihtsdo.buildcloud.entity.Build;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;

public class MavenExecutorImpl implements MavenExecutor {

	private static final Logger LOGGER = LoggerFactory.getLogger(MavenExecutorImpl.class);

	@Override
	public String exec(Build build, File buildFilesDirectory, Date triggerDate) throws IOException {

		addSettingsFile(buildFilesDirectory.toPath());

		// Run maven
		String output = exec("mvn clean deploy -s settings.xml", buildFilesDirectory);
		return output;
	}

	private String exec(String command, File workingDir) throws IOException {
		LOGGER.info("Exec: '{}', in {}", command, workingDir);
		Runtime runtime = Runtime.getRuntime();
		Process process = runtime.exec(command, null, workingDir);

		String out = FileCopyUtils.copyToString(new InputStreamReader(process.getInputStream()));
		if (out != null) {
			LOGGER.info("Exec out: '{}'", out);
		}

		String error = FileCopyUtils.copyToString(new InputStreamReader(process.getErrorStream()));
		if (error != null && !error.isEmpty()) {
			LOGGER.error("Exec error: '{}'", error);
		}

		int exitValue = process.exitValue();
		if (exitValue != 0) {
			LOGGER.error("Exec bad exit value: {}", exitValue);
		}

		return out;
	}

	// All hardcoded until we have pom generation done.
	private void addSettingsFile(Path workingDir) throws IOException {
		Files.copy(new ClassPathResource("/settings.xml").getInputStream(), workingDir.resolve("settings.xml"));
	}

}

package org.ihtsdo.buildcloud.service.maven;

import org.ihtsdo.buildcloud.entity.Build;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MavenExecutorImpl implements MavenExecutor {

	private static final Logger LOGGER = LoggerFactory.getLogger(MavenExecutorImpl.class);

	private static final String BUILD_TRIGGER_DATE_FORMAT = "yyyy_MM_dd_HHmmss";

	@Override
	public String exec(Build build, ClassPathResource buildFilesDirectory, Date triggerDate) throws IOException {
		String buildBusinessKey = build.getBusinessKey();
		String triggerDateString = new SimpleDateFormat(BUILD_TRIGGER_DATE_FORMAT).format(triggerDate);
		Path workingDir = Files.createTempDirectory(triggerDateString + "-" + buildBusinessKey);

		downloadBuildFiles(buildBusinessKey, triggerDateString, workingDir);

		// Run maven
		String output = exec("mvn clean package", workingDir);
		return output;
	}

	private String exec(String command, Path workingDir) throws IOException {
		LOGGER.info("Exec: '{}', in {}", command, workingDir);
		Runtime runtime = Runtime.getRuntime();
		Process process = runtime.exec(command, null, workingDir.toFile());

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
	private void downloadBuildFiles(String buildBusinessKey, String triggerDateString, Path workingDir) throws IOException {
		workingDir.resolve("packageA").toFile().mkdir();
		ClassPathResource classPathResource = new ClassPathResource("/example-build/pom.xml");
		Files.copy(classPathResource.getInputStream(), workingDir.resolve("pom.xml"));
		Files.copy(new ClassPathResource("/example-build/packageA/pom.xml").getInputStream(), workingDir.resolve("packageA/pom.xml"));
		Files.copy(new ClassPathResource("/example-build/packageA/assembly.xml").getInputStream(), workingDir.resolve("packageA/assembly.xml"));
	}

}

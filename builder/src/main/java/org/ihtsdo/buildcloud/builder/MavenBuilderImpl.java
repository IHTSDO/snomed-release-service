package org.ihtsdo.buildcloud.builder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class MavenBuilderImpl implements MavenBuilder {

	private static final Logger LOGGER = LoggerFactory.getLogger(MavenBuilderImpl.class);

	@Override
	public void exec(File buildFilesDirectory) throws IOException {
		addSettingsFile(buildFilesDirectory.toPath());

		// Run maven
		exec("mvn clean deploy -s settings.xml", buildFilesDirectory);
	}

	private void exec(String command, File workingDir) throws IOException {
		LOGGER.info("Exec: '{}', in {}", command, workingDir);
		Runtime runtime = Runtime.getRuntime();
		Process process = runtime.exec(command, null, workingDir);
		// TODO: Forward streaming output to S3.
	}

//	All hardcoded until we have pom generation done.
	private void addSettingsFile(Path workingDir) throws IOException {
		Files.copy(new FileInputStream("classpath:/settings.xml"), workingDir.resolve("settings.xml"));
	}

}

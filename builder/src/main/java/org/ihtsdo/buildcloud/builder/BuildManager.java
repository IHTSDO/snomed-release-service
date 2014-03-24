package org.ihtsdo.buildcloud.builder;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IQueue;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.nio.file.Files;

public class BuildManager {

	private final IQueue<String> queue;
	private final ReleaseApiClient apiClient;
	private final MavenExecutor mavenExecutor;

	private static final String BUILD_QUEUE_NAME = "org.ihtsdo.buildcloud.build.queue";
	private static final Logger LOGGER = LoggerFactory.getLogger(BuildManager.class);

	public static void main(String[] args) {
		BuildManager buildManager = new BuildManager();
		try {
			buildManager.run();
		} catch (InterruptedException e) {
			LOGGER.error("Interrupted while waiting for queue or exec process.", e);
		} catch (Exception e) {
			LOGGER.error(e.getLocalizedMessage(), e);
		}
	}

	private BuildManager() {
		HazelcastInstance hazelcastInstance = Hazelcast.newHazelcastInstance();
		queue = hazelcastInstance.getQueue(BUILD_QUEUE_NAME);

		apiClient = new ReleaseApiClient();
		mavenExecutor = new MavenExecutor();

		Authenticator.setDefault(new Authenticator() {
			@Override
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication("test", new char[]{});
			}
		});
	}

	private void run() throws InterruptedException, IOException, ZipException {
		while (true) {
			String executionUrl = queue.take();
			LOGGER.info("Consuming executionUrl:{}", executionUrl);

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
		}
	}

}

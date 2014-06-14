package org.ihtsdo.buildcloud.builder;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public class ReleaseApiClient {

	private static final String ZIP = ".zip";
	private static final Logger LOGGER = LoggerFactory.getLogger(ReleaseApiClient.class);

	public ReleaseApiClient() {
		Authenticator.setDefault(new Authenticator() {
			@Override
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication("test", new char[]{});
			}
		});
	}

	public File downloadBuildScriptsZip(String executionUrl) throws IOException {
		String buildScriptsUrl = executionUrl + "build-scripts.zip";
		LOGGER.info("Downloading {}", buildScriptsUrl);
		File buildScriptsZip = File.createTempFile(getClass().getName(), ZIP);
		FileUtils.copyURLToFile(new URL(buildScriptsUrl), buildScriptsZip);
		LOGGER.info("Download complete {}", buildScriptsZip.getAbsolutePath());
		return buildScriptsZip;
	}

	public void uploadTargetDirectories(File buildDirectory, final String executionOutputUrl) throws IOException {
		LOGGER.info("Uploading all output files");
		final int buildDirectoryPathLength = buildDirectory.getAbsolutePath().length();
		Files.walkFileTree(buildDirectory.toPath(), new SimpleFileVisitor<Path>() {

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
				String path = getRelativePath(file.toFile(), buildDirectoryPathLength);
				if (path.contains( File.separator + "target" + File.separator)) {
					LOGGER.info("Uploading output file :{}", path);
					robustUpload(file.toFile());
				}
				return FileVisitResult.CONTINUE;
			}

			private void robustUpload(File file) {
				int attempt = 0;
				boolean success = false;
				boolean giveUp = false;
				while (!success && !giveUp) {
					attempt++;
					try {
						uploadFileAttempt(file);
						success = true;
					} catch (IOException e) {
						giveUp = attempt == 3;
						if (!giveUp) {
							LOGGER.warn("Upload failed, will retry, file:{}", file.getPath(), e);
						} else {
							LOGGER.error("Upload failed, giving up, file:{}", file.getPath(), e);
						}
					}
				}
			}

			private void uploadFileAttempt(File file) throws IOException {
				String path = getRelativePath(file, buildDirectoryPathLength);
				HttpURLConnection connection = openConnection(executionOutputUrl + path, "POST");
				connection.setRequestProperty("Content-Length", new Long(file.length()).toString());
				connection.setDoOutput(true);
				OutputStream outputStream = connection.getOutputStream();
				try {
					LOGGER.debug("POST to {}", connection.getURL());
					FileUtils.copyFile(file, outputStream);
				} finally {
					outputStream.close();
				}
				int responseCode = connection.getResponseCode();
				if (responseCode != 201) {
					throw new IOException("HTTP Response Code:" + responseCode);
				}
			}

			private String getRelativePath(File file, int rootPathLength) {
				return file.getPath().substring(rootPathLength);
			}

		});
	}

	public void setExecutionStatus(String status, String executionUrl) throws IOException {
		LOGGER.info("Setting status '{}' of execution {}", status, executionUrl);
		HttpURLConnection connection = openConnection(executionUrl + "/status/" + status, "POST");
		assertExpectedResponseCode(200, connection.getResponseCode());
	}

	private void assertExpectedResponseCode(int expectedResponseCode, int actualResponseCode) throws IOException {
		if (actualResponseCode != expectedResponseCode) {
			throw new IOException("Expected Response Code " + expectedResponseCode + ", but got " + actualResponseCode);
		}
	}

	private HttpURLConnection openConnection(String url, String method) throws IOException {
		HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
		connection.setRequestMethod(method);
		connection.setRequestProperty("User-Agent", getClass().getName());
		return connection;
	}

}

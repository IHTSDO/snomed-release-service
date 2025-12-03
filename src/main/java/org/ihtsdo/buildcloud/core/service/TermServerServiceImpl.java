package org.ihtsdo.buildcloud.core.service;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.ihtsdo.otf.constants.Concepts;
import org.ihtsdo.otf.rest.client.RestClientException;
import org.ihtsdo.otf.rest.client.terminologyserver.SnowstormRestClient;
import org.ihtsdo.otf.rest.client.terminologyserver.SnowstormRestClient.ExportCategory;
import org.ihtsdo.otf.rest.client.terminologyserver.SnowstormRestClient.ExportType;
import org.ihtsdo.otf.rest.client.terminologyserver.SnowstormRestClientFactory;
import org.ihtsdo.otf.rest.client.terminologyserver.pojo.Branch;
import org.ihtsdo.otf.rest.client.terminologyserver.pojo.CodeSystem;
import org.ihtsdo.otf.rest.client.terminologyserver.pojo.CodeSystemVersion;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.otf.utils.ZipFileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.io.*;
import java.nio.file.*;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class TermServerServiceImpl implements TermServerService {

	@Value("${snowstorm.reasonerId}")
	private String reasonerId;

	@Value("${snowstorm.url}")
	private String termServerUrl;

	@Value("${srs.file-export.max.retry:3}")
	private int maxExportRetry;

	@Value("${srs.file-export.retry.delay:20000}")
	private long retryDelayInMillis;

	private static final Logger logger = LoggerFactory.getLogger(TermServerServiceImpl.class);

	private static final String DELTA = "Delta";
	private static final String SNAPSHOT = "Snapshot";

	@Override
	public File export(String branchPath, String effectiveDate, Set<String> exportModuleIds, ExportCategory exportCategory) throws BusinessServiceException {
		SnowstormRestClient snowstormRestClient = getSnowstormClient();
		waitForBranchUnlock(snowstormRestClient, branchPath);
		File exportFile = performExportWithRetry(snowstormRestClient, branchPath, effectiveDate, exportModuleIds, exportCategory);
		return processExportFile(exportFile, effectiveDate);
	}

	private void waitForBranchUnlock(SnowstormRestClient client, String branchPath) throws BusinessServiceException {
		int counter = 0;
		while (counter++ < maxExportRetry) {
			try {
				if (!client.isBranchLocked(branchPath)) {
					return;
				}
			} catch (RestClientException e) {
				throw new BusinessServiceException(
						String.format("Failed to check branch lock status for %s. Error: %s",
								branchPath, e.getCause() != null ? e.getCause().getMessage() : e.getMessage()), e);
			}

			logger.info("Branch {} is locked. SRS will wait {} seconds and retry.", branchPath, retryDelayInMillis / 1000);
			try {
				Thread.sleep(retryDelayInMillis);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new BusinessServiceException("Sleep interrupted while retrying export on locked branch", e);
			}
		}
		throw new BusinessServiceException("Branch " + branchPath + " remained locked after " + maxExportRetry + " retries.");
	}

	private File performExportWithRetry(SnowstormRestClient client, String branchPath, String effectiveDate,
	                                    Set<String> exportModuleIds, ExportCategory exportCategory) throws BusinessServiceException {
		int counter = 0;
		while (counter++ < maxExportRetry) {
			try {
				return client.export(branchPath, effectiveDate, exportModuleIds, exportCategory, ExportType.SNAPSHOT);
			} catch (Exception e) {
				logger.error("Failed to export from branch {} on attempt {} due to {}", branchPath, counter, ExceptionUtils.getRootCauseMessage(e));
				if (counter == maxExportRetry) {
					throw new BusinessServiceException(
							String.format("Failed to export from %s after %d retries. Error: %s",
									branchPath, maxExportRetry, e.getCause() != null ? e.getCause().getMessage() : e.getMessage()), e);
				} else {
					logger.info("Retry will start in {} seconds", retryDelayInMillis / 1000);
					try {
						Thread.sleep(retryDelayInMillis);
					} catch (InterruptedException ie) {
						Thread.currentThread().interrupt();
						throw new BusinessServiceException("Sleep interrupted while retrying failed export", e);
					}
				}
			}
		}
		throw new BusinessServiceException("Export failed after maximum retries for branch " + branchPath);
	}

	private File processExportFile(File exportFile, String effectiveDate) throws BusinessServiceException {
		File tempDir = null;
		try {
			tempDir = Files.createTempDirectory("export-temp").toFile();
			unzipFlat(exportFile, tempDir);
			renameFiles(tempDir, SNAPSHOT, DELTA);
			enforceReleaseDate(tempDir, effectiveDate);
			File newZipFile = File.createTempFile("term-server-export", ".zip");
			ZipFileUtils.zip(tempDir.getAbsolutePath(), newZipFile.getAbsolutePath());
			return newZipFile;
		} catch (IOException e) {
			throw new BusinessServiceException("Failed to process export data from Snowstorm", e);
		} finally {
			if (tempDir != null) {
				FileUtils.deleteQuietly(tempDir);
			}
		}
	}


	@Override
	public List<CodeSystem> getCodeSystems() {
		SnowstormRestClient snowstormRestClient = getSnowstormClient();
		return snowstormRestClient.getCodeSystems();
	}

	@Override
	public List <CodeSystemVersion> getCodeSystemVersions(String shortName, boolean showFutureVersions, boolean showInternalReleases) {
		SnowstormRestClient snowstormRestClient = getSnowstormClient();
		return snowstormRestClient.getCodeSystemVersions(shortName, showFutureVersions, showInternalReleases);
	}

	@Override
	public Branch getBranch(String branchPath) throws RestClientException {
		SnowstormRestClient snowstormRestClient = getSnowstormClient();
		return snowstormRestClient.getBranch(branchPath);
	}

	@Override
	public void updateCodeSystemVersionPackage(String codeSystemShortName, String effectiveDate, String releasePackage) throws RestClientException {
		SnowstormRestClient snowstormRestClient = getSnowstormClient();
		snowstormRestClient.updateCodeSystemVersionPackage(codeSystemShortName, effectiveDate, releasePackage);
	}

	@Override
	public Set<String> getModulesForBranch(String branchPath) throws RestClientException {
		SnowstormRestClient snowstormRestClient = getSnowstormClient();
		return snowstormRestClient.eclQuery(branchPath, "<<" + Concepts.MODULE, 1000);
	}


	public void unzipFlat(File archive, File targetDir) throws BusinessServiceException, IOException {

		if (!targetDir.exists() || !targetDir.isDirectory()) {
			throw new BusinessServiceException(targetDir + " is not a viable directory in which to extract archive");
		}

		try (ZipInputStream zis = new ZipInputStream(new FileInputStream(archive))) {
			ZipEntry ze = zis.getNextEntry();
			while (ze != null) {
				if (!ze.isDirectory()) {
					Path p = Paths.get(ze.getName());
					String extractedFileName = p.getFileName().toString();
					File extractedFile = new File(targetDir, extractedFileName);
					try (OutputStream out = new FileOutputStream(extractedFile)) {
						IOUtils.copy(zis, out);
					}
				}
				ze = zis.getNextEntry();
			}
		}
	}

	private SnowstormRestClient getSnowstormClient() {
		return new SnowstormRestClientFactory(termServerUrl, this.reasonerId).getClient();
	}

	private void renameFiles(File targetDirectory, String find, String replace) {
		Assert.isTrue(targetDirectory.isDirectory(), targetDirectory.getAbsolutePath()
				+ " must be a directory in order to rename files from " + find + " to " + replace);
		for (File thisFile : targetDirectory.listFiles()) {
			renameFile(targetDirectory, thisFile, find, replace);
		}
	}

	private void renameFile(File parentDir, File thisFile, String find, String replace) {
		if (thisFile.exists() && !thisFile.isDirectory()) {
			String currentName = thisFile.getName();
			String newName = currentName.replace(find, replace);
			if (!newName.equals(currentName)) {
				File newFile = new File(parentDir, newName);
				if (!thisFile.renameTo(newFile)) {
					throw new IllegalStateException("Failed to rename " + thisFile + " to " + newFile);
				}
			}
		}
	}

	private void enforceReleaseDate(File extractDir, String enforcedReleaseDate) throws BusinessServiceException {
		//Loop through all the files in the directory and change the release date if required
		for (File thisFile : extractDir.listFiles()) {
			if (thisFile.isFile()) {
				String thisReleaseDate = findDateInString(thisFile.getName(), true);
				if (thisReleaseDate != null && !thisReleaseDate.equals("_" + enforcedReleaseDate)) {
					logger.debug("Modifying releaseDate in {} to _{}", thisFile.getName(), enforcedReleaseDate);
					renameFile(extractDir, thisFile, thisReleaseDate, "_" + enforcedReleaseDate);
				}
			}
		}
	}

	public String findDateInString(String str, boolean optional) throws BusinessServiceException {
		Matcher dateMatcher = Pattern.compile("(_\\d{8})").matcher(str);
		if (dateMatcher.find()) {
			return dateMatcher.group();
		} else {
			if (optional) {
				logger.warn("Did not find a date in: {}", str);
			} else {
				throw new BusinessServiceException("Unable to determine date from " + str);
			}
		}
		return null;
	}
}

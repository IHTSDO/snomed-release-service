package org.ihtsdo.buildcloud.service;

import com.google.common.io.Files;
import org.apache.commons.io.IOUtils;
import org.ihtsdo.otf.constants.Concepts;
import org.ihtsdo.otf.rest.client.RestClientException;
import org.ihtsdo.otf.rest.client.terminologyserver.SnowstormRestClient;
import org.ihtsdo.otf.rest.client.terminologyserver.SnowstormRestClient.ExportCategory;
import org.ihtsdo.otf.rest.client.terminologyserver.SnowstormRestClient.ExportType;
import org.ihtsdo.otf.rest.client.terminologyserver.SnowstormRestClientFactory;

import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.otf.utils.ZipFileUtils;
import org.ihtsdo.sso.integration.SecurityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class TermServerServiceImpl implements TermServerService{

	@Value("${snowstorm.reasonerId}")
	private String reasonerId;

	@Value("${snowstorm.path}")
	private String snowstormPath;

	private static final Logger logger = LoggerFactory.getLogger(TermServerService.class);

	private static final String DELTA = "Delta";
	private static final String SNAPSHOT = "Snapshot";


	@Override
	public File export(String termServerUrl, String branchPath, String effectiveDate, Set<String> excludedModuleIds, ExportCategory exportCategory) throws BusinessServiceException {
		try {
			String snowstormUrl = termServerUrl + snowstormPath;
			logger.info("Starting export from snowstorm {} on branch {} ", snowstormUrl, branchPath);
			System.out.println("Security context = " + SecurityContextHolder.getContext().getAuthentication());
			System.out.println("Token=" + SecurityUtil.getAuthenticationToken());
			SnowstormRestClientFactory clientFactory = new SnowstormRestClientFactory(snowstormUrl, reasonerId);
			SnowstormRestClient snowstormRestClient = clientFactory.getClient();
			Set<String> moduleList = buildModulesList(snowstormRestClient, branchPath, excludedModuleIds);
			File export = snowstormRestClient.export(branchPath, effectiveDate, moduleList, exportCategory, ExportType.SNAPSHOT);
			File extractDir = Files.createTempDir();
			unzipFlat(export, extractDir);
			renameFiles(extractDir, SNAPSHOT, DELTA);
			enforceReleaseDate(extractDir, effectiveDate);
			File tempDir = Files.createTempDir();
			File newZipFile = new File(tempDir,"term-server.zip");
			ZipFileUtils.zip(extractDir.getAbsolutePath(), newZipFile.getAbsolutePath());
			return newZipFile;
		} catch (IOException e) {
			logger.error("Failed export data from term server.", e);
			throw new BusinessServiceException(e);
		}
	}


	public void unzipFlat(File archive, File targetDir) throws BusinessServiceException, IOException {

		if (!targetDir.exists() || !targetDir.isDirectory()) {
			throw new BusinessServiceException(targetDir + " is not a viable directory in which to extract archive");
		}

		ZipInputStream zis = new ZipInputStream(new FileInputStream(archive));
		ZipEntry ze = zis.getNextEntry();
		try {
			while (ze != null) {
				if (!ze.isDirectory()) {
					Path p = Paths.get(ze.getName());
					String extractedFileName = p.getFileName().toString();
					File extractedFile = new File(targetDir, extractedFileName);
					OutputStream out = new FileOutputStream(extractedFile);
					IOUtils.copy(zis, out);
					IOUtils.closeQuietly(out);
				}
				ze = zis.getNextEntry();
			}
		} finally {
			zis.closeEntry();
			zis.close();
		}
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
				thisFile.renameTo(newFile);
			}
		}
	}

	private void enforceReleaseDate(File extractDir, String enforcedReleaseDate) throws BusinessServiceException {
		//Loop through all the files in the directory and change the release date if required
		for (File thisFile : extractDir.listFiles()) {
			if (thisFile.isFile()) {
				String thisReleaseDate = findDateInString(thisFile.getName(), true);
				if (thisReleaseDate != null && !thisReleaseDate.equals("_" + enforcedReleaseDate)) {
					logger.debug("Modifying releaseDate in " + thisFile.getName() + " to _" + enforcedReleaseDate);
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
				logger.warn("Did not find a date in: " + str);
			} else {
				throw new BusinessServiceException("Unable to determine date from " + str);
			}
		}
		return null;
	}

	private Set<String> buildModulesList(SnowstormRestClient SnowstormRestClient, String branchPath, Set<String> excludedModuleIds) throws BusinessServiceException {
		// If any modules are excluded build a list of modules to include
		Set<String> exportModuleIds = null;
		if (excludedModuleIds != null && !excludedModuleIds.isEmpty()) {
			try {
				Set<String> allModules = SnowstormRestClient.eclQuery(branchPath, "<<" + Concepts.MODULE, 1000);
				allModules.removeAll(excludedModuleIds);
				exportModuleIds = new HashSet<>();
				exportModuleIds.addAll(allModules);
				logger.info("Excluded modules are {}, included modules are {} for release on {}", excludedModuleIds, exportModuleIds, branchPath);
			} catch (RestClientException e) {
				throw new BusinessServiceException("Failed to build list of modules for export.", e);
			}
		}
		return exportModuleIds;
	}
}

package org.ihtsdo.buildcloud.service;

import com.google.common.io.Files;
import org.apache.commons.io.IOUtils;
import org.ihtsdo.otf.constants.Concepts;
import org.ihtsdo.otf.rest.client.RestClientException;

import org.ihtsdo.otf.rest.client.terminologyserver.SnowOwlRestClient;
import org.ihtsdo.otf.rest.client.terminologyserver.SnowOwlRestClientFactory;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.otf.rest.exception.ProcessWorkflowException;
import org.ihtsdo.otf.utils.DateUtils;
import org.ihtsdo.otf.utils.ZipFileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

@Service
public class TermServerServiceImpl implements TermServerService{

    @Value("${snowowl.reasonerId}")
    private String reasonerId;

    @Value("${snowowl.path}")
    private String snowowlPath;

    @Value("${snowstorm.path}")
    private String snowstormPath;

    private static final Logger logger = LoggerFactory.getLogger(TermServerService.class);

    private static final String DELTA = "Delta";
    private static final String SNAPSHOT = "Snapshot";


    @Override
    public File export(boolean useSnowOwl, String termServerUrl, String branchPath, String effectiveDate, Set<String> excludedModuleId, SnowOwlRestClient.ExportCategory exportCategory) throws BusinessServiceException, IOException, ProcessWorkflowException {
        String contextPath = useSnowOwl ? snowowlPath : snowstormPath;
        String snowOwlUrl = termServerUrl + contextPath;
        SnowOwlRestClientFactory clientFactory = new SnowOwlRestClientFactory(snowOwlUrl, reasonerId, true);
        SnowOwlRestClient snowOwlRestClient = clientFactory.getClient();
        Set<String> moduleList = useSnowOwl ? buildModulesList(snowOwlRestClient, branchPath, excludedModuleId) : null;
        File export = snowOwlRestClient.export(branchPath, effectiveDate, moduleList, exportCategory, SnowOwlRestClient.ExportType.SNAPSHOT);
        try {
            ZipFile zipFile = new ZipFile(export);
            File extractDir = Files.createTempDir();
            unzipFlat(export, extractDir);
            renameFiles(extractDir, SNAPSHOT, DELTA);
            enforceReleaseDate(extractDir, effectiveDate);
            File tempDir = Files.createTempDir();
            File newZipFile = new File(tempDir,"term-server.zip");
            ZipFileUtils.zip(extractDir.getAbsolutePath(), newZipFile.getAbsolutePath());
            return newZipFile;
        } catch (IOException e) {
            String returnedError = org.apache.commons.io.FileUtils.readFileToString(export);
            logger.error("Failed export data from term server. Term server returned error: {}", returnedError);
            throw new BusinessServiceException("Failed export data from term server. Term server returned error:" + returnedError);
        }

    }


    public void unzipFlat(File archive, File targetDir) throws ProcessWorkflowException, IOException {

        if (!targetDir.exists() || !targetDir.isDirectory()) {
            throw new ProcessWorkflowException(targetDir + " is not a viable directory in which to extract archive");
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

    private void enforceReleaseDate(File extractDir, String enforcedReleaseDate) throws ProcessWorkflowException {
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

    public String findDateInString(String str, boolean optional) throws ProcessWorkflowException {
        Matcher dateMatcher = Pattern.compile("(_\\d{8})").matcher(str);
        if (dateMatcher.find()) {
            return dateMatcher.group();
        } else {
            if (optional) {
                logger.warn("Did not find a date in: " + str);
            } else {
                throw new ProcessWorkflowException("Unable to determine date from " + str);
            }
        }
        return null;
    }

    private Set<String> buildModulesList(SnowOwlRestClient snowOwlRestClient, String branchPath, Set<String> excludedModuleIds) throws BusinessServiceException {
        // If any modules are excluded build a list of modules to include
        Set<String> exportModuleIds = null;
        if (excludedModuleIds != null && !excludedModuleIds.isEmpty()) {
            try {
                Set<String> allModules = snowOwlRestClient.eclQuery(branchPath, "<<" + Concepts.MODULE, 1000);
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

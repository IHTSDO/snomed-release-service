package org.ihtsdo.buildcloud.core.service;

import org.apache.commons.io.FileUtils;
import org.ihtsdo.buildcloud.core.dao.BuildDAO;
import org.ihtsdo.buildcloud.core.entity.Build;
import org.ihtsdo.buildcloud.core.entity.ReleaseCenter;
import org.ihtsdo.buildcloud.core.service.browser.update.BrowserUpdateServerService;
import org.ihtsdo.buildcloud.core.service.build.RF2Constants;
import org.ihtsdo.buildcloud.rest.pojo.BrowserUpdateJob;
import org.ihtsdo.buildcloud.rest.pojo.BrowserUpdateRequest;
import org.ihtsdo.buildcloud.rest.pojo.PostReleaseRequest;
import org.ihtsdo.otf.rest.client.RestClientException;
import org.ihtsdo.otf.rest.client.terminologyserver.pojo.CodeSystem;
import org.ihtsdo.otf.rest.client.terminologyserver.pojo.CodeSystemVersion;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.snomed.module.storage.ModuleMetadata;
import org.snomed.module.storage.ModuleStorageCoordinatorException;
import org.snomed.otf.delta.DeltaGeneratorTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AdminServiceImpl implements AdminService {

    @Autowired
    private BuildService buildService;

    @Autowired
    private PublishService publishService;

    @Autowired
    private ReleaseService releaseService;

    @Autowired
    private ReleaseCenterService releaseCenterService;

    @Autowired
    private BrowserUpdateServerService browserUpdateServerService;

    @Autowired
    private BuildDAO buildDAO;

    @Autowired
    private ModuleStorageCoordinatorCache moduleStorageCoordinatorCache;

    Map<String, BrowserUpdateJob> browserUpdateJobMap = new HashMap<>();

    public BrowserUpdateJob getBrowserUpdateJob(String jobId) {
        return browserUpdateJobMap.get(jobId);
    }

    @Override
    @Async
    public void triggerBrowserUpdate(String releaseCenterKey, BrowserUpdateRequest request, SecurityContext securityContext, String browserUpdateId) {
        SecurityContextHolder.setContext(securityContext);
        BrowserUpdateJob browserUpdateJob = new BrowserUpdateJob();
        browserUpdateJob.setJobId(browserUpdateId);
        browserUpdateJob.setStatus(BrowserUpdateJob.JobStatus.RUNNING);
        browserUpdateJobMap.put(browserUpdateId, browserUpdateJob);
        try {
            processBrowserUpdate(releaseCenterKey, request, browserUpdateJob);
        } catch (Exception e) {
            browserUpdateJob.setStatus(BrowserUpdateJob.JobStatus.FAILED);
            browserUpdateJob.setErrorMessage(e.getMessage());
        }
    }

    private void processBrowserUpdate(String releaseCenterKey, BrowserUpdateRequest request, BrowserUpdateJob browserUpdateJob) throws BusinessServiceException, RestClientException, IOException {
        File prospectiveReleaseFile = null;
        File importReleaseFile = null;
        try {
            browserUpdateServerService.checkConnection();

            ReleaseCenter releaseCenter = releaseCenterService.find(releaseCenterKey);
            if (releaseCenter == null || releaseCenter.getCodeSystem() == null) {
                throw new BusinessServiceException("Could not find any code system associated with the release center " + releaseCenterKey);
            }
            Build build = buildService.find(releaseCenterKey, request.getReleasedProductKey(), request.getReleasedBuildKey(), true, null, null, null);

            String codeSystemShortName = releaseCenter.getCodeSystem();
            CodeSystem codeSystem = browserUpdateServerService.getCodeSystem(codeSystemShortName);
            List<CodeSystemVersion> versions = browserUpdateServerService.getCodeSystemVersions(codeSystemShortName, true);
            for (CodeSystemVersion version : versions) {
                if (version.getVersion().equals(build.getConfiguration().getEffectiveTimeFormatted())) {
                    throw new BusinessServiceException("A version with the effective date " + version.getVersion() + " was found. No Browser update required");
                }
            }
            browserUpdateServerService.rollBackDailyBuild(codeSystemShortName);

            String publishedReleaseFileName = getReleaseFileName(releaseCenterKey, request.getReleasedProductKey(), request.getReleasedBuildKey());
            prospectiveReleaseFile = downloadProspectiveReleasePackage(build, publishedReleaseFileName);
            importReleaseFile = getImportFile(request, versions, releaseCenterKey, codeSystemShortName, prospectiveReleaseFile, publishedReleaseFileName);

            String dependencyRelease = build.getConfiguration().getExtensionConfig() != null ? build.getConfiguration().getExtensionConfig().getDependencyRelease() : null;
            upgradeCodeSystemIfNeeded(codeSystemShortName, dependencyRelease, versions);

            browserUpdateServerService.createAndStartFileImport(request.getImportType().name(), codeSystem.getBranchPath(), importReleaseFile);
            browserUpdateJob.setStatus(BrowserUpdateJob.JobStatus.COMPLETED);
        } catch (ModuleStorageCoordinatorException.OperationFailedException |
                 ModuleStorageCoordinatorException.InvalidArgumentsException |
                 ModuleStorageCoordinatorException.ResourceNotFoundException e) {
            throw new BusinessServiceException(e);
        } finally {
            if (prospectiveReleaseFile != null) {
                FileUtils.deleteQuietly(prospectiveReleaseFile);
            }
            if (importReleaseFile != null && importReleaseFile.exists()) {
                FileUtils.deleteQuietly(importReleaseFile);
            }
        }
    }

    @Override
    public void runPostReleaseTask(String releaseCenterKey, PostReleaseRequest request) throws BusinessServiceException, RestClientException, IOException {
        Build build = buildService.find(releaseCenterKey, request.releasedSourceProductKey(), request.releasedSourceBuildKey(), true, null, null, null);
        String publishedReleaseFileName = getReleaseFileName(releaseCenterKey, request.releasedSourceProductKey(), request.releasedSourceBuildKey());
        publishService.copyReleaseFileToPublishedStore(build);
        publishService.copyReleaseFileToVersionedContentStore(build);
        releaseService.startNewAuthoringCycle(releaseCenterKey, request.dailyBuildProductKey(), build, request.nextCycleEffectiveTime(), publishedReleaseFileName, request.newDependencyPackage());
    }

    private File getImportFile(BrowserUpdateRequest request, List<CodeSystemVersion> versions, String releaseCenterKey, String codeSystemShortName, File prospectiveReleaseFile, String publishedReleaseFileName) throws BusinessServiceException, IOException, ModuleStorageCoordinatorException.OperationFailedException, ModuleStorageCoordinatorException.ResourceNotFoundException, ModuleStorageCoordinatorException.InvalidArgumentsException {
        File previousReleaseFile = null;
        try {
            if (BrowserUpdateRequest.ImportType.DELTA.equals(request.getImportType())) {
                if (versions.isEmpty()) return prospectiveReleaseFile;

                CodeSystemVersion latestVersion = versions.get(versions.size() - 1);
                String transformedShortNameForModuleStorageCoordinator = RF2Constants.SNOMEDCT.equals(codeSystemShortName) ? RF2Constants.INT : codeSystemShortName.substring(codeSystemShortName.indexOf('-') + 1);
                List<ModuleMetadata> moduleMetadata = moduleStorageCoordinatorCache.getAllReleases().get(transformedShortNameForModuleStorageCoordinator);
                if (moduleMetadata.isEmpty())
                    throw new BusinessServiceException("No module metadata found against code system " + codeSystemShortName + " in Module Storage Coordinator");
                ModuleMetadata found = moduleMetadata.stream().filter(item -> Objects.equals(item.getEffectiveTime(), latestVersion.getEffectiveDate())).findFirst().orElse(null);
                if (found == null)
                    throw new BusinessServiceException("Could not find any metadata with effective date  " + latestVersion.getEffectiveDate() + " against code system " + codeSystemShortName + " in Module Storage Coordinator");

                previousReleaseFile = downloadPreviousReleasePackage(releaseCenterKey, found.getFilename());
                if (previousReleaseFile == null)
                    throw new BusinessServiceException("Could not find the previous release " + found.getFilename());
                return generateDelta(previousReleaseFile, prospectiveReleaseFile, publishedReleaseFileName);
            } else {
                return prospectiveReleaseFile;
            }
        } finally {
            if (previousReleaseFile != null) {
                FileUtils.deleteQuietly(previousReleaseFile);
            }
        }
    }

    private void upgradeCodeSystemIfNeeded(String codeSystemShortName, String dependencyRelease, List<CodeSystemVersion> versions) throws BusinessServiceException {
        if (!versions.isEmpty()) {
            CodeSystemVersion latestVersion = versions.get(versions.size() - 1);
            if (latestVersion.getDependantVersionEffectiveTime() != null && dependencyRelease != null
                    && !dependencyRelease.contains(String.valueOf(latestVersion.getDependantVersionEffectiveTime()))) {
                browserUpdateServerService.upgradeCodeSystem(codeSystemShortName, extractEffectiveTimeFromVersion(dependencyRelease));
            }
        }
    }

    private String getReleaseFileName(String releaseCenterKey, String productKey, String buildKey) throws BusinessServiceException {
        List<String> fileNames = buildService.getOutputFilePaths(releaseCenterKey, productKey, buildKey);
        for (String fileName : fileNames) {
            if (fileName.endsWith(RF2Constants.ZIP_FILE_EXTENSION)) {
                return fileName;
            }
        }
        throw new BusinessServiceException("Could not find the release package file for build " + buildKey);
    }

    private File generateDelta(File previousRelease, File prospectiveRelease, String prospectiveReleaseFileName) throws IOException {
        String[] args = new String[]{previousRelease.getAbsolutePath(), prospectiveRelease.getAbsolutePath(), "--latest-state"};
        DeltaGeneratorTool.main(args);
        File curDir = new File(".");
        File[] filesList = curDir.listFiles();
        if (filesList == null) return null;
        for (File file : filesList) {
            if (file.isFile() && file.getName().equals(prospectiveReleaseFileName)) {
                return file;
            }
        }
        return null;
    }

    private File downloadProspectiveReleasePackage(Build build, String fileName) throws IOException {
        File releaseFile = File.createTempFile(fileName, RF2Constants.ZIP_FILE_EXTENSION);
        try (InputStream inputFileStream = buildDAO.getOutputFileInputStream(build, fileName);
             FileOutputStream out = new FileOutputStream(releaseFile)) {
            if (inputFileStream != null) {
                StreamUtils.copy(inputFileStream, out);
            } else {
                FileUtils.forceDelete(releaseFile);
                return null;
            }
        }

        return releaseFile;
    }

    private File downloadPreviousReleasePackage(String releaseCenterKey, String fileName) throws IOException {
        File releaseFile = File.createTempFile(fileName, RF2Constants.ZIP_FILE_EXTENSION);
        try (InputStream inputFileStream = publishService.downloadPublishedRelease(releaseCenterKey, fileName);
             FileOutputStream out = new FileOutputStream(releaseFile)) {
            if (inputFileStream != null) {
                StreamUtils.copy(inputFileStream, out);
            } else {
                FileUtils.forceDelete(releaseFile);
                return null;
            }
        }
        return releaseFile;
    }

    private Integer extractEffectiveTimeFromVersion(String dependencyVersion) throws BusinessServiceException {
        String effectiveTime = null;
        try {
            Pattern pattern = null;
            String text;
            if (dependencyVersion.endsWith(RF2Constants.ZIP_FILE_EXTENSION)) {
                pattern = Pattern.compile("\\d{8}(?=(T\\d+|.zip))");
                String[] splits = dependencyVersion.split("/");
                text = splits[splits.length - 1];
            } else {
                pattern = Pattern.compile("(?<=_)(\\d{8})");
                text = dependencyVersion;
            }
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                effectiveTime = matcher.group();
            }
        } catch (Exception e) {
            throw new BusinessServiceException("Encounter error when extracting effective time from " + dependencyVersion);
        }
        return Integer.parseInt(effectiveTime);
    }

}

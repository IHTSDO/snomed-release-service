package org.ihtsdo.buildcloud.core.service.build.compare.type;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.ihtsdo.buildcloud.core.dao.BuildDAO;
import org.ihtsdo.buildcloud.core.dao.helper.S3PathHelper;
import org.ihtsdo.buildcloud.core.entity.Build;
import org.ihtsdo.buildcloud.core.entity.PreConditionCheckReport;
import org.ihtsdo.buildcloud.core.service.PublishService;
import org.ihtsdo.buildcloud.core.service.build.RF2Constants;
import org.ihtsdo.buildcloud.core.service.build.compare.BuildComparisonManager;
import org.ihtsdo.buildcloud.core.service.build.compare.ComponentComparison;
import org.ihtsdo.buildcloud.core.service.build.compare.DefaultComponentComparisonReport;
import org.ihtsdo.otf.rest.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class ReleasePackageComparison extends ComponentComparison {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReleasePackageComparison.class);

    public enum PackageTestName {
        FILE_CONTENT("File content"), PACKAGE_FILE("Package file");

        private final String label;

        PackageTestName(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    @Value("${srs.publish.job.useOwnBackupBucket}")
    private Boolean useOwnBackupBucket;

    @Value("${srs.publish.job.backup.storage.bucketName}")
    private String publishJobBackupStorageBucketName;

    @Autowired
    private BuildDAO buildDAO;

    @Autowired
    private PublishService publishService;

    @Override
    public String getTestName() {
        return BuildComparisonManager.TestType.RELEASE_PACKAGE_TEST.getLabel();
    }

    @Override
    public String getTestNameShortname() {
        return BuildComparisonManager.TestType.RELEASE_PACKAGE_TEST.name();
    }

    @Override
    public int getTestOrder() {
        return BuildComparisonManager.TestType.RELEASE_PACKAGE_TEST.getTestOrder();
    }

    @Override
    public void findDiff(Build leftBuild, Build rightBuild) throws IOException {
        List<DefaultComponentComparisonReport> result = new ArrayList<>();
        File leftReleasePackage = null;
        File rightReleasePackage = null;
        File leftDir = null;
        File rightDir = null;
        try {
            try {
                leftReleasePackage = getReleaseFile(leftBuild);
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            }
            try {
                rightReleasePackage = getReleaseFile(rightBuild);
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            }
            if (leftReleasePackage != null && rightReleasePackage != null) {
                leftDir = Files.createTempDirectory("left-package-temp").toFile();
                rightDir = Files.createTempDirectory("right-package-temp").toFile();
                unzipFlat(leftReleasePackage, leftDir);
                unzipFlat(rightReleasePackage, rightDir);
                compareFiles(leftDir, rightDir, result);

                if (result.size() > 0) {
                    fail(result);
                } else {
                    pass();
                }
            } else {
                if ((leftReleasePackage == null && rightReleasePackage != null)
                    || (leftReleasePackage != null && rightReleasePackage == null)) {
                    DefaultComponentComparisonReport dto = new DefaultComponentComparisonReport();
                    dto.setName(PackageTestName.PACKAGE_FILE.name());
                    dto.setStatus(BuildComparisonManager.ComparisonState.NOT_FOUND.name());
                    dto.setExpected(leftReleasePackage != null ? getReleaseFileName(leftBuild) : null);
                    dto.setActual(rightReleasePackage != null ? getReleaseFileName(rightBuild) : null);
                    result.add(dto);
                    fail(result);
                }
            }
        } finally {
            if (leftReleasePackage != null) FileUtils.forceDelete(leftReleasePackage);
            if (rightReleasePackage != null) FileUtils.forceDelete(rightReleasePackage);
            if (leftDir != null) FileUtils.forceDelete(leftDir);
            if (rightDir != null) FileUtils.forceDelete(rightDir);
        }
    }

    private void compareFiles(File leftDir, File rightDir, List<DefaultComponentComparisonReport> result) throws IOException {
        File[] leftFiles = leftDir.listFiles();
        File[] rightFiles = rightDir.listFiles();
        for (File leftFile : leftFiles) {
            File rightFile = Arrays.stream(rightFiles).filter(file -> file.getName().equals(leftFile.getName())).findAny().orElse(null);
            DefaultComponentComparisonReport dto = new DefaultComponentComparisonReport();
            if (rightFile != null) {
                boolean contentEqual = IOUtils.contentEquals(new FileInputStream(leftFile), new FileInputStream(rightFile));
                if(!contentEqual) {
                    dto.setName(PackageTestName.FILE_CONTENT.name());
                    dto.setStatus(BuildComparisonManager.ComparisonState.CONTENT_MISMATCH.name());
                    dto.setExpected(leftFile.getName());
                    dto.setActual(rightFile.getName());
                    result.add(dto);
                }
            } else {
                dto.setName(PackageTestName.FILE_CONTENT.name());
                dto.setStatus(BuildComparisonManager.ComparisonState.DELETED.name());
                dto.setExpected(leftFile.getName());
                result.add(dto);
            }
        }

        for (File rightFile : rightFiles) {
            File leftFile = Arrays.stream(leftFiles).filter(file -> file.getName().equals(rightFile.getName())).findAny().orElse(null);
            if (leftFile == null) {
                DefaultComponentComparisonReport dto = new DefaultComponentComparisonReport();
                dto.setName(PackageTestName.FILE_CONTENT.name());
                dto.setStatus(BuildComparisonManager.ComparisonState.ADD_NEW.name());
                dto.setExpected(leftFile.getName());
                result.add(dto);
            }
        }
    }

    private File downloadReleasePackage(Build build, String fileName) throws IOException {
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

    private File downloadReleasePackage(String buildPath, String fileName) throws IOException {
        File releaseFile = File.createTempFile(fileName, RF2Constants.ZIP_FILE_EXTENSION);
        try (InputStream inputFileStream =  Boolean.TRUE.equals(useOwnBackupBucket) ? buildDAO.getOutputFileInputStream(this.publishJobBackupStorageBucketName, buildPath, fileName)
                                                                                    : buildDAO.getOutputFileInputStream(buildPath, fileName);
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

    private void unzipFlat(File archive, File targetDir) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(archive))) {
            ZipEntry ze = zis.getNextEntry();
            while (ze != null) {
                if (!ze.isDirectory()) {
                    Path p = Paths.get(ze.getName());
                    String extractedFileName = p.getFileName().toString();
                    File extractedFile = new File(targetDir, extractedFileName);
                    try (OutputStream out = new FileOutputStream(extractedFile);) {
                        IOUtils.copy(zis, out);
                    }
                }
                ze = zis.getNextEntry();
            }
        }
    }

    private File getReleaseFile(Build build) throws IOException {
        Build found = buildDAO.find(build.getReleaseCenterKey(), build.getProductKey(), build.getId(), false, false, false, null);
        if (found != null) {
            // Trying to find the output files from build folder
            List<String> outputFiles = buildDAO.listOutputFilePaths(build);
            String releaseFilePath = outputFiles.stream().filter(path -> path.endsWith(".zip")).findAny().orElse(null);
            if (releaseFilePath != null) {
                return downloadReleasePackage(build, releaseFilePath);
            }
        } else {
            // Trying to find  the output files from published folder
            Map<String, String> publishedBuildPathMap = publishService.getPublishedBuildPathMap(build.getReleaseCenterKey(), build.getProductKey());
            if (publishedBuildPathMap.containsKey(build.getId())) {
                List<String> outputFiles;
                if (Boolean.TRUE.equals(useOwnBackupBucket)) {
                    outputFiles = buildDAO.listOutputFilePaths(this.publishJobBackupStorageBucketName, publishedBuildPathMap.get(build.getId()));
                } else {
                    outputFiles = buildDAO.listOutputFilePaths(publishedBuildPathMap.get(build.getId()));
                }

                String releaseFilePath = outputFiles.stream().filter(path -> path.endsWith(".zip")).findAny().orElse(null);
                if (releaseFilePath != null) {
                    return downloadReleasePackage(publishedBuildPathMap.get(build.getId()), releaseFilePath);
                }
            }
        }

        throw new ResourceNotFoundException("Release file not found for build " + build.getId());
    }

    private String getReleaseFileName(Build build) {
        Build found = buildDAO.find(build.getReleaseCenterKey(), build.getProductKey(), build.getId(), false, false, false, null);
        if (found != null) {
            // Trying to find the output files from build folder
            List<String> outputFiles = buildDAO.listOutputFilePaths(build);
            return outputFiles.stream().filter(path -> path.endsWith(".zip")).findAny().orElse(null);
        } else {
            // Trying to find  the output files from published folder
            Map<String, String> publishedBuildPathMap = publishService.getPublishedBuildPathMap(build.getReleaseCenterKey(), build.getProductKey());
            if (publishedBuildPathMap.containsKey(build.getId())) {
                List<String> outputFiles;
                if (Boolean.TRUE.equals(useOwnBackupBucket)) {
                    outputFiles = buildDAO.listOutputFilePaths(this.publishJobBackupStorageBucketName, publishedBuildPathMap.get(build.getId()));
                } else {
                    outputFiles = buildDAO.listOutputFilePaths(publishedBuildPathMap.get(build.getId()));
                }
                return outputFiles.stream().filter(path -> path.endsWith(".zip")).findAny().orElse(null);
            }
        }

        return null;
    }
}

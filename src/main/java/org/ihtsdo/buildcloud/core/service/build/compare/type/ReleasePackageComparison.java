package org.ihtsdo.buildcloud.core.service.build.compare.type;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.ihtsdo.buildcloud.core.dao.BuildDAO;
import org.ihtsdo.buildcloud.core.entity.Build;
import org.ihtsdo.buildcloud.core.service.build.RF2Constants;
import org.ihtsdo.buildcloud.core.service.build.compare.BuildComparisonManager;
import org.ihtsdo.buildcloud.core.service.build.compare.ComponentComparison;
import org.ihtsdo.buildcloud.core.service.build.compare.DefaultComponentComparisonReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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

    @Autowired
    private BuildDAO buildDAO;

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
            String leftPackageFilePath = null;
            String rightPackageFilePath = null;
            try {
                List<String> leftOutputFiles = buildDAO.listOutputFilePaths(leftBuild);
                leftPackageFilePath = leftOutputFiles.stream().filter(path -> path.endsWith(".zip")).findAny().orElse(null);
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            }
            try {
                List<String> rightOutputFiles = buildDAO.listOutputFilePaths(rightBuild);
                rightPackageFilePath = rightOutputFiles.stream().filter(path -> path.endsWith(".zip")).findAny().orElse(null);
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            }
            if (leftPackageFilePath != null && rightPackageFilePath != null) {
                leftReleasePackage = downloadReleasePackage(leftBuild, leftPackageFilePath);
                rightReleasePackage = downloadReleasePackage(rightBuild, rightPackageFilePath);
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
                if ((leftPackageFilePath == null && rightPackageFilePath != null)
                    || (leftPackageFilePath != null && rightPackageFilePath == null)) {
                    DefaultComponentComparisonReport dto = new DefaultComponentComparisonReport();
                    dto.setName(PackageTestName.PACKAGE_FILE.name());
                    dto.setStatus(BuildComparisonManager.ComparisonState.NOT_FOUND.name());
                    dto.setExpected(leftPackageFilePath != null ? leftPackageFilePath : null);
                    dto.setActual(rightPackageFilePath != null ? rightPackageFilePath : null);
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
}

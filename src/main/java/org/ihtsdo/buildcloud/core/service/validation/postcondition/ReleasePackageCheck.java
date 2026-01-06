package org.ihtsdo.buildcloud.core.service.validation.postcondition;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.ihtsdo.buildcloud.core.dao.BuildDAO;
import org.ihtsdo.buildcloud.core.entity.Build;
import org.ihtsdo.buildcloud.core.service.NetworkRequired;
import org.ihtsdo.buildcloud.core.service.build.RF2Constants;
import org.ihtsdo.otf.rest.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Service
public class ReleasePackageCheck extends PostconditionCheck implements NetworkRequired {

    @Autowired
    private BuildDAO buildDAO;

    private static final Logger LOGGER = LoggerFactory.getLogger(ReleasePackageCheck.class);

    @Override
    public void runCheck(Build build) {
        String errorMsg = validateReleasePackageAndMD5FilesExisting(build);
        if (errorMsg != null) {
            fatalError(errorMsg);
            return;
        }

        // Validate presence and correctness of release_package_information.json
        if (!build.getConfiguration().isDailyBuild() && !build.getConfiguration().isBetaRelease()) {
            errorMsg = validateReleasePackageInformationJson(build);
            if (errorMsg != null) {
                fatalError(errorMsg);
                return;
            }
        }

        if (build.getConfiguration() != null && build.getConfiguration().isBetaRelease()) {
            try {
                errorMsg = validateBetaReleasePackage(build);
                if (errorMsg != null) {
                    fatalError("The following files are required starting with x for a Beta release: " + errorMsg);
                    return;
                }
            } catch (ResourceNotFoundException ex) {
                fail(ex.getMessage());
                return;
            }
        }

        pass();
    }

    private String validateReleasePackageAndMD5FilesExisting(Build build) {
        List<String> filePaths = buildDAO.listOutputFilePaths(build);
        boolean hasZipFile = filePaths.stream()
                .anyMatch(filepath -> filepath.endsWith(RF2Constants.ZIP_FILE_EXTENSION));
        boolean hasMd5File = filePaths.stream()
                .anyMatch(filepath -> filepath.endsWith(RF2Constants.MD5_FILE_EXTENSION));

        if (!hasZipFile && !hasMd5File) {
            return "The release package and MD5 files are missing from the S3 output-files";
        } else if (!hasZipFile) {
            return "The release package file is missing from the S3 output-files";
        } else if (!hasMd5File) {
            return "The MD5 file is missing from the S3 output-files";
        }
        return null;
    }

    /**
     * Validate the mandatory release_package_information.json file:
     * - must exist in the output files
     * - must be non-zero in size
     * - must contain valid JSON.
     */
    private String validateReleasePackageInformationJson(Build build) {
        List<String> filePaths = buildDAO.listOutputFilePaths(build);
        
        // Check if release_package_information.json exists in output files
        boolean fileExists = filePaths.stream()
                .anyMatch(filepath -> {
                    String normalized = filepath.replace("\\", "/").toLowerCase();
                    return normalized.endsWith("release_package_information.json");
                });

        if (!fileExists) {
            return "release_package_information.json is missing";
        }

        try (InputStream inputStream = buildDAO.getOutputFileStream(build, "release_package_information.json")) {
            String jsonContent = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            
            // Check for non-zero size
            if (jsonContent == null || jsonContent.trim().isEmpty()) {
                return "release_package_information.json is empty.";
            }

            // Validate JSON structure
            String x = validateJsonStructure(jsonContent);
            if (x != null) return x;
        } catch (ResourceNotFoundException e) {
            return "Failed to locate release_package_information.json in output-files: " + e.getMessage();
        } catch (IOException e) {
            LOGGER.error("Error while validating release_package_information.json", e);
            return "Error while reading release_package_information.json: " + e.getMessage();
        }

        return null;
    }

    private static String validateJsonStructure(String jsonContent) {
        try {
            JsonElement jsonElement = JsonParser.parseString(jsonContent);
            if (jsonElement == null || jsonElement.isJsonNull()) {
                return "release_package_information.json does not contain valid JSON.";
            }
            if (jsonElement.isJsonObject() &&
                    jsonElement.getAsJsonObject().isEmpty()) {
                return "release_package_information.json is empty.";
            }
        } catch (JsonSyntaxException e) {
            return "release_package_information.json is not valid JSON: " + e.getMessage();
        }
        return null;
    }

    private String validateBetaReleasePackage(Build build) throws ResourceNotFoundException {
        List<String> filePaths = buildDAO.listOutputFilePaths(build);
        String zippedFilePath = filePaths.stream()
                .filter(filepath -> filepath.endsWith(RF2Constants.ZIP_FILE_EXTENSION))
                .findAny()
                .orElse(null);

        if (zippedFilePath == null) {
            throw new ResourceNotFoundException("Package file could not be found");
        }

        String zippedFileName = zippedFilePath.substring(zippedFilePath.lastIndexOf("/") + 1);
        StringBuilder stringBuilder = new StringBuilder();
        
        if (!zippedFileName.startsWith(RF2Constants.BETA_RELEASE_PREFIX)) {
            stringBuilder.append(zippedFileName).append(",");
        }

        try (InputStream inputStream = buildDAO.getOutputFileStream(build, zippedFileName)) {
            Path tempFile = Files.createTempFile(zippedFileName, RF2Constants.ZIP_FILE_EXTENSION);
            Path tempDir = Files.createTempDirectory("beta-release-");

            try {
                FileUtils.copyInputStreamToFile(inputStream, tempFile.toFile());
                extractFilesFromZipToOneFolder(tempFile.toFile(), tempDir.toAbsolutePath().toString());
                
                File[] files = tempDir.toFile().listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (!file.isDirectory()) {
                            String fileName = file.getName();
                            if (!fileName.startsWith(RF2Constants.README_FILENAME_PREFIX) &&
                                    !fileName.endsWith(RF2Constants.README_FILENAME_EXTENSION) &&
                                    !fileName.startsWith(RF2Constants.RELEASE_INFORMATION_FILENAME_PREFIX) &&
                                    !fileName.endsWith(RF2Constants.RELEASE_INFORMATION_FILENAME_EXTENSION) &&
                                    !fileName.startsWith(RF2Constants.BETA_RELEASE_PREFIX)) {
                                stringBuilder.append(fileName).append(",");
                            }
                        }
                    }
                }
                
                String errorMsg = stringBuilder.toString();
                return StringUtils.hasLength(errorMsg) ? errorMsg.substring(0, errorMsg.length() - 1) : null;
            } finally {
                FileUtils.deleteQuietly(tempFile.toFile());
                FileUtils.deleteQuietly(tempDir.toFile());
            }
        } catch (IOException e) {
            LOGGER.error("Error occurred when validating beta release package", e);
            return "Error validating beta release package: " + e.getMessage();
        }
    }

    /**
     * Utility method for extracting a zip file to a given folder
     *
     * @param file      the zip file to be extracted
     * @param outputDir the output folder to extract the zip to
     * @throws IOException if an I/O error occurs
     */
    private void extractFilesFromZipToOneFolder(final File file, final String outputDir) throws IOException {
        try (ZipFile zipFile = new ZipFile(file)) {
            final Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                final ZipEntry entry = entries.nextElement();
                if (!entry.isDirectory()) {
                    String fileName = Path.of(entry.getName()).getFileName().toString();
                    File entryDestination = new File(outputDir, fileName);
                    
                    try (InputStream in = zipFile.getInputStream(entry);
                         OutputStream out = new FileOutputStream(entryDestination)) {
                        IOUtils.copy(in, out);
                    }
                }
            }
        }
    }
}

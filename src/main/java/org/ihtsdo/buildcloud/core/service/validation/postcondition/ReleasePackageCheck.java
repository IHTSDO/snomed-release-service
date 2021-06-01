package org.ihtsdo.buildcloud.core.service.validation.postcondition;

import com.google.common.io.Files;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.ihtsdo.buildcloud.core.service.NetworkRequired;
import org.ihtsdo.buildcloud.core.service.build.RF2Constants;
import org.ihtsdo.buildcloud.core.dao.BuildDAO;
import org.ihtsdo.buildcloud.core.entity.Build;
import org.ihtsdo.otf.rest.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import java.io.*;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ReleasePackageCheck extends PostconditionCheck implements NetworkRequired {

    @Autowired
    private BuildDAO buildDAO;

    @Override
    public void runCheck(Build build) {
        if (build.getConfiguration() != null && build.getConfiguration().isBetaRelease()) {
            try {
                String errorMsg = validateBetaReleasePackage(build);
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

    private String validateBetaReleasePackage(Build build) throws ResourceNotFoundException{
        List<String> filePaths = buildDAO.listOutputFilePaths(build);
        String zippedFileName = filePaths.stream().filter(filepath -> filepath.endsWith(RF2Constants.ZIP_FILE_EXTENSION)).findAny().orElse(null);
        if (zippedFileName != null) {
            StringBuilder stringBuilder = new StringBuilder();
            zippedFileName = zippedFileName.substring(zippedFileName.lastIndexOf("/") + 1);
            if (!zippedFileName.startsWith(RF2Constants.BETA_RELEASE_PREFIX)) {
                stringBuilder.append(zippedFileName).append(",");
            }
            InputStream inputStream = buildDAO.getOutputFileStream(build, zippedFileName);
            File tempFile = null, tempDir = null;

            try {
                tempFile = File.createTempFile(zippedFileName, RF2Constants.ZIP_FILE_EXTENSION);
                FileUtils.copyInputStreamToFile(inputStream, tempFile);
                tempDir = Files.createTempDir();
                extractFilesFromZipToOneFolder(tempFile, tempDir.getAbsolutePath());
                File[] files = tempDir.listFiles();
                for (File file : files) {
                    if (!file.isDirectory()) {
                        if (!file.getName().startsWith(RF2Constants.README_FILENAME_PREFIX) &&
                                !file.getName().endsWith(RF2Constants.README_FILENAME_EXTENSION) &&
                                !file.getName().startsWith(RF2Constants.RELEASE_INFORMATION_FILENAME_PREFIX) &&
                                !file.getName().endsWith(RF2Constants.RELEASE_INFORMATION_FILENAME_EXTENSION) &&
                                !file.getName().startsWith(RF2Constants.BETA_RELEASE_PREFIX)) {
                            stringBuilder.append(file.getName()).append(",");
                        }
                    }
                }
                String errorMsg = stringBuilder.toString();
                return !StringUtils.isEmpty(errorMsg) ? errorMsg.substring(0, errorMsg.length() - 1) : null;
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (tempFile != null) {
                    tempFile.delete();
                }
                if (tempDir != null) {
                    tempDir.delete();
                }
            }
        } else {
            throw new ResourceNotFoundException("Package file could not be found");
        }
        return null;
    }

    /**
     * Utility method for extracting a zip file to a given folder
     *
     * @param file      the zip file to be extracted
     * @param outputDir the output folder to extract the zip to.
     * @throws IOException
     */
    private void extractFilesFromZipToOneFolder(final File file, final String outputDir) throws IOException {
        try (ZipFile zipFile = new ZipFile(file)) {
            final Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                final ZipEntry entry = entries.nextElement();
                if (!entry.isDirectory()) {
                    InputStream in = null;
                    OutputStream out = null;
                    try {
                        in = zipFile.getInputStream(entry);
                        String fileName = Paths.get(entry.getName()).getFileName().toString();
                        File entryDestination = new File(outputDir, fileName);
                        out = new FileOutputStream(entryDestination);
                        IOUtils.copy(in, out);
                    } finally {
                        IOUtils.closeQuietly(in);
                        IOUtils.closeQuietly(out);
                    }
                }
            }
        }
    }
}

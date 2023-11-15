package org.ihtsdo.buildcloud.core.service;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.ihtsdo.buildcloud.core.dao.helper.S3PathHelper;
import org.ihtsdo.otf.dao.s3.S3Client;
import org.ihtsdo.otf.dao.s3.helper.FileHelper;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.security.NoSuchAlgorithmException;
import java.util.List;

@Service
@Transactional
public class ExternalMaintainedRefsetsServiceImpl implements ExternalMaintainedRefsetsService {

    public static final Logger LOGGER = LoggerFactory.getLogger(ExternalMaintainedRefsetsServiceImpl.class);

    private final FileHelper fileHelper;

    @Autowired
    private S3PathHelper s3PathHelper;

    @Autowired
    public ExternalMaintainedRefsetsServiceImpl(@Value("${srs.storage.bucketName}") final String storageBucketName,
                                                final S3Client s3Client) {
        fileHelper = new FileHelper(storageBucketName, s3Client);
    }

    @Override
    public void copyExternallyMaintainedFiles(String releaseCenterKey, String source, String target, boolean isHeaderOnly) throws BusinessServiceException, IOException {
        String sourceDirPath = s3PathHelper.getExternallyMaintainedDirectoryPath(releaseCenterKey, source);
        String targetDirPath = s3PathHelper.getExternallyMaintainedDirectoryPath(releaseCenterKey, target);

        List<String> externalFiles = fileHelper.listFiles(sourceDirPath);
        for (String externalFile : externalFiles) {
            // Skip if current object is a directory
            if (StringUtils.isBlank(externalFile) || externalFile.endsWith(S3PathHelper.SEPARATOR)) {
                continue;
            }
            String sourceFilePath = sourceDirPath + externalFile;
            String targetFilePath = targetDirPath + externalFile.replaceAll(source, target);

            File tmpFile = File.createTempFile("sct2-file", ".txt");
            try {
                InputStream inputStream = fileHelper.getFileStream(sourceFilePath);
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                     PrintWriter writer = new PrintWriter(new BufferedOutputStream(new FileOutputStream(tmpFile)))) {
                    String str = reader.readLine();
                    if (str != null) {
                        writer.println(str);
                        if (!isHeaderOnly) {
                            while ((str = reader.readLine()) != null) {
                                writer.println(str);
                            }
                        }
                    }
                } catch (IOException e) {
                    LOGGER.error("Error copying object {} to temp file. Message: {}", sourceFilePath, e.getMessage());
                    throw new BusinessServiceException("Error copying object " + sourceFilePath + " to temp file. Message: " + e.getMessage(), e);
                }
                putFile(tmpFile, targetFilePath);
            } finally {
                FileUtils.forceDelete(tmpFile);
            }
        }
    }

    private void putFile(File file, String targetFilePath) throws BusinessServiceException {
        if (fileHelper.exists(targetFilePath)) {
            fileHelper.deleteFile(targetFilePath);
        }
        try {
            fileHelper.putFile(file, targetFilePath);
        } catch (NoSuchAlgorithmException | IOException | DecoderException e) {
            LOGGER.error("Error putting object to target {}. Message: {}", targetFilePath, e.getMessage());
            throw new BusinessServiceException("Error putting object to target. Message: " + e.getMessage(), e);
        }
    }
}
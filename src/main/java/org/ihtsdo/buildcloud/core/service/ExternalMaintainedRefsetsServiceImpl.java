package org.ihtsdo.buildcloud.core.service;

import org.apache.commons.codec.DecoderException;
import org.ihtsdo.otf.dao.s3.S3Client;
import org.ihtsdo.otf.dao.s3.helper.FileHelper;
import org.ihtsdo.otf.dao.s3.helper.S3ClientHelper;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.util.List;

@Service
@Transactional
public class ExternalMaintainedRefsetsServiceImpl implements ExternalMaintainedRefsetsService {

    public static final Logger LOGGER = LoggerFactory.getLogger(ExternalMaintainedRefsetsServiceImpl.class);

    private final FileHelper externallyMaintainedFileHelper;

    @Autowired
    public ExternalMaintainedRefsetsServiceImpl(@Value("${srs.build.externally-maintained-bucketName}") final String externallyMaintainedBucketName,
                                                final S3Client s3Client,
                                                final S3ClientHelper s3ClientHelper) {
        externallyMaintainedFileHelper = new FileHelper(externallyMaintainedBucketName, s3Client, s3ClientHelper);
    }

    @Override
    public void putFile(File file, String target) throws BusinessServiceException {
        if (externallyMaintainedFileHelper.exists(target)) {
            externallyMaintainedFileHelper.deleteFile(target);
        }
        try {
            externallyMaintainedFileHelper.putFile(file, target);
        } catch (NoSuchAlgorithmException | IOException | DecoderException e) {
            LOGGER.error("Error putting object to target {}. Message: {}", target, e.getMessage());
            throw new BusinessServiceException("Error putting object to target. Message: " + e.getMessage(), e);
        }
    }

    @Override
    public InputStream getFileStream(String filePath) {
        return externallyMaintainedFileHelper.getFileStream(filePath);
    }

    @Override
    public List <String> listFiles(String directoryPath) {
        return externallyMaintainedFileHelper.listFiles(directoryPath);
    }
}
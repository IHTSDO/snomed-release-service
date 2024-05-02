package org.ihtsdo.buildcloud.core.dao;

import org.ihtsdo.otf.dao.s3.S3Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.util.ArrayList;
import java.util.List;

@Service
public class BuildDAOCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(BuildDAOCache.class);

    @Cacheable(value = "build-records", key="#releaseCenterKey.concat('-').concat(#productKey)")
    public List<S3Object> getAllS3ObjectKeysWithCache(final S3Client s3Client, String buildBucketName, String prefix, String releaseCenterKey, String productKey) {
        return getS3Objects(s3Client, buildBucketName, prefix);
    }

    public List<S3Object> getAllS3ObjectKeysNoCache(final S3Client s3Client, String buildBucketName, String prefix) {
        return getS3Objects(s3Client, buildBucketName, prefix);
    }

    private static List<S3Object> getS3Objects(S3Client s3Client, String buildBucketName, String prefix) {
        LOGGER.debug("Reading S3Objects in {}, {} in batches.", buildBucketName, prefix);
        ListObjectsRequest listObjectsRequest = ListObjectsRequest.builder().bucket(buildBucketName).prefix(prefix).maxKeys(10000).build();
        List<S3Object> s3Objects = new ArrayList<>();
        boolean done = false;
        while (!done) {
            ListObjectsResponse listObjectsResponse = s3Client.listObjects(listObjectsRequest);
            s3Objects.addAll(listObjectsResponse.contents());
            if (Boolean.TRUE.equals(listObjectsResponse.isTruncated())) {
                String nextMarker = s3Objects.get(s3Objects.size() - 1).key();
                listObjectsRequest = ListObjectsRequest.builder().bucket(buildBucketName).prefix(prefix).maxKeys(10000).marker(nextMarker).build();
            } else {
                done = true;
            }
        }
        return s3Objects;
    }
}

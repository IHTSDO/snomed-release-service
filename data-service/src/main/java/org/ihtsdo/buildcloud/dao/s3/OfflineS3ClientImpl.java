package org.ihtsdo.buildcloud.dao.s3;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.model.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Offers an offline version of S3 cloud storage for demos or working without a connection.
 * N.B. Metadata and ACL security are not implemented.
 */
public class OfflineS3ClientImpl implements S3Client {

	public static final String UTF_8 = "UTF-8";
	private File bucketsDirectory;

	private static final Logger LOGGER = LoggerFactory.getLogger(OfflineS3ClientImpl.class);

	public OfflineS3ClientImpl(File bucketsDirectory) {
		this.bucketsDirectory = bucketsDirectory;
	}

	@Override
	public ObjectListing listObjects(String bucketName, String prefix) throws AmazonClientException, AmazonServiceException {
		ObjectListing listing = new ObjectListing();
		File bucketFile = getFile(bucketName);
		final String fileNamePrefix = getFilename(prefix);

		String[] list = bucketFile.list(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.startsWith(fileNamePrefix);
			}
		});

		List<S3ObjectSummary> objectSummaries = listing.getObjectSummaries();
		
		if (list != null) {
			for (String filename : list) {
				String key = getKey(filename);
				S3ObjectSummary summary = new S3ObjectSummary();
				summary.setKey(key);
				summary.setBucketName(bucketName);
				objectSummaries.add(summary);
			}
		}
		listing.setBucketName(bucketName);
		
		//Mac appears to return these objects in a sorted list, Ubuntu does not.   
		//Sorting programatically for now until we can get this nailed down.
		Collections.sort(objectSummaries, new S3ObjectSummaryComparator());
		
		return listing;
	}

	@Override
	/**
	 * Only bucketName and prefix is used from the ListObjectsRequest.
	 */
	public ObjectListing listObjects(ListObjectsRequest listObjectsRequest) throws AmazonClientException, AmazonServiceException {
		return listObjects(listObjectsRequest.getBucketName(), listObjectsRequest.getPrefix());
	}

	@Override
	public S3Object getObject(String bucketName, String key) {
		File file = getFile(bucketName, key);
		return new OfflineS3Object(bucketName, key, file);
	}

	@Override
	public PutObjectResult putObject(String bucketName, String key, File file) throws AmazonClientException, AmazonServiceException {
		return putObject(bucketName, key, getInputStream(file), null);
	}

	@Override
	public PutObjectResult putObject(String bucketName, String key, InputStream inputStream, ObjectMetadata metadata) throws AmazonClientException, AmazonServiceException {
		File outFile = getFile(bucketName, key);
		if (inputStream != null) {
			try {
				Files.copy(inputStream, outFile.toPath());
			} catch (IOException e) {
				throw new AmazonServiceException(String.format("Failed to store object, bucket:%s, objectKey:%s", bucketName, key), e);
			} finally {
				try {
					inputStream.close();
				} catch (IOException e) {
					// Just log
					LOGGER.error("Failed to close stream.", e);
				}
			}
			return null;
		} else {
			throw new AmazonClientException("Failed to store object, no input given.");
		}
	}

	@Override
	public PutObjectResult putObject(PutObjectRequest putRequest) throws AmazonClientException, AmazonServiceException {
		String bucketName = putRequest.getBucketName();
		String key = putRequest.getKey();
		InputStream inputStream = putRequest.getInputStream();
		if (inputStream == null) {
			File inFile = putRequest.getFile();
			inputStream = getInputStream(inFile);
		}
		putObject(bucketName, key, inputStream, null);
		return new PutObjectResult();
	}

	private InputStream getInputStream(File inFile) {
		if (inFile != null && inFile.isFile()) {
			try {
				return new FileInputStream(inFile);
			} catch (FileNotFoundException e) {
				throw new AmazonClientException(String.format("File not found:%s", inFile.getAbsoluteFile()), e);
			}
		}
		return null;
	}

	@Override
	public void deleteObject(String bucketName, String key) throws AmazonClientException, AmazonServiceException {
		File file = getFile(bucketName, key);
		file.delete();
	}

	@Override
	public AccessControlList getBucketAcl(String bucketName) {
		return null;
	}

	private File getFile(String bucketName) {
		File bucketFile = new File(bucketsDirectory, bucketName);
		if (bucketFile.isDirectory()) {
			return bucketFile;
		} else {
			throw new AmazonServiceException("Bucket does not exist.");
		}
	}

	private File getFile(String bucketName, String key) {
		File bucketFile = getFile(bucketName);
		return new File(bucketFile, getFilename(key));
	}

	private String getFilename(String key) {
		try {
			String encode = URLEncoder.encode(key, UTF_8);
			//System.out.println("Offline S3 Client url-encoded filename: " + encode);
			return encode;
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException("Unsupported Encoding", e);
		}
	}

	private String getKey(String filename) {
		try {
			return URLDecoder.decode(filename, UTF_8);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException("Unsupported Encoding", e);
		}
	}
	
	public class S3ObjectSummaryComparator implements Comparator<S3ObjectSummary> {
		@Override
		public int compare(S3ObjectSummary o1, S3ObjectSummary o2) {
			return o1.getKey().compareTo(o2.getKey());
		}
	}

}

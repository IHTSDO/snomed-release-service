package org.ihtsdo.buildcloud.dao.s3;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.model.*;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Offers an offline version of S3 cloud storage for demos or working without a connection.
 * N.B. Metadata and ACL security are not implemented.
 */
public class OfflineS3ClientImpl implements S3Client {

	private File bucketsDirectory;

	private static final boolean REPLACE_SEPARATOR = !File.pathSeparator.equals("/");
	private static final Logger LOGGER = LoggerFactory.getLogger(OfflineS3ClientImpl.class);

	public OfflineS3ClientImpl(File bucketsDirectory) {
		this.bucketsDirectory = bucketsDirectory;
	}

	@Override
	public ObjectListing listObjects(String bucketName, String prefix) throws AmazonClientException, AmazonServiceException {
		ObjectListing listing = new ObjectListing();
		List<S3ObjectSummary> objectSummaries = listing.getObjectSummaries();

		String searchLocation = bucketName + File.separator + getPlatformDependantPath(prefix);
		File searchStartDir;
		
		try{
			searchStartDir = getBucket(searchLocation, false);
		} catch (Exception e) {
			LOGGER.warn("Failed to find files at {}", searchLocation, e);
			return listing;
		}
		
		Collection<File> list = FileUtils.listFiles(searchStartDir, null, true); //No filter files, yes search recursively
		
		if (list != null) {
			for (File file : list) {
				String key = getRelativePathAsKey(bucketName, file);
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
		File file = getFile(bucketName, key, false); //Don't create bucket
		return new OfflineS3Object(bucketName, key, file);
	}

	@Override
	public PutObjectResult putObject(String bucketName, String key, File file) throws AmazonClientException, AmazonServiceException {
		return putObject(bucketName, key, getInputStream(file), null);
	}

	@Override
	public PutObjectResult putObject(String bucketName, String key, InputStream inputStream, ObjectMetadata metadata) throws AmazonClientException, AmazonServiceException {
		File outFile = getFile(bucketName, key, true);  //Create the target bucket if required
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
		} else {
			throw new AmazonClientException("Failed to store object, no input given.");
		}
		return new PutObjectResult();
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
		return putObject(bucketName, key, inputStream, null);
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
	public CopyObjectResult copyObject(String sourceBucketName, String sourceKey, String destinationBucketName, String destinationKey) throws AmazonClientException, AmazonServiceException {
		S3Object object = getObject(sourceBucketName, sourceKey);
		putObject(destinationBucketName, destinationKey, object.getObjectContent(), null);
		return null;
	}

	@Override
	public void deleteObject(String bucketName, String key) throws AmazonClientException, AmazonServiceException {
		File file = getFile(bucketName, key, false);
		file.delete();
	}

	@Override
	public AccessControlList getBucketAcl(String bucketName) {
		return null;
	}

	private File getBucket(String bucketName, boolean createIfRequired) {
		File bucket = new File(bucketsDirectory, bucketName);
		
		//Is bucket there already, or do we need to create it?
		if (!bucket.isDirectory()) {
			//Attempt to create - will fail if file already exists at that location.
			if (createIfRequired) {
				boolean success = bucket.mkdirs();
				
				if (!success) {
					throw new AmazonServiceException("Could neither find nor create Bucket at: "  + bucketsDirectory + File.separator + bucketName);
				}
			} else {
				throw new AmazonServiceException("Could not find Bucket expected at: "  + bucketsDirectory + File.separator + bucketName);
			}
		}
		return bucket;
	}

	private File getFile(String bucketName, String key, boolean createIfRequired) {
		//Limitations on length of filename mean we have to use the slashed elements in the key as a directory path, unlike in the online implementation
		//split the last filename off to create the parent directory as required.
		key = getPlatformDependantPath(key);
		File relativeLocation = new File(bucketName + File.separator + key);
		File subBucket = getBucket(relativeLocation.getParent(), createIfRequired);
		File file = new File (subBucket.getAbsolutePath() + File.separator + relativeLocation.getName());
		return file;
	}
	
	/**
	 * 
	 * @param file
	 * @return The path relative to the bucket directory and bucket
	 */
	private String getRelativePathAsKey(String bucketName, File file) {
		String absolutePath = file.getAbsolutePath();
		int relativeStart =  bucketsDirectory.getAbsolutePath().length() + bucketName.length() + 2; //Take off the slash between bucketDirectory and final slash 
		String relativePath = absolutePath.substring(relativeStart);
		relativePath = getPlatformIndependentPath(relativePath);
		return relativePath;
	}

	private String getPlatformDependantPath(String path) {
		if (REPLACE_SEPARATOR) {
			path = path.replace('/', File.separatorChar);
		}
		return path;
	}

	private String getPlatformIndependentPath(String path) {
		if (REPLACE_SEPARATOR) {
			path = path.replace(File.separatorChar, '/');
		}
		return path;
	}

	public class S3ObjectSummaryComparator implements Comparator<S3ObjectSummary> {
		@Override
		public int compare(S3ObjectSummary o1, S3ObjectSummary o2) {
			return o1.getKey().compareTo(o2.getKey());
		}
	}

}

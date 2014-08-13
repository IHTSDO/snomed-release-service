package org.ihtsdo.buildcloud.dao.s3;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.model.*;
import org.ihtsdo.buildcloud.service.file.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Offers an offline version of S3 cloud storage for demos or working without a connection.
 * N.B. Metadata and ACL security are not implemented.
 */
public class OfflineS3ClientImpl implements S3Client, TestS3Client {

	private File bucketsDirectory;

	private static final boolean REPLACE_SEPARATOR = !File.pathSeparator.equals("/");
	private static final Logger LOGGER = LoggerFactory.getLogger(OfflineS3ClientImpl.class);

	public OfflineS3ClientImpl(File bucketsDirectory) {
		this.bucketsDirectory = bucketsDirectory;
	}

	public OfflineS3ClientImpl() throws IOException {
		this(Files.createTempDirectory(OfflineS3ClientImpl.class.getName() + "-mock-s3").toFile());
	}

	@Override
	public void createBucket(String bucketName) {
		getBucket(bucketName);
	}

	@Override
	public ObjectListing listObjects(String bucketName, String prefix) throws AmazonClientException, AmazonServiceException {
		ObjectListing listing = new ObjectListing();
		List<S3ObjectSummary> objectSummaries = listing.getObjectSummaries();

		String searchLocation = getPlatformDependantPath(prefix);
		File searchStartDir;

		File bucket = getBucket(bucketName);
		searchStartDir = new File(bucket, searchLocation);
		if (searchStartDir.isDirectory()) {
			Collection<File> list = org.apache.commons.io.FileUtils.listFiles(searchStartDir, null, true); //No filter files, yes search recursively
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
		}
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
		if (file.isFile()) {
			return new OfflineS3Object(bucketName, key, file);
		} else {
			AmazonS3Exception amazonS3Exception = new AmazonS3Exception("Object does not exist.");
			amazonS3Exception.setStatusCode(404);
			throw amazonS3Exception;
		}
	}

	@Override
	public ObjectMetadata getObjectMetadata(String bucketName, String key) {
		getObject(bucketName, key);
		return new ObjectMetadata();
	}

	@Override
	public PutObjectResult putObject(String bucketName, String key, File file) throws AmazonClientException, AmazonServiceException {
		return putObject(bucketName, key, getInputStream(file), null);
	}

	@Override
	public PutObjectResult putObject(String bucketName, String key, InputStream inputStream, ObjectMetadata metadata) throws AmazonClientException, AmazonServiceException {
		File outFile = getFile(bucketName, key);

		// Create the target directory
		outFile.getParentFile().mkdirs();
		LOGGER.info("Offline bucket location {}", outFile.getAbsolutePath());
		//For ease of testing, if we're writing the final results (eg a zip file) we'll output the full path to STDOUT
		String outputFilePath = outFile.getAbsolutePath();
		if (FileUtils.isZip(outputFilePath)) {
			LOGGER.info("Writing out local results file to {}", outputFilePath);
		}

		if (inputStream != null) {
			try {
				//As per the online implmentation, if the file is already there we will overwrite it.
				Files.copy(inputStream, outFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
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

		PutObjectResult result = new PutObjectResult();
		//For the offline implmentation we'll just copy the incoming MD5 and say we received the same thing
		if (metadata != null) {
			result.setContentMd5(metadata.getContentMD5());
		}

		return result;
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
		return putObject(bucketName, key, inputStream, putRequest.getMetadata());
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
		File file = getFile(bucketName, key);

		//Are we deleting a file or a directory?
		if (file.isDirectory()) {
			try {
				LOGGER.warn("Deleting directory {}.", file.getAbsoluteFile());
				org.apache.commons.io.FileUtils.deleteDirectory(file);
			} catch (IOException e) {
				throw new AmazonServiceException("Failed to delete directory: " + file.getAbsolutePath(), e);
			}
		} else if (file.isFile()) {
			LOGGER.debug("Deleting file {}.", file.getAbsoluteFile());
			boolean deletedOK = file.delete();
			if (!deletedOK) {
				throw new AmazonServiceException("Failed to delete " + file.getAbsoluteFile());
			}
		} else {
			//Does it, in fact, not exist already? No foul if so
			if (!file.exists()) {
				throw new AmazonServiceException("Attempted to delete entity, but it does not exist: " + file.getAbsoluteFile());
			} else {
				throw new AmazonServiceException("Encountered unexpected thing: " + file.getAbsolutePath());
			}
		}
	}

	@Override
	public AccessControlList getBucketAcl(String bucketName) {
		return null;
	}

	@Override
	/**
	 * Part of the TestS3Client interface.
	 * For clearing down before and after testing.
	 */
	public void freshBucketStore() throws IOException {
		bucketsDirectory = Files.createTempDirectory(getClass().getName() + "-temp-S3").toFile();
	}

	private File getBucket(String bucketName) {
		File bucket = new File(bucketsDirectory, bucketName);

		//Is bucket there already, or do we need to create it?
		if (!bucket.isDirectory()) {
			//Attempt to create - will fail if file already exists at that location.
			boolean success = bucket.mkdirs();
			if (!success) {
				throw new AmazonServiceException("Could neither find nor create Bucket at: " + bucketsDirectory + File.separator + bucketName);
			}
		}
		return bucket;
	}

	private File getFile(String bucketName, String key) {
		//Limitations on length of filename mean we have to use the slashed elements in the key as a directory path, unlike in the online implementation
		File bucket = getBucket(bucketName);
		key = getPlatformDependantPath(key);
		return new File(bucket, key);
	}

	/**
	 * @param file
	 * @return The path relative to the bucket directory and bucket
	 */
	private String getRelativePathAsKey(String bucketName, File file) {
		String absolutePath = file.getAbsolutePath();
		int relativeStart = bucketsDirectory.getAbsolutePath().length() + bucketName.length() + 2; //Take off the slash between bucketDirectory and final slash
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

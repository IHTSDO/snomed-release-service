package org.ihtsdo.buildcloud.dao.helper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.codec.DecoderException;
import org.ihtsdo.buildcloud.dao.s3.S3Client;
import org.ihtsdo.buildcloud.service.file.ArchiveEntry;
import org.ihtsdo.buildcloud.service.file.FileNameTransformation;
import org.ihtsdo.buildcloud.service.file.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.util.StreamUtils;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;

@Repository
public class FileHelper {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(FileHelper.class);

	private S3Client s3Client;

	public void setS3Client(S3Client s3Client) {
		this.s3Client = s3Client;
	}

	private final S3ClientHelper s3ClientHelper;

	private final String bucketName;
	
	public FileHelper (String bucketName, S3Client s3Client, S3ClientHelper s3ClientHelper) {
		this.bucketName = bucketName;
		this.s3Client = s3Client;
		this.s3ClientHelper = s3ClientHelper;
	}

	public void putFile(InputStream fileStream, long fileSize, String targetFilePath) {
		S3PutRequestBuilder putRequest = s3ClientHelper.newPutRequest(bucketName, targetFilePath, fileStream).length(fileSize).useBucketAcl();
		s3Client.putObject(putRequest);
	}

	/**
	 * This method causes a warning when using S3 because we don't know the file length up front.
	 * TODO: Investigate multipart upload to avoid the S3 library buffering the whole file.
	 */
	public void putFile(InputStream fileStream, String targetFilePath) {
		S3PutRequestBuilder putRequest = s3ClientHelper.newPutRequest(bucketName, targetFilePath, fileStream).useBucketAcl();
		s3Client.putObject(putRequest);
	}
	
	public String putFile(File file, String targetFilePath) throws NoSuchAlgorithmException, IOException, DecoderException {
		return putFile(file, targetFilePath, false);
	}
	

	public String putFile(File file, String targetFilePath, boolean calcMD5) throws NoSuchAlgorithmException, IOException, DecoderException {

		InputStream is = new FileInputStream (file);
		S3PutRequestBuilder putRequest = s3ClientHelper.newPutRequest(bucketName, targetFilePath, is).length(file.length()).useBucketAcl();
		if (calcMD5){
			String md5 = FileUtils.calculateMD5(file);
			putRequest.withMD5(md5);
		}		
		PutObjectResult putResult = s3Client.putObject(putRequest);
		String md5Received = (putResult == null ? null : putResult.getContentMd5());
		LOGGER.debug ("S3Client put request returned MD5: " + md5Received);
		
		if (calcMD5){
			//Also upload the hex encoded (ie normal) md5 digest in a file
			String md5TargetPath = targetFilePath + ".md5";
			File md5File = FileUtils.createMD5File(file);
			InputStream isMD5 = new FileInputStream (md5File);
			S3PutRequestBuilder md5PutRequest = s3ClientHelper.newPutRequest(bucketName, md5TargetPath, isMD5).length(md5File.length()).useBucketAcl();
			s3Client.putObject(md5PutRequest);
		}
		
		return md5Received;
	}

	public InputStream getFileStream(String filePath) {

		try {
			S3Object s3Object = s3Client.getObject(bucketName, filePath);
			if (s3Object != null) {
				return s3Object.getObjectContent();
			}
		} catch (AmazonS3Exception e) {
			if (404 != e.getStatusCode()) {
				throw e;
			}
		}
		return null;
	}
	
	/**
	 * 
	 * @param targetFileName
	 * @param previousPublishedPackagePath
	 * @param fnt - A FileTransformationObject that will strip out the releaseDate in the filenames to allow matching.
	 * @return an inputStream positioned at the correct point in the archive.  Make sure you close it!
	 * @throws IOException
	 */
	public ArchiveEntry getArchiveEntry(String targetFileName, String previousPublishedPackagePath, FileNameTransformation fnt) throws IOException {
		
		ArchiveEntry result = null;
		//Get hold of the Archive Input Stream
		InputStream archiveInputStream = getFileStream(previousPublishedPackagePath);
		
		if (archiveInputStream == null) {
			throw new FileNotFoundException ("Failed to find published package: " + previousPublishedPackagePath);
		}
		
		ZipInputStream zis = new ZipInputStream (archiveInputStream);
		
		//Now what's our target filename template (ie with the date stripped off)
		String targetNameTemplate = fnt.transformFilename(targetFileName);
		
		//Now lets iterate through that zip archive and see if we can find it's partner
		ZipEntry zEntry;
		while ( (zEntry = zis.getNextEntry()) != null) {
			//The Zip entry will contain the whole path to the file.  We'll just check the end string matches...should be ok.
			if (!zEntry.isDirectory() && fnt.transformFilename(zEntry.getName()).endsWith(targetNameTemplate)) {
				//I have to expose this inputStream.  The alternative is to copy N bytes into memory!
				Path p = Paths.get(zEntry.getName());
				result = new ArchiveEntry(p.getFileName().toString(), zis);
			}
		    }
		return result;
	}

	public List<String> listFiles(String directoryPath) {
		ArrayList<String> files = new ArrayList<>();
		try {
			ObjectListing objectListing = s3Client.listObjects(bucketName, directoryPath);
			for (S3ObjectSummary summary : objectListing.getObjectSummaries()) {
				files.add(summary.getKey().substring(directoryPath.length()));
			}
		} catch (AmazonServiceException e) {
			//Trying to list files in a directory that doesn't exist isn't a problem, we'll just return an empty array
			LOGGER.debug("Probable attempt to get listing on non-existent directory: {} error {}", directoryPath, e.getLocalizedMessage());
		}
		return files;
	}

	// TODO: User logging against file actions?
	public void deleteFile(String filePath) {
		s3Client.deleteObject(bucketName, filePath);
	}

	public void copyFile(String sourcePath, String targetPath) {
		LOGGER.debug("Copy file '{}' to '{}'", sourcePath, targetPath);
		s3Client.copyObject(bucketName, sourcePath, bucketName, targetPath);
	}

	public void putFiles(File sourceDirectory, StringBuffer targetDirectoryPath) {
		File[] files = sourceDirectory.listFiles();
		for (File file : files) {
			StringBuffer filePath = new StringBuffer(targetDirectoryPath).append(file.getName());
			if (file.isFile()) {
				s3Client.putObject(bucketName, filePath.toString(), file);
			} else if (file.isDirectory()) {
				filePath.append(File.separator);
				putFiles(file, filePath);
			}
		}
	}
	
	public void streamS3FilesAsZip(String buildScriptsPath, OutputStream outputStream) throws IOException {
		LOGGER.debug("Serving zip of files in {}", buildScriptsPath);
		ObjectListing objectListing = s3Client.listObjects(bucketName, buildScriptsPath);
		int buildScriptsPathLength = buildScriptsPath.length();

		ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream);
		for (S3ObjectSummary summary : objectListing.getObjectSummaries()) {
			String key = summary.getKey();
			String relativePath = key.substring(buildScriptsPathLength);
			LOGGER.debug("Zip entry. S3Key {}, Entry path {}", key, relativePath);
			zipOutputStream.putNextEntry(new ZipEntry(relativePath));
			S3Object object = s3Client.getObject(bucketName, key);
			try (InputStream objectContent = object.getObjectContent()) {
				StreamUtils.copy(objectContent, zipOutputStream);
			}
		}
		zipOutputStream.close();
	}
}

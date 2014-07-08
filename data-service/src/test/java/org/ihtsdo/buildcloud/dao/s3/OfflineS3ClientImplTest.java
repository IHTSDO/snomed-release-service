package org.ihtsdo.buildcloud.dao.s3;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.model.*;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StreamUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class OfflineS3ClientImplTest {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(OfflineS3ClientImplTest.class);

	public static final String TEST_FILE_TXT = "testFile.txt";
	private S3Client s3Client;
	private List<InputStream> streamsToClose;

	private static final String TEST_BUCKET = "test-bucket";

	@Before
	public void setup() throws IOException {
		streamsToClose = new ArrayList<>();
		Path tempDirectory = Files.createTempDirectory(getClass().getName());
		Assert.assertTrue(new File(tempDirectory.toFile(), TEST_BUCKET).mkdirs());
		s3Client = new OfflineS3ClientImpl(tempDirectory.toFile());
		Assert.assertNotNull(getTestFileStream());
	}

	@Test
	public void testListObjects() {
		String buildDir = "builds/123/";
		Assert.assertEquals(0, s3Client.listObjects(TEST_BUCKET, buildDir).getObjectSummaries().size());

		s3Client.putObject(TEST_BUCKET, buildDir + "execA/file1.txt", getTestFileStream(), null);
		s3Client.putObject(TEST_BUCKET, buildDir + "exec1/file1.txt", getTestFileStream(), null);
		s3Client.putObject(TEST_BUCKET, buildDir + "exec1/file2.txt", getTestFileStream(), null);
		s3Client.putObject(TEST_BUCKET, buildDir + "execZ/file1.txt", getTestFileStream(), null);
		s3Client.putObject(TEST_BUCKET, buildDir + "exec2/file2.txt", getTestFileStream(), null);
		s3Client.putObject(TEST_BUCKET, buildDir + "exec2/file1.txt", getTestFileStream(), null);

		List<S3ObjectSummary> objectSummaries = s3Client.listObjects(TEST_BUCKET, buildDir).getObjectSummaries();

		Assert.assertEquals(6, objectSummaries.size());
		Assert.assertEquals("builds/123/exec1/file1.txt", objectSummaries.get(0).getKey());
		Assert.assertEquals("builds/123/exec1/file2.txt", objectSummaries.get(1).getKey());
		Assert.assertEquals("builds/123/exec2/file1.txt", objectSummaries.get(2).getKey());
		Assert.assertEquals("builds/123/exec2/file2.txt", objectSummaries.get(3).getKey());
		Assert.assertEquals("builds/123/execA/file1.txt", objectSummaries.get(4).getKey());
		Assert.assertEquals("builds/123/execZ/file1.txt", objectSummaries.get(5).getKey());
	}

	@Test
	public void testListObjectsUsingListObjectsRequest() {
		String buildDir = "builds/123/";
		Assert.assertEquals(0, s3Client.listObjects(TEST_BUCKET, buildDir).getObjectSummaries().size());

		s3Client.putObject(TEST_BUCKET, buildDir + "execA/file1.txt", getTestFileStream(), null);
		s3Client.putObject(TEST_BUCKET, buildDir + "exec1/file1.txt", getTestFileStream(), null);
		s3Client.putObject(TEST_BUCKET, buildDir + "exec1/file2.txt", getTestFileStream(), null);
		s3Client.putObject(TEST_BUCKET, buildDir + "execZ/file1.txt", getTestFileStream(), null);
		s3Client.putObject(TEST_BUCKET, buildDir + "exec2/file2.txt", getTestFileStream(), null);
		s3Client.putObject(TEST_BUCKET, buildDir + "exec2/file1.txt", getTestFileStream(), null);

		List<S3ObjectSummary> objectSummaries = s3Client.listObjects(new ListObjectsRequest(TEST_BUCKET, buildDir, null, null, null)).getObjectSummaries();

		Assert.assertEquals(6, objectSummaries.size());
		Assert.assertEquals("builds/123/exec1/file1.txt", objectSummaries.get(0).getKey());
		Assert.assertEquals("builds/123/exec1/file2.txt", objectSummaries.get(1).getKey());
		Assert.assertEquals("builds/123/exec2/file1.txt", objectSummaries.get(2).getKey());
		Assert.assertEquals("builds/123/exec2/file2.txt", objectSummaries.get(3).getKey());
		Assert.assertEquals("builds/123/execA/file1.txt", objectSummaries.get(4).getKey());
		Assert.assertEquals("builds/123/execZ/file1.txt", objectSummaries.get(5).getKey());
	}

	@Test
	public void testPutObjectGetObject() throws IOException {
		String buildDir = "builds/123/";
		String key = buildDir + "execA/file1.txt";

		s3Client.putObject(TEST_BUCKET, key, getTestFileStream(), null);

		List<S3ObjectSummary> objectSummaries = s3Client.listObjects(TEST_BUCKET, "").getObjectSummaries();
		Assert.assertEquals(1, objectSummaries.size());
		Assert.assertEquals("builds/123/execA/file1.txt", objectSummaries.get(0).getKey());

		S3Object s3Object = s3Client.getObject(TEST_BUCKET, key);
		Assert.assertEquals(TEST_BUCKET, s3Object.getBucketName());
		Assert.assertEquals(key, s3Object.getKey());
		S3ObjectInputStream objectContent = s3Object.getObjectContent();
		Assert.assertNotNull(objectContent);
		Assert.assertTrue(objectContent.available() > 0);
		String content = StreamUtils.copyToString(objectContent, Charset.defaultCharset());
		Assert.assertEquals("Some content".trim(), content.trim());
	}

	@Test
	public void testPutObjectByFile() throws IOException {
		String buildDir = "builds/123/";
		String key = buildDir + "execA/file1.txt";

		s3Client.putObject(TEST_BUCKET, key, getTestFile());

		S3Object s3Object = s3Client.getObject(TEST_BUCKET, key);
		Assert.assertEquals(TEST_BUCKET, s3Object.getBucketName());
		Assert.assertEquals(key, s3Object.getKey());
		S3ObjectInputStream objectContent = s3Object.getObjectContent();
		Assert.assertNotNull(objectContent);
		Assert.assertTrue(objectContent.available() > 0);
		String content = StreamUtils.copyToString(objectContent, Charset.defaultCharset());
		Assert.assertEquals("Some content".trim(), content.trim());
	}

	@Test
	public void testCopyObject() throws IOException {
		String buildDir = "builds/123/";
		String key = buildDir + "execA/file1.txt";

		// put first file
		s3Client.putObject(TEST_BUCKET, key, getTestFile());

		// get first file
		S3Object s3Object = s3Client.getObject(TEST_BUCKET, key);

		// test first file
		Assert.assertEquals(TEST_BUCKET, s3Object.getBucketName());
		Assert.assertEquals(key, s3Object.getKey());
		S3ObjectInputStream objectContent = s3Object.getObjectContent();
		Assert.assertNotNull(objectContent);
		Assert.assertTrue(objectContent.available() > 0);
		String content = StreamUtils.copyToString(objectContent, Charset.defaultCharset());
		Assert.assertEquals("Some content".trim(), content.trim());

		// copy file
		String destinationKey = key + "2";
		s3Client.copyObject(TEST_BUCKET, key, TEST_BUCKET, destinationKey);

		// get copy
		S3Object s3Object2 = s3Client.getObject(TEST_BUCKET, destinationKey);

		// test copy
		Assert.assertEquals(TEST_BUCKET, s3Object2.getBucketName());
		Assert.assertEquals(destinationKey, s3Object2.getKey());
		S3ObjectInputStream object2Content = s3Object2.getObjectContent();
		Assert.assertNotNull(object2Content);
		Assert.assertTrue(object2Content.available() > 0);
		String content2 = StreamUtils.copyToString(object2Content, Charset.defaultCharset());
		Assert.assertEquals("Some content".trim(), content2.trim());
	}

	@Test
	public void testPutObjectByPutRequest() throws IOException {
		String buildDir = "builds/123/";
		String key = buildDir + "execA/file1.txt";

		ObjectMetadata metadata = new ObjectMetadata();
		PutObjectRequest putRequest = new PutObjectRequest(TEST_BUCKET, key, getTestFileStream(), metadata);
		metadata.setContentLength(13);

		s3Client.putObject(putRequest);

		S3Object s3Object = s3Client.getObject(TEST_BUCKET, key);
		Assert.assertEquals(TEST_BUCKET, s3Object.getBucketName());
		Assert.assertEquals(key, s3Object.getKey());
		S3ObjectInputStream objectContent = s3Object.getObjectContent();
		Assert.assertNotNull(objectContent);
		Assert.assertTrue(objectContent.available() > 0);
		String content = StreamUtils.copyToString(objectContent, Charset.defaultCharset());
		Assert.assertEquals("Some content".trim(), content.trim());
	}

	@Test
	public void testPutObjectNoInput() {
		try {
			s3Client.putObject(TEST_BUCKET, "123", null, null);
			Assert.fail("Should have thrown Exception");
		} catch (AmazonClientException e) {
			// pass
		}
	}

	@Test
	public void testDeleteNonExistentObject() {
		// now throws an AmazonServiceExecption when delete attempt fails.
		boolean exceptionDetected = false;
		try {
			s3Client.deleteObject(TEST_BUCKET, "file-does-not-exist.txt");
		} catch (AmazonServiceException ase) {
			exceptionDetected = true;
		}
		
		Assert.assertTrue("Expected to see exception thrown when attempting to delete non-existant object", exceptionDetected);
	}

	@Test
	public void testDeleteObject() throws IOException {
		String buildDir = "builds/123/";
		String key = buildDir + "execA/file1.txt";
		s3Client.putObject(TEST_BUCKET, key, getTestFileStream(), null);
		List<S3ObjectSummary> objectSummaries = s3Client.listObjects(TEST_BUCKET, "").getObjectSummaries();
		Assert.assertEquals(1, objectSummaries.size());
		Assert.assertEquals("builds/123/execA/file1.txt", objectSummaries.get(0).getKey());

		s3Client.deleteObject(TEST_BUCKET, key);

		Assert.assertEquals(0, s3Client.listObjects(TEST_BUCKET, "").getObjectSummaries().size());
	}

	@After
	public void tearDown() {
		for (InputStream inputStream : streamsToClose) {
			try {
				inputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private InputStream getTestFileStream() {
		InputStream stream = getClass().getResourceAsStream(TEST_FILE_TXT);
		streamsToClose.add(stream);
		return stream;
	}

	private File getTestFile() {
		File testFile = new File(getClass().getResource(TEST_FILE_TXT).getFile());
		if (!testFile.exists()) {
			LOGGER.warn("Failed to recover test resource from: " + getClass().getResource(".").getPath());
		}
		return testFile;
	}

}

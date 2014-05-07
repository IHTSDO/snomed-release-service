package org.ihtsdo.buildcloud.dao.s3;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.model.*;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
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
		Assert.assertEquals(13, objectContent.available());
		String content = StreamUtils.copyToString(objectContent, Charset.defaultCharset());
		Assert.assertEquals("Some content\n", content);
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
		Assert.assertEquals(13, objectContent.available());
		String content = StreamUtils.copyToString(objectContent, Charset.defaultCharset());
		Assert.assertEquals("Some content\n", content);
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
		Assert.assertEquals(13, objectContent.available());
		String content = StreamUtils.copyToString(objectContent, Charset.defaultCharset());
		Assert.assertEquals("Some content\n", content);
	}

	@Test
	public void testPutObjectKeyCharacters() throws IOException {
		String buildDir = "builds/123/";
		String key = buildDir + "execA/file1_01:01:01.txt";

		s3Client.putObject(TEST_BUCKET, key, getTestFileStream(), null);

		List<S3ObjectSummary> objectSummaries = s3Client.listObjects(TEST_BUCKET, "").getObjectSummaries();
		Assert.assertEquals(1, objectSummaries.size());
		Assert.assertEquals("builds/123/execA/file1_01:01:01.txt", objectSummaries.get(0).getKey());
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
		// should not throw an error
		s3Client.deleteObject(TEST_BUCKET, "file-does-not-exist.txt");
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
		LOGGER.warn("Attempting to recover test resource from: " + getClass().getResource(".").getPath());
		return new File(getClass().getResource(TEST_FILE_TXT).getFile());
	}

}

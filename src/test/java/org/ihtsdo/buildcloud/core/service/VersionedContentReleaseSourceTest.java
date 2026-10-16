package org.ihtsdo.buildcloud.core.service;

import org.apache.commons.io.FileUtils;
import org.ihtsdo.otf.dao.s3.OfflineS3ClientImpl;
import org.ihtsdo.otf.dao.s3.helper.FileHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VersionedContentReleaseSourceTest {

	private static final String BUCKET = "local.snomed.international.bucket";
	private static final String VERSIONED_CONTENT_PATH = "authoring/version-content/";
	private static final String RELEASE_FILENAME = "SnomedCT_Release_INT_20140131.zip";
	private static final String PREFIXED_RELEASE_FILENAME = "SnomedCT_Release_INT_20140731.zip";
	private static final String CONCEPT_FILE = "sct2_Concept_Snapshot_INT_20140131.txt";

	private Path storageDirectory;
	private VersionedContentReleaseSource releaseSource;
	private File extractedDirectory;

	@BeforeEach
	void setup() throws Exception {
		storageDirectory = Files.createTempDirectory("versioned-content");
		OfflineS3ClientImpl s3Client = new OfflineS3ClientImpl(storageDirectory.toFile());
		File releaseZip = zipFile(CONCEPT_FILE, "id\tactive\n");
		File prefixedZip = zipFile(CONCEPT_FILE, "id\tactive\n");
		s3Client.putObject(BUCKET, VERSIONED_CONTENT_PATH + RELEASE_FILENAME, releaseZip);
		s3Client.putObject(BUCKET, VERSIONED_CONTENT_PATH + "DEV_" + PREFIXED_RELEASE_FILENAME, prefixedZip);
		releaseSource = new VersionedContentReleaseSource(new FileHelper(BUCKET, s3Client), VERSIONED_CONTENT_PATH);
	}

	@AfterEach
	void cleanup() throws Exception {
		if (extractedDirectory != null) {
			FileUtils.deleteDirectory(extractedDirectory);
		}
		FileUtils.deleteDirectory(storageDirectory.toFile());
	}

	@Test
	void existsWhenPackageIsStoredByFilename() {
		assertTrue(releaseSource.exists(RELEASE_FILENAME));
	}

	@Test
	void existsWhenPackageIsStoredWithEnvironmentPrefix() {
		assertTrue(releaseSource.exists(PREFIXED_RELEASE_FILENAME));
	}

	@Test
	void missingPackageDoesNotExist() {
		assertFalse(releaseSource.exists("SnomedCT_Release_INT_20190131.zip"));
	}

	@Test
	void readsAndExtractsPackageFromVersionedContent() throws Exception {
		extractedDirectory = releaseSource.readExtractedReleaseOrNull(RELEASE_FILENAME);

		assertNotNull(extractedDirectory);
		File conceptFile = new File(extractedDirectory, CONCEPT_FILE);
		assertTrue(conceptFile.isFile());
		assertEquals("id\tactive\n", Files.readString(conceptFile.toPath(), StandardCharsets.UTF_8));
	}

	@Test
	void returnsNullWhenPackageIsMissing() {
		assertNull(releaseSource.readExtractedReleaseOrNull("SnomedCT_Release_INT_20190131.zip"));
	}

	private static File zipFile(String entryName, String contents) throws Exception {
		File zipFile = Files.createTempFile("release-", ".zip").toFile();
		zipFile.deleteOnExit();
		try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(zipFile))) {
			zipOutputStream.putNextEntry(new ZipEntry(entryName));
			zipOutputStream.write(contents.getBytes(StandardCharsets.UTF_8));
			zipOutputStream.closeEntry();
		}
		return zipFile;
	}
}

package org.ihtsdo.buildcloud.service.srs;

import com.google.common.io.Files;
import org.apache.commons.io.FileUtils;
import org.ihtsdo.otf.rest.exception.ProcessWorkflowException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;

public class SRSRestClientHelperTest {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private static String TEST_DATE = "20150318";
	private static String ALTERNATIVE_DATE = "20990101";
	private static String TEST_ARCHIVE = "test_archive_20150318.zip";
	private static String TEST_FILE = "rel2_Concept_Delta_INT_20150318.txt";
	File testArchive = null;

	private SRSFileDAO srsFileDAO;

	@Before
	public void setUp() throws Exception {
		URL testArchiveURL = getClass().getResource(TEST_ARCHIVE);
		testArchive = new File(testArchiveURL.getFile());
		if (!testArchive.exists()) {
			throw new Exception("Unable to load test resource: " + TEST_ARCHIVE);
		}

		srsFileDAO = new SRSFileDAO(null, false);
	}

	@Test
	public void testReleaseDateRecovery() throws ProcessWorkflowException {
		String testFileName = "der2_cRefset_DescriptionInactivationIndicatorReferenceSetDelta_INT_20150318.txt";
		String actualDate = srsFileDAO.findDateInString(testFileName, false);
		Assert.assertEquals(TEST_DATE, actualDate);
	}

	@Test
	public void testRecoverDate() throws Exception {
		String releaseDate = srsFileDAO.recoverReleaseDate(testArchive);
		Assert.assertEquals(TEST_DATE, releaseDate);
	}

	@Test
	public void testPrepareFiles() throws ProcessWorkflowException, IOException {
		File location = srsFileDAO.readyInputFiles(testArchive, "international", TEST_DATE, false);
		logger.debug("Test files made ready for SRS input at {}", location.getAbsolutePath());
		logger.debug("Tidying up folder at {}", location.getAbsolutePath());
		FileUtils.deleteDirectory(location);
	}

	@Test
	public void testReplaceInFiles() throws ProcessWorkflowException, IOException {
		URL testFileURL = getClass().getResource(TEST_FILE);
		File testFile = new File(testFileURL.getFile());

		// Create a temp directory and copy the file in there for processing
		File tempDir = Files.createTempDir();
		File copiedFile = new File(tempDir, testFile.getName());
		FileUtils.copyFile(testFile, copiedFile);

		srsFileDAO.replaceInFiles(tempDir, SRSFileDAO.UNKNOWN_EFFECTIVE_DATE, ALTERNATIVE_DATE,
				SRSFileDAO.EFFECTIVE_DATE_COLUMN);

		// Check the new file now contains our test date and does not contain the unknown effective date
		String testFileContent = FileUtils.readFileToString(copiedFile);
		Assert.assertTrue("Relplaced file should contain replaced date", testFileContent.contains(ALTERNATIVE_DATE));
		Assert.assertFalse("Relplaced file should not contain unknown effective date",
				testFileContent.contains(SRSFileDAO.UNKNOWN_EFFECTIVE_DATE));

	}


}

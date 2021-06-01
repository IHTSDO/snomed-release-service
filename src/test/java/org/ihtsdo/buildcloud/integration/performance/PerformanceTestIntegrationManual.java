package org.ihtsdo.buildcloud.integration.performance;

import org.ihtsdo.buildcloud.rest.controller.AbstractControllerTest;
import org.ihtsdo.buildcloud.rest.controller.helper.IntegrationTestHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class PerformanceTestIntegrationManual extends AbstractControllerTest {

	private static final String INTERNATIONAL_RELEASE = "SnomedCT_Release_INT_";
	public static final Logger LOGGER = LoggerFactory.getLogger(PerformanceTestIntegrationManual.class);

	private IntegrationTestHelper integrationTestHelper;
	private long lastMemoryInMb;
	private List<String> memoryRecordings;
	private long lastSeconds;

	@Override
	@Before
	public void setup() throws Exception {
		super.setup();
		integrationTestHelper = new IntegrationTestHelper(mockMvc, "PerformanceTest");
		memoryRecordings = new ArrayList<>();
	}

	/**
	 * You can put some load on the process by replacing the cut down rel2_cRefset_LanguageDelta-en_INT_20140228.txt with the real
	 * 300MB version from the international release.
	 */
	@Test
	public void testMultipleReleases() throws Exception {

		integrationTestHelper.createTestProductStructure();

		memoryRecordings.add(getStats("Start mem used"));

		// Perform first time release
		String effectiveDate = "20140228";
		integrationTestHelper.setFirstTimeRelease(true);
		integrationTestHelper.setEffectiveTime(effectiveDate);
		integrationTestHelper.setReadmeHeader("This is the readme for the first release © 2002-{readmeEndDate}.\\nTable of contents:\\n");
		integrationTestHelper.setReadmeEndDate("2014");
		uploadInputFiles(effectiveDate);

		memoryRecordings.add(getStats("After upload mem used"));

		String buildURL1 = integrationTestHelper.createBuild();

		memoryRecordings.add(getStats("After create exec mem used"));


		integrationTestHelper.triggerBuild(buildURL1);

		memoryRecordings.add(getStats("After trigger exec mem used"));
		Runtime.getRuntime().gc();
		memoryRecordings.add(getStats("After trigger exec mem used after GC"));

		integrationTestHelper.publishOutput(buildURL1);

		memoryRecordings.add(getStats("After publish mem used"));

		verfiyResults(effectiveDate, buildURL1);

		memoryRecordings.add(getStats("After verify mem used"));
	}

	@After
	public void tearDown() {

		LOGGER.info("MemoryRecordings:");
		for (String memoryRecording : memoryRecordings) {
			LOGGER.info("\t" + memoryRecording);
		}

		super.tearDown();
	}

	private void uploadInputFiles(String releaseDate) throws Exception {
		integrationTestHelper.uploadManifest("core_manifest_"+releaseDate+".xml", getClass());
		integrationTestHelper.uploadDeltaInputFile("rel2_cRefset_LanguageDelta-en_INT_" + releaseDate + ".txt", getClass());
	}

	private void verfiyResults(String releaseDate, String buildURL1) throws Exception {
		// Assert first release output expectations
		String expectedZipFilename = "SnomedCT_Release_INT_"+releaseDate+".zip";
		String expectedZipEntries = createExpectedZipEntries(releaseDate);
		integrationTestHelper.testZipNameAndEntryNames(buildURL1, expectedZipFilename, expectedZipEntries, getClass());
	}

	private String createExpectedZipEntries(String effectiveTime) {
		return INTERNATIONAL_RELEASE + effectiveTime + "/\n" +
		INTERNATIONAL_RELEASE + effectiveTime + "/Readme_" + effectiveTime + ".txt\n" +
		INTERNATIONAL_RELEASE + effectiveTime + "/RF2Release/\n" +
		INTERNATIONAL_RELEASE + effectiveTime + "/RF2Release/Full/\n" +
		INTERNATIONAL_RELEASE + effectiveTime + "/RF2Release/Full/Refset/\n" +
		INTERNATIONAL_RELEASE + effectiveTime + "/RF2Release/Full/Refset/Language/\n" +
		INTERNATIONAL_RELEASE + effectiveTime + "/RF2Release/Full/Refset/Language/der2_cRefset_LanguageFull-en_INT_" + effectiveTime + ".txt\n" +
		INTERNATIONAL_RELEASE + effectiveTime + "/RF2Release/Snapshot/\n" +
		INTERNATIONAL_RELEASE + effectiveTime + "/RF2Release/Snapshot/Refset/\n" +
		INTERNATIONAL_RELEASE + effectiveTime + "/RF2Release/Snapshot/Refset/Language/\n" +
		INTERNATIONAL_RELEASE + effectiveTime + "/RF2Release/Snapshot/Refset/Language/der2_cRefset_LanguageSnapshot-en_INT_"+ effectiveTime +".txt\n" +
		INTERNATIONAL_RELEASE + effectiveTime + "/RF2Release/Delta/\n" +
		INTERNATIONAL_RELEASE + effectiveTime + "/RF2Release/Delta/Refset/\n" +
		INTERNATIONAL_RELEASE + effectiveTime + "/RF2Release/Delta/Refset/Language/\n" +
		INTERNATIONAL_RELEASE + effectiveTime + "/RF2Release/Delta/Refset/Language/der2_cRefset_LanguageDelta-en_INT_"+ effectiveTime + ".txt";
	}

	private String getStats(String message) {
		Runtime runtime = Runtime.getRuntime();
		long inUse = runtime.totalMemory() - runtime.freeMemory();
		long memoryInMb = bytesToMb(inUse);
		long memoryDiff = memoryInMb - lastMemoryInMb;
		this.lastMemoryInMb = memoryInMb;
		long seconds = new Date().getTime() / 1000;
		long timeTaken = seconds - lastSeconds;
		this.lastSeconds = seconds;
		return message + ": " + memoryInMb + " MB. " + memoryDiff +" MB diff. Max " + bytesToMb(runtime.maxMemory()) + ". Seconds " + timeTaken;
	}

	private long bytesToMb(long inUse) {
		return inUse / (1024 * 1024);
	}

}

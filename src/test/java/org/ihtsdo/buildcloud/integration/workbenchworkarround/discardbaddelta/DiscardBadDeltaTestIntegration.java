package org.ihtsdo.buildcloud.integration.workbenchworkarround.discardbaddelta;

import org.ihtsdo.buildcloud.rest.controller.AbstractControllerTest;
import org.ihtsdo.buildcloud.rest.controller.helper.IntegrationTestHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.zip.ZipFile;

public class DiscardBadDeltaTestIntegration extends AbstractControllerTest {

	private static final String INTERNATIONAL_RELEASE = "SnomedCT_Release_INTBadDelta_";

	private IntegrationTestHelper integrationTestHelper;

	@Override
	@BeforeEach
	public void setup() throws Exception {
		super.setup();
		integrationTestHelper = new IntegrationTestHelper(mockMvc, getClass().getSimpleName());
	}

	@Test
	public void testMultipleReleases() throws Exception {
		integrationTestHelper.createTestProductStructure();

		// Perform first time release
		integrationTestHelper.setFirstTimeRelease(true);
		integrationTestHelper.setWorkbenchDataFixesRequired(true);
		integrationTestHelper.setEffectiveTime("20140131");
		integrationTestHelper.setReadmeHeader("This is the readme for the first release © 2002-{readmeEndDate}.\\nTable of contents:\\n");
		integrationTestHelper.setReadmeEndDate("2014");
		executeAndVerifyResults("20140131");

		Thread.sleep(1000);

		integrationTestHelper.setFirstTimeRelease(false);
		integrationTestHelper.setEffectiveTime("20140731");
		integrationTestHelper.setReadmeHeader("This is the readme for the second release © 2002-{readmeEndDate}.\\nTable of contents:\\n");
		integrationTestHelper.setReadmeEndDate("2015");
		//get previous published files
		integrationTestHelper.setPreviousPublishedPackage(integrationTestHelper.getPreviousPublishedPackage());
		executeAndVerifyResults("20140731");
	}

	private void executeAndVerifyResults(String releaseDate) throws Exception {
		integrationTestHelper.uploadManifest("core_manifest_" + releaseDate + ".xml", getClass());

		String buildURL = integrationTestHelper.createBuild(releaseDate);
		loadDeltaFilesToInputDirectory(releaseDate);

		integrationTestHelper.scheduleBuild(buildURL);

		integrationTestHelper.waitUntilCompleted(buildURL);

		// Assert first release output expectations
		String expectedZipFilename = "SnomedCT_Release_INTBadDelta_" + releaseDate + ".zip";
		String expectedZipEntries = createExpectedZipEntries(releaseDate);
		ZipFile zipFile = integrationTestHelper.testZipNameAndEntryNames(buildURL, expectedZipFilename, expectedZipEntries, getClass());

		integrationTestHelper.assertZipContents("expectedoutput", zipFile, getClass());

		integrationTestHelper.publishOutput(buildURL);
	}

	private void loadDeltaFilesToInputDirectory(String releaseDate) throws Exception {
		integrationTestHelper.uploadDeltaInputFile("rel2_Concept_Delta_INT_" + releaseDate + ".txt", getClass());
		integrationTestHelper.uploadDeltaInputFile("rel2_cRefset_LanguageDelta-en_INT_" + releaseDate + ".txt", getClass());
		integrationTestHelper.uploadDeltaInputFile("rel2_Relationship_Delta_INT_" + releaseDate + ".txt", getClass());
		integrationTestHelper.uploadDeltaInputFile("rel2_StatedRelationship_Delta_INT_" + releaseDate + ".txt", getClass());
	}

	private String createExpectedZipEntries(String effectiveTime) {
		String expectedZipEntries =
			INTERNATIONAL_RELEASE + effectiveTime + "/\n" +
			INTERNATIONAL_RELEASE + effectiveTime + "/Readme_" + effectiveTime + ".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime + "/RF2Release/\n" +
			INTERNATIONAL_RELEASE + effectiveTime + "/RF2Release/Full/\n" +
			INTERNATIONAL_RELEASE + effectiveTime + "/RF2Release/Full/Refset/\n" +
			INTERNATIONAL_RELEASE + effectiveTime + "/RF2Release/Full/Refset/Language/\n" +
			INTERNATIONAL_RELEASE + effectiveTime + "/RF2Release/Full/Refset/Language/der2_cRefset_LanguageFull-en_INT_" + effectiveTime + ".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime + "/RF2Release/Full/Terminology/\n" +
			INTERNATIONAL_RELEASE + effectiveTime + "/RF2Release/Full/Terminology/sct2_Concept_Full_INT_" + effectiveTime + ".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime + "/RF2Release/Snapshot/\n" +
			INTERNATIONAL_RELEASE + effectiveTime + "/RF2Release/Snapshot/Refset/\n" +
			INTERNATIONAL_RELEASE + effectiveTime + "/RF2Release/Snapshot/Refset/Language/\n" +
			INTERNATIONAL_RELEASE + effectiveTime + "/RF2Release/Snapshot/Refset/Language/der2_cRefset_LanguageSnapshot-en_INT_" + effectiveTime + ".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime + "/RF2Release/Snapshot/Terminology/\n" +
			INTERNATIONAL_RELEASE + effectiveTime + "/RF2Release/Snapshot/Terminology/sct2_Concept_Snapshot_INT_" + effectiveTime + ".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime + "/RF2Release/Delta/\n" +
			INTERNATIONAL_RELEASE + effectiveTime + "/RF2Release/Delta/Refset/\n" +
			INTERNATIONAL_RELEASE + effectiveTime + "/RF2Release/Delta/Refset/Language/\n" +
			INTERNATIONAL_RELEASE + effectiveTime + "/RF2Release/Delta/Refset/Language/der2_cRefset_LanguageDelta-en_INT_" + effectiveTime + ".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime + "/RF2Release/Delta/Terminology/\n" +
			INTERNATIONAL_RELEASE + effectiveTime + "/RF2Release/Delta/Terminology/sct2_Concept_Delta_INT_" + effectiveTime + ".txt";
		return expectedZipEntries;
	}

}

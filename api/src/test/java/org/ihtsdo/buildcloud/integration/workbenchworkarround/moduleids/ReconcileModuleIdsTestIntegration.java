package org.ihtsdo.buildcloud.integration.workbenchworkarround.moduleids;

import org.ihtsdo.buildcloud.controller.AbstractControllerTest;
import org.ihtsdo.buildcloud.controller.helper.IntegrationTestHelper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.zip.ZipFile;

public class ReconcileModuleIdsTestIntegration extends AbstractControllerTest {

	private static final String INTERNATIONAL_RELEASE = "SnomedCT_Release_INT_";

	private IntegrationTestHelper integrationTestHelper;

	@Override
	@Before
	public void setup() throws Exception {
		super.setup();
		integrationTestHelper = new IntegrationTestHelper(mockMvc, "ReconcileModuleIdsTest");
	}

	@Test
	public void testMultipleReleases() throws Exception {
		integrationTestHelper.loginAsManager();
		integrationTestHelper.createTestProductStructure();

		// Perform first time release
		integrationTestHelper.setFirstTimeRelease(true);
		integrationTestHelper.setCreateInferredRelationships(true);
		integrationTestHelper.setWorkbenchDataFixesRequired(true);
		integrationTestHelper.setEffectiveTime("20140131");
		integrationTestHelper.setReadmeHeader("This is the readme for the first release © 2002-{readmeEndDate}.\\nTable of contents:\\n");
		integrationTestHelper.setReadmeEndDate("2014");
		loadDeltaFilesToInputDirectory("20140131");
		executeAndVerfiyResults("20140131");

		Thread.sleep(1000);

		//delete previous input files
		integrationTestHelper.deletePreviousTxtInputFiles();
		integrationTestHelper.setFirstTimeRelease(false);
		integrationTestHelper.setEffectiveTime("20140731");
		integrationTestHelper.setReadmeHeader("This is the readme for the second release © 2002-{readmeEndDate}.\\nTable of contents:\\n");
		integrationTestHelper.setReadmeEndDate("2015");
		//get previous published files
		String previousPublishedPackage = integrationTestHelper.getPreviousPublishedPackage();
		Assert.assertEquals("SnomedCT_Release_INT_20140131.zip", previousPublishedPackage);
		integrationTestHelper.setPreviousPublishedPackage(previousPublishedPackage);
		loadDeltaFilesToInputDirectory("20140731");
		executeAndVerfiyResults("20140731");
	}

	private void executeAndVerfiyResults(String releaseDate) throws Exception {
		String buildURL1 = integrationTestHelper.createBuild();
		integrationTestHelper.triggerBuild(buildURL1);
		integrationTestHelper.publishOutput(buildURL1);

		// Assert first release output expectations
		String expectedZipFilename = "SnomedCT_Release_INT_"+releaseDate+".zip";
		String expectedZipEntries = createExpectedZipEntries(releaseDate);
		ZipFile zipFile = integrationTestHelper.testZipNameAndEntryNames(buildURL1, expectedZipFilename, expectedZipEntries, getClass());

		integrationTestHelper.assertZipContents("expectedoutput", zipFile, getClass());
	}

	private void loadDeltaFilesToInputDirectory(String releaseDate) throws Exception {
		integrationTestHelper.uploadManifest("core_manifest_"+releaseDate+".xml", getClass());
		integrationTestHelper.uploadDeltaInputFile("rel2_Concept_Delta_INT_" + releaseDate + ".txt", getClass());
		integrationTestHelper.uploadDeltaInputFile("rel2_StatedRelationship_Delta_INT_"+releaseDate +".txt", getClass());
		integrationTestHelper.uploadDeltaInputFile("rel2_cRefset_LanguageDelta-en_INT_" + releaseDate +".txt", getClass());
	}

	private String createExpectedZipEntries(String effectiveTime) {
		String expectedZipEntries =
			INTERNATIONAL_RELEASE + effectiveTime + "/\n" +
			INTERNATIONAL_RELEASE + effectiveTime + "/RF2Release/\n" +
			INTERNATIONAL_RELEASE + effectiveTime + "/RF2Release/Full/\n" +
			INTERNATIONAL_RELEASE + effectiveTime + "/RF2Release/Full/Refset/\n" +
			INTERNATIONAL_RELEASE + effectiveTime + "/RF2Release/Full/Refset/Language/\n" +
			INTERNATIONAL_RELEASE + effectiveTime + "/RF2Release/Full/Refset/Language/der2_cRefset_LanguageFull-en_INT_" + effectiveTime + ".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime + "/RF2Release/Full/Terminology/\n" +
			INTERNATIONAL_RELEASE + effectiveTime + "/RF2Release/Full/Terminology/sct2_Concept_Full_INT_"+effectiveTime+".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime + "/RF2Release/Full/Terminology/sct2_Relationship_Full_INT_"+effectiveTime+".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime + "/RF2Release/Full/Terminology/sct2_StatedRelationship_Full_INT_"+effectiveTime+".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime + "/RF2Release/Snapshot/\n" +
			INTERNATIONAL_RELEASE + effectiveTime + "/RF2Release/Snapshot/Refset/\n" +
			INTERNATIONAL_RELEASE + effectiveTime + "/RF2Release/Snapshot/Refset/Language/\n" +
			INTERNATIONAL_RELEASE + effectiveTime + "/RF2Release/Snapshot/Refset/Language/der2_cRefset_LanguageSnapshot-en_INT_" + effectiveTime + ".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime + "/RF2Release/Snapshot/Terminology/\n" +
			INTERNATIONAL_RELEASE + effectiveTime + "/RF2Release/Snapshot/Terminology/sct2_Concept_Snapshot_INT_"+effectiveTime+".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime + "/RF2Release/Snapshot/Terminology/sct2_Relationship_Snapshot_INT_"+ effectiveTime+".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime + "/RF2Release/Snapshot/Terminology/sct2_StatedRelationship_Snapshot_INT_"+ effectiveTime+".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime + "/RF2Release/Delta/\n" +
			INTERNATIONAL_RELEASE + effectiveTime + "/RF2Release/Delta/Refset/\n" +
			INTERNATIONAL_RELEASE + effectiveTime + "/RF2Release/Delta/Refset/Language/\n" +
			INTERNATIONAL_RELEASE + effectiveTime + "/RF2Release/Delta/Refset/Language/der2_cRefset_LanguageDelta-en_INT_" + effectiveTime + ".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime + "/RF2Release/Delta/Terminology/\n" +
			INTERNATIONAL_RELEASE + effectiveTime + "/RF2Release/Delta/Terminology/sct2_Concept_Delta_INT_"+ effectiveTime +".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime + "/RF2Release/Delta/Terminology/sct2_Relationship_Delta_INT_"+ effectiveTime +".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime + "/RF2Release/Delta/Terminology/sct2_StatedRelationship_Delta_INT_"+ effectiveTime +".txt";
		return expectedZipEntries;
	}
}

package org.ihtsdo.buildcloud.integration.uuidrefsetid;

import java.util.zip.ZipFile;

import org.ihtsdo.buildcloud.rest.controller.AbstractControllerTest;
import org.ihtsdo.buildcloud.rest.controller.helper.IntegrationTestHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class UUIDRefsetIdTestIntegration extends AbstractControllerTest {

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
		final String effectiveTime = "20140131";
		integrationTestHelper.uploadManifest("simple_refset_manifest_" + effectiveTime + ".xml", getClass());
		integrationTestHelper.setEffectiveTime(effectiveTime);
		integrationTestHelper.setFirstTimeRelease(true);
		integrationTestHelper.setWorkbenchDataFixesRequired(true);
		integrationTestHelper.setReadmeHeader("This is the readme for the first release.\\nTable of contents:\\n");

		final String buildURL1 = integrationTestHelper.createBuild(effectiveTime);
		integrationTestHelper.uploadDeltaInputFile("rel2_Concept_Delta_INT_" + effectiveTime + ".txt", getClass());
		integrationTestHelper.uploadDeltaInputFile("rel2_Refset_SimpleDelta_INT_" + effectiveTime + ".txt", getClass());
		integrationTestHelper.uploadDeltaInputFile("rel2_Relationship_Delta_INT_" + effectiveTime + ".txt", getClass());
		integrationTestHelper.uploadDeltaInputFile("rel2_StatedRelationship_Delta_INT_" + effectiveTime + ".txt", getClass());
		integrationTestHelper.scheduleBuild(buildURL1);
		integrationTestHelper.waitUntilCompleted(buildURL1);

		// Assert first release output expectations
		String expectedZipFilename = "SnomedCT_Release_INT_20140131.zip";
		String expectedZipEntries = "SnomedCT_Release_INT_20140131/\n" +
				"SnomedCT_Release_INT_20140131/Readme_20140131.txt\n" +
				"SnomedCT_Release_INT_20140131/RF2Release/\n" +
				"SnomedCT_Release_INT_20140131/RF2Release/Full/\n" +
				"SnomedCT_Release_INT_20140131/RF2Release/Full/Refset/\n" +
				"SnomedCT_Release_INT_20140131/RF2Release/Full/Refset/Content/\n" +
				"SnomedCT_Release_INT_20140131/RF2Release/Full/Refset/Content/sct2_Concept_Full_INT_20140131.txt\n" +
				"SnomedCT_Release_INT_20140131/RF2Release/Full/Refset/Content/der2_Refset_SimpleFull_INT_20140131.txt\n" +
				"SnomedCT_Release_INT_20140131/RF2Release/Snapshot/\n" +
				"SnomedCT_Release_INT_20140131/RF2Release/Snapshot/Refset/\n" +
				"SnomedCT_Release_INT_20140131/RF2Release/Snapshot/Refset/Content/\n" +
				"SnomedCT_Release_INT_20140131/RF2Release/Snapshot/Refset/Content/sct2_Concept_Snapshot_INT_20140131.txt\n" +
				"SnomedCT_Release_INT_20140131/RF2Release/Snapshot/Refset/Content/der2_Refset_SimpleSnapshot_INT_20140131.txt\n" +
				"SnomedCT_Release_INT_20140131/RF2Release/Delta/\n" +
				"SnomedCT_Release_INT_20140131/RF2Release/Delta/Refset/\n" +
				"SnomedCT_Release_INT_20140131/RF2Release/Delta/Refset/Content/\n" +
				"SnomedCT_Release_INT_20140131/RF2Release/Delta/Refset/Content/sct2_Concept_Delta_INT_20140131.txt\n" +
				"SnomedCT_Release_INT_20140131/RF2Release/Delta/Refset/Content/der2_Refset_SimpleDelta_INT_20140131.txt";
		final ZipFile zipFileFirstRelease = integrationTestHelper.testZipNameAndEntryNames(buildURL1, expectedZipFilename, expectedZipEntries, getClass());
		integrationTestHelper.assertZipContents("expectedoutput", zipFileFirstRelease, getClass());

		integrationTestHelper.publishOutput(buildURL1);

		// Sleep for a second. Next product must have a different timestamp.
		Thread.sleep(1000);


		// Perform second release
		final String effectiveDateTime = "20140731";
		integrationTestHelper.uploadManifest("simple_refset_manifest_" + effectiveDateTime + ".xml", getClass());
		integrationTestHelper.setEffectiveTime(effectiveDateTime);
		integrationTestHelper.setFirstTimeRelease(false);
		integrationTestHelper.setPreviousPublishedPackage(integrationTestHelper.getPreviousPublishedPackage());
		integrationTestHelper.setReadmeHeader("This is the readme for the second release.\\nTable of contents:\\n");
		final String buildURL2 = integrationTestHelper.createBuild(effectiveDateTime);
		integrationTestHelper.uploadDeltaInputFile("rel2_Refset_SimpleDelta_INT_" + effectiveDateTime + ".txt", getClass());
		integrationTestHelper.uploadDeltaInputFile("rel2_Relationship_Delta_INT_" + effectiveTime + ".txt", getClass());
		integrationTestHelper.uploadDeltaInputFile("rel2_StatedRelationship_Delta_INT_" + effectiveTime + ".txt", getClass());
		integrationTestHelper.scheduleBuild(buildURL2);
		integrationTestHelper.waitUntilCompleted(buildURL2);

		// Assert second release output expectations
		expectedZipFilename = "SnomedCT_Release_INT_20140731.zip";
		expectedZipEntries = "SnomedCT_Release_INT_20140731/\n" +
				"SnomedCT_Release_INT_20140731/Readme_20140731.txt\n" +
				"SnomedCT_Release_INT_20140731/RF2Release/\n" +
				"SnomedCT_Release_INT_20140731/RF2Release/Full/\n" +
				"SnomedCT_Release_INT_20140731/RF2Release/Full/Refset/\n" +
				"SnomedCT_Release_INT_20140731/RF2Release/Full/Refset/Content/\n" +
				"SnomedCT_Release_INT_20140731/RF2Release/Full/Refset/Content/der2_Refset_SimpleFull_INT_20140731.txt\n" +
				"SnomedCT_Release_INT_20140731/RF2Release/Snapshot/\n" +
				"SnomedCT_Release_INT_20140731/RF2Release/Snapshot/Refset/\n" +
				"SnomedCT_Release_INT_20140731/RF2Release/Snapshot/Refset/Content/\n" +
				"SnomedCT_Release_INT_20140731/RF2Release/Snapshot/Refset/Content/der2_Refset_SimpleSnapshot_INT_20140731.txt\n" +
				"SnomedCT_Release_INT_20140731/RF2Release/Delta/\n" +
				"SnomedCT_Release_INT_20140731/RF2Release/Delta/Refset/\n" +
				"SnomedCT_Release_INT_20140731/RF2Release/Delta/Refset/Content/\n" +
				"SnomedCT_Release_INT_20140731/RF2Release/Delta/Refset/Content/der2_Refset_SimpleDelta_INT_20140731.txt";
		final ZipFile zipFileSecondRelease = integrationTestHelper.testZipNameAndEntryNames(buildURL2, expectedZipFilename, expectedZipEntries, getClass());
		integrationTestHelper.assertZipContents("expectedoutput", zipFileSecondRelease, getClass());
	}

}

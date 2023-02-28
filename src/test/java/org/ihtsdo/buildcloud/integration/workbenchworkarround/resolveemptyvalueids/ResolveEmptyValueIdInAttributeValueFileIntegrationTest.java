package org.ihtsdo.buildcloud.integration.workbenchworkarround.resolveemptyvalueids;

import java.util.zip.ZipFile;

import org.ihtsdo.buildcloud.rest.controller.AbstractControllerTest;
import org.ihtsdo.buildcloud.rest.controller.helper.IntegrationTestHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ResolveEmptyValueIdInAttributeValueFileIntegrationTest extends AbstractControllerTest {
	private IntegrationTestHelper integrationTestHelper;
	private static final String INTERNATIONAL_RELEASE = "SnomedCT_Release_INT_";

	@Override
	@BeforeEach
	public void setup() throws Exception {
		super.setup();
		integrationTestHelper = new IntegrationTestHelper(mockMvc, "ResolveEmptyValueIdInAttributeValueFileIntegrationTest");
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
		loadDeltaFilesToInputDirectory("20140731");
		executeAndVerifyResults("20140731");

	}

	private void executeAndVerifyResults(String releaseDate) throws Exception {
		integrationTestHelper.uploadManifest("complex_refset_manifest_" + releaseDate + ".xml", getClass());
		String buildURL1 = integrationTestHelper.createBuild(releaseDate);
		loadDeltaFilesToInputDirectory(releaseDate);
		integrationTestHelper.scheduleBuild(buildURL1);
		integrationTestHelper.waitUntilCompleted(buildURL1);
		integrationTestHelper.publishOutput(buildURL1);

		// Assert first release output expectations
		String expectedZipFilename = "SnomedCT_Release_INT_" + releaseDate + ".zip";
		String expectedZipEntries = createExpectedZipEntries(releaseDate);
		ZipFile zipFile = integrationTestHelper.testZipNameAndEntryNames(buildURL1, expectedZipFilename, expectedZipEntries, getClass());

		integrationTestHelper.assertZipContents("expectedoutput", zipFile, getClass());
	}

	private void loadDeltaFilesToInputDirectory(String releaseDate) throws Exception {
		integrationTestHelper.uploadDeltaInputFile("rel2_cRefset_AttributeValueDelta_INT_" + releaseDate + ".txt", getClass());
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
			INTERNATIONAL_RELEASE + effectiveTime + "/RF2Release/Full/Refset/Content/\n" +
			INTERNATIONAL_RELEASE + effectiveTime + "/RF2Release/Full/Refset/Content/der2_cRefset_AttributeValueFull_INT_" + effectiveTime + ".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime + "/RF2Release/Snapshot/\n" +
			INTERNATIONAL_RELEASE + effectiveTime + "/RF2Release/Snapshot/Refset/\n" +
			INTERNATIONAL_RELEASE + effectiveTime + "/RF2Release/Snapshot/Refset/Content/\n" +
			INTERNATIONAL_RELEASE + effectiveTime + "/RF2Release/Snapshot/Refset/Content/der2_cRefset_AttributeValueSnapshot_INT_" + effectiveTime + ".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime + "/RF2Release/Delta/\n" +
			INTERNATIONAL_RELEASE + effectiveTime + "/RF2Release/Delta/Refset/\n" +
			INTERNATIONAL_RELEASE + effectiveTime + "/RF2Release/Delta/Refset/Content/\n" +
			INTERNATIONAL_RELEASE + effectiveTime + "/RF2Release/Delta/Refset/Content/der2_cRefset_AttributeValueDelta_INT_" + effectiveTime + ".txt";
		return expectedZipEntries;
	}

}

package org.ihtsdo.buildcloud.integration.refsetcustomkey;

import org.ihtsdo.buildcloud.rest.controller.AbstractControllerTest;
import org.ihtsdo.buildcloud.rest.controller.helper.IntegrationTestHelper;
import org.junit.Before;
import org.junit.Test;

import java.util.zip.ZipFile;

public class RefsetCustomKeyTestIntegration extends AbstractControllerTest {

	private IntegrationTestHelper integrationTestHelper;

	@Override
	@Before
	public void setup() throws Exception {
		super.setup();
		integrationTestHelper = new IntegrationTestHelper(mockMvc, "refset_customkey_test");
	}

	@Test
	public void testNoCustomKey() throws Exception {
		String customRefsetCompositeKeys = "";
		String expectedOutputPackageName = "expectedoutput/defaults";

		runTest(customRefsetCompositeKeys, expectedOutputPackageName);
	}

	@Test
	public void testCustomKeys() throws Exception {
		String customRefsetCompositeKeys = "447562003=9|447535001=13|447200001=13,14|700043003=3,4";
		String expectedOutputPackageName = "expectedoutput/keyMapAdvice";

		runTest(customRefsetCompositeKeys, expectedOutputPackageName);
	}

	private void runTest(String customRefsetCompositeKeys, String expectedOutputPackageName) throws Exception {
		String effectiveTime = "20140131";
		setupProduct(effectiveTime);
		integrationTestHelper.setCustomRefsetCompositeKeys(customRefsetCompositeKeys);
		String buildUrl = integrationTestHelper.createBuild();
		integrationTestHelper.uploadDeltaInputFile("rel2_iisssccRefset_ExtendedMapDelta_INT_" + effectiveTime + ".txt", getClass());
		integrationTestHelper.uploadDeltaInputFile("rel2_iisssccsiRefset_UnknownFormatDelta_INT_" + effectiveTime + ".txt", getClass());
		integrationTestHelper.uploadDeltaInputFile("rel2_Refset_SimpleDelta_INT_" + effectiveTime + ".txt", getClass());

		integrationTestHelper.scheduleBuild(buildUrl);

		// wait until the build is completed
		integrationTestHelper.waitUntilCompleted(buildUrl);

		assertOutput(expectedOutputPackageName, buildUrl);
	}

	private void setupProduct(String effectiveTime) throws Exception {
		integrationTestHelper.createTestProductStructure();
		// Perform first time release
		integrationTestHelper.uploadManifest("manifest.xml", getClass());
		integrationTestHelper.setEffectiveTime(effectiveTime);
		integrationTestHelper.setFirstTimeRelease(true);
		integrationTestHelper.setWorkbenchDataFixesRequired(true);
		integrationTestHelper.setReadmeHeader("header");
	}

	private void assertOutput(String expectedOutputPackageName, String buildUrl) throws Exception {
		String expectedZipFilename = "SnomedCT_Release_INT_20140131.zip";
		String expectedZipEntries = "SnomedCT_Release_INT_20140131/\n" +
				"SnomedCT_Release_INT_20140131/RF2Release/\n" +
				"SnomedCT_Release_INT_20140131/RF2Release/Full/\n" +
				"SnomedCT_Release_INT_20140131/RF2Release/Full/Refset/\n" +
				"SnomedCT_Release_INT_20140131/RF2Release/Full/Refset/Content/\n" +
				"SnomedCT_Release_INT_20140131/RF2Release/Full/Refset/Content/der2_iisssccRefset_ExtendedMapFull_INT_20140131.txt\n" +
				"SnomedCT_Release_INT_20140131/RF2Release/Full/Refset/Content/der2_iisssccsiRefset_UnknownFormatFull_INT_20140131.txt\n" +
				"SnomedCT_Release_INT_20140131/RF2Release/Full/Refset/Content/der2_Refset_SimpleFull_INT_20140131.txt";
		ZipFile zipFileFirstRelease = integrationTestHelper.testZipNameAndEntryNames(buildUrl, expectedZipFilename, expectedZipEntries, getClass());
		integrationTestHelper.assertZipContents(expectedOutputPackageName, zipFileFirstRelease, getClass());
	}

}

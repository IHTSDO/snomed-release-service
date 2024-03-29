package org.ihtsdo.buildcloud.integration.zipinput;

import org.ihtsdo.buildcloud.rest.controller.AbstractControllerTest;
import org.ihtsdo.buildcloud.rest.controller.helper.IntegrationTestHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.zip.ZipFile;

public class ZipInputTestIntegration extends AbstractControllerTest {

	private IntegrationTestHelper integrationTestHelper;
	
	@Override
	@BeforeEach
	public void setup() throws Exception {
		super.setup();
		integrationTestHelper = new IntegrationTestHelper(mockMvc, getClass().getSimpleName());
	}

	@Test
	public void testRelease() throws Exception {
		integrationTestHelper.createTestProductStructure();

		// Perform first time release
		integrationTestHelper.uploadManifest("simple_refset_manifest_20140131.xml", getClass());
		integrationTestHelper.setEffectiveTime("20140131");
		integrationTestHelper.setFirstTimeRelease(true);
		integrationTestHelper.setReadmeHeader("This is the readme for the first release.\\nTable of contents:\\n");
		String buildURL1 = integrationTestHelper.createBuild("20140131");
		integrationTestHelper.uploadDeltaInputFile("Archive.zip", getClass());
		integrationTestHelper.scheduleBuild(buildURL1);
		integrationTestHelper.waitUntilCompleted(buildURL1);
		integrationTestHelper.publishOutput(buildURL1);

		// Assert first release output expectations
		String expectedZipFilename = "SnomedCT_Release_INT_20140131.zip";
		String expectedZipEntries = """
                SnomedCT_Release_INT_20140131/
                SnomedCT_Release_INT_20140131/Readme_20140131.txt
                SnomedCT_Release_INT_20140131/RF2Release/
                SnomedCT_Release_INT_20140131/RF2Release/Full/
                SnomedCT_Release_INT_20140131/RF2Release/Full/Refset/
                SnomedCT_Release_INT_20140131/RF2Release/Full/Refset/Content/
                SnomedCT_Release_INT_20140131/RF2Release/Full/Refset/Content/der2_Refset_SimpleFull_INT_20140131.txt
                SnomedCT_Release_INT_20140131/RF2Release/Full/Refset/Content/der2_Refset_SimpleAnotherFull_INT_20140131.txt
                SnomedCT_Release_INT_20140131/RF2Release/Snapshot/
                SnomedCT_Release_INT_20140131/RF2Release/Snapshot/Refset/
                SnomedCT_Release_INT_20140131/RF2Release/Snapshot/Refset/Content/
                SnomedCT_Release_INT_20140131/RF2Release/Snapshot/Refset/Content/der2_Refset_SimpleSnapshot_INT_20140131.txt
                SnomedCT_Release_INT_20140131/RF2Release/Snapshot/Refset/Content/der2_Refset_SimpleAnotherSnapshot_INT_20140131.txt
                SnomedCT_Release_INT_20140131/RF2Release/Delta/
                SnomedCT_Release_INT_20140131/RF2Release/Delta/Refset/
                SnomedCT_Release_INT_20140131/RF2Release/Delta/Refset/Content/
                SnomedCT_Release_INT_20140131/RF2Release/Delta/Refset/Content/der2_Refset_SimpleDelta_INT_20140131.txt
                SnomedCT_Release_INT_20140131/RF2Release/Delta/Refset/Content/der2_Refset_SimpleAnotherDelta_INT_20140131.txt""";
		ZipFile zipFileFirstRelease = integrationTestHelper.testZipNameAndEntryNames(buildURL1, expectedZipFilename, expectedZipEntries, getClass());
		integrationTestHelper.assertZipContents("expectedoutput", zipFileFirstRelease, getClass());
	}

}

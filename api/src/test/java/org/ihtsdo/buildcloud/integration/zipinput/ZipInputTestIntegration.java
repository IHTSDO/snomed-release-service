package org.ihtsdo.buildcloud.integration.zipinput;

import org.ihtsdo.buildcloud.controller.AbstractControllerTest;
import org.ihtsdo.buildcloud.controller.helper.IntegrationTestHelper;
import org.junit.Before;
import org.junit.Test;

import java.util.zip.ZipFile;

public class ZipInputTestIntegration extends AbstractControllerTest {

	private IntegrationTestHelper integrationTestHelper;
	
	@Override
	@Before
	public void setup() throws Exception {
		super.setup();
		integrationTestHelper = new IntegrationTestHelper(mockMvc,"zip_input_test");
	}

	@Test
	public void testRelease() throws Exception {
		integrationTestHelper.loginAsManager();
		integrationTestHelper.createTestBuildStructure();

		// Perform first time release
		integrationTestHelper.uploadDeltaInputFile("Archive.zip", getClass());
		integrationTestHelper.uploadManifest("simple_refset_manifest_20140131.xml", getClass());
		integrationTestHelper.setEffectiveTime("20140131");
		integrationTestHelper.setFirstTimeRelease(true);
		integrationTestHelper.setReadmeHeader("This is the readme for the first release.\\nTable of contents:\\n");
		String executionURL1 = integrationTestHelper.createExecution();
		integrationTestHelper.triggerExecution(executionURL1);
		integrationTestHelper.publishOutput(executionURL1);

		// Assert first release output expectations
		String expectedZipFilename = "SnomedCT_Release_INT_20140131.zip";
		String expectedZipEntries = "SnomedCT_Release_INT_20140131/\n" +
				"SnomedCT_Release_INT_20140131/RF2Release/\n" +
				"SnomedCT_Release_INT_20140131/RF2Release/Full/\n" +
				"SnomedCT_Release_INT_20140131/RF2Release/Full/Refset/\n" +
				"SnomedCT_Release_INT_20140131/RF2Release/Full/Refset/Content/\n" +
				"SnomedCT_Release_INT_20140131/RF2Release/Full/Refset/Content/der2_Refset_SimpleFull_INT_20140131.txt\n" +
				"SnomedCT_Release_INT_20140131/RF2Release/Full/Refset/Content/der2_Refset_SimpleAnotherFull_INT_20140131.txt\n" +
				"SnomedCT_Release_INT_20140131/RF2Release/Snapshot/\n" +
				"SnomedCT_Release_INT_20140131/RF2Release/Snapshot/Refset/\n" +
				"SnomedCT_Release_INT_20140131/RF2Release/Snapshot/Refset/Content/\n" +
				"SnomedCT_Release_INT_20140131/RF2Release/Snapshot/Refset/Content/der2_Refset_SimpleSnapshot_INT_20140131.txt\n" +
				"SnomedCT_Release_INT_20140131/RF2Release/Snapshot/Refset/Content/der2_Refset_SimpleAnotherSnapshot_INT_20140131.txt\n" +
				"SnomedCT_Release_INT_20140131/RF2Release/Delta/\n" +
				"SnomedCT_Release_INT_20140131/RF2Release/Delta/Refset/\n" +
				"SnomedCT_Release_INT_20140131/RF2Release/Delta/Refset/Content/\n" +
				"SnomedCT_Release_INT_20140131/RF2Release/Delta/Refset/Content/der2_Refset_SimpleDelta_INT_20140131.txt\n" +
				"SnomedCT_Release_INT_20140131/RF2Release/Delta/Refset/Content/der2_Refset_SimpleAnotherDelta_INT_20140131.txt";
		ZipFile zipFileFirstRelease = integrationTestHelper.testZipNameAndEntryNames(executionURL1, expectedZipFilename, expectedZipEntries, getClass());
		integrationTestHelper.assertZipContents("expectedoutput", zipFileFirstRelease, getClass());
	}

}

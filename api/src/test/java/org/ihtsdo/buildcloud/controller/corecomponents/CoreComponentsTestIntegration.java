package org.ihtsdo.buildcloud.controller.corecomponents;

import org.ihtsdo.buildcloud.controller.AbstractControllerTest;
import org.ihtsdo.buildcloud.controller.helper.IntegrationTestHelper;
import org.ihtsdo.buildcloud.dao.s3.S3Client;
import org.ihtsdo.buildcloud.dao.s3.TestS3Client;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.ServletException;
import java.util.zip.ZipFile;

public class CoreComponentsTestIntegration extends AbstractControllerTest {

	@Autowired
	private S3Client s3Client;

	private IntegrationTestHelper integrationTestHelper;

	@Test
	public void testFirstRelease() throws Exception {
		integrationTestHelper.loginAsManager();
		integrationTestHelper.createTestBuildStructure();


		// Perform first time release
		integrationTestHelper.setEffectiveTime("20140131");
		integrationTestHelper.uploadManifest("core_manifest_20140131.xml", getClass());
		integrationTestHelper.uploadDeltaInputFile("sct2_Concept_Delta_INT_20140131.txt", getClass());
		integrationTestHelper.uploadDeltaInputFile("sct2_Description_Delta-en_INT_20140131.txt", getClass());
		integrationTestHelper.uploadDeltaInputFile("sct2_StatedRelationship_Delta_INT_20140131.txt", getClass());
		integrationTestHelper.uploadDeltaInputFile("der2_cRefset_LanguageDelta-en_INT_20140131.txt", getClass());
		integrationTestHelper.setFirstTimeRelease(true);
		integrationTestHelper.setReadmeHeader("This is the readme for the first core release.\\nTable of contents:\\n");
		String executionURL1 = integrationTestHelper.createExecution();
		integrationTestHelper.triggerExecution(executionURL1);
		integrationTestHelper.publishOutput(executionURL1);


		// Assert first release output expectations
		String expectedZipFilename = "SnomedCT_Release_INT_20140131.zip";
		String expectedZipEntries = "SnomedCT_Release_INT_20140131/\n" +
				"SnomedCT_Release_INT_20140131/RF2Release/\n" +
				"SnomedCT_Release_INT_20140131/RF2Release/Full/\n" +
				"SnomedCT_Release_INT_20140131/RF2Release/Full/Refset/\n" +
				"SnomedCT_Release_INT_20140131/RF2Release/Full/Refset/Language/\n" +
				"SnomedCT_Release_INT_20140131/RF2Release/Full/Refset/Language/der2_cRefset_LanguageFull-en_INT_20140131.txt\n" +
				"SnomedCT_Release_INT_20140131/RF2Release/Full/Terminology/\n" +
				"SnomedCT_Release_INT_20140131/RF2Release/Full/Terminology/sct2_Concept_Full_INT_20140131.txt\n" +
				"SnomedCT_Release_INT_20140131/RF2Release/Full/Terminology/sct2_Description_Full-en_INT_20140131.txt\n" +
				"SnomedCT_Release_INT_20140131/RF2Release/Full/Terminology/sct2_StatedRelationship_Full_INT_20140131.txt\n" +
				"SnomedCT_Release_INT_20140131/RF2Release/Snapshot/\n" +
				"SnomedCT_Release_INT_20140131/RF2Release/Snapshot/Refset/\n" +
				"SnomedCT_Release_INT_20140131/RF2Release/Snapshot/Refset/Language/\n" +
				"SnomedCT_Release_INT_20140131/RF2Release/Snapshot/Refset/Language/der2_cRefset_LanguageSnapshot-en_INT_20140131.txt\n" +
				"SnomedCT_Release_INT_20140131/RF2Release/Snapshot/Terminology/\n" +
				"SnomedCT_Release_INT_20140131/RF2Release/Snapshot/Terminology/sct2_Concept_Snapshot_INT_20140131.txt\n" +
				"SnomedCT_Release_INT_20140131/RF2Release/Snapshot/Terminology/sct2_Description_Snapshot-en_INT_20140131.txt\n" +
				"SnomedCT_Release_INT_20140131/RF2Release/Snapshot/Terminology/sct2_StatedRelationship_Snapshot_INT_20140131.txt\n" +
				"SnomedCT_Release_INT_20140131/RF2Release/Delta/\n" +
				"SnomedCT_Release_INT_20140131/RF2Release/Delta/Refset/\n" +
				"SnomedCT_Release_INT_20140131/RF2Release/Delta/Refset/Language/\n" +
				"SnomedCT_Release_INT_20140131/RF2Release/Delta/Refset/Language/der2_cRefset_LanguageDelta-en_INT_20140131.txt\n" +
				"SnomedCT_Release_INT_20140131/RF2Release/Delta/Terminology/\n" +
				"SnomedCT_Release_INT_20140131/RF2Release/Delta/Terminology/sct2_Concept_Delta_INT_20140131.txt\n" +
				"SnomedCT_Release_INT_20140131/RF2Release/Delta/Terminology/sct2_Description_Delta-en_INT_20140131.txt\n" +
				"SnomedCT_Release_INT_20140131/RF2Release/Delta/Terminology/sct2_StatedRelationship_Delta_INT_20140131.txt";
		ZipFile zipFile = integrationTestHelper.testZipNameAndEntryNames(executionURL1, 14, expectedZipFilename, expectedZipEntries, getClass());

		integrationTestHelper.assertZipContents("expectedoutput", zipFile, getClass());
	}

	@Override
	@Before
	public void setup() throws ServletException {
		super.setup();
		integrationTestHelper = new IntegrationTestHelper(mockMvc);
		((TestS3Client) s3Client).deleteBuckets();
	}

}

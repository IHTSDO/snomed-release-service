package org.ihtsdo.buildcloud.controller.corecomponents;

import java.io.IOException;
import java.util.zip.ZipFile;

import org.ihtsdo.buildcloud.controller.AbstractControllerTest;
import org.ihtsdo.buildcloud.controller.helper.IntegrationTestHelper;
import org.ihtsdo.buildcloud.dao.s3.S3Client;
import org.ihtsdo.buildcloud.dao.s3.TestS3Client;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class CoreComponentsTestIntegration extends AbstractControllerTest {

	private static final String INTERNATIONAL_RELEASE = "SnomedCT_Release_INT_";

	@Autowired
	private S3Client s3Client;

	private IntegrationTestHelper integrationTestHelper;

	@Override
	@Before
	public void setup() throws Exception {
		super.setup();
		integrationTestHelper = new IntegrationTestHelper(mockMvc);
		((TestS3Client) s3Client).deleteBuckets();
	}
	@Test
	public void testMultipleReleases() throws Exception {
		integrationTestHelper.loginAsManager();
		integrationTestHelper.createTestBuildStructure();
		performFirstRelease("20140131");
		Thread.sleep(1000);
		peformSubsequentRelease("20140731");
	}

	private void peformSubsequentRelease(String releaseDate) throws Exception {
	    //delete previous input files
	    integrationTestHelper.deletePreviousTxtInputFiles();
	    integrationTestHelper.setFirstTimeRelease(false);
	    integrationTestHelper.setEffectiveTime(releaseDate);
	    //get previous published files
	    integrationTestHelper.setPreviousPublishedPackage(integrationTestHelper.getPreviousPublishedPackage());
	    loadDeltaFilesToInputDirectory(releaseDate);
	    executeAndVerfiyResults(releaseDate);
	    
	}

	private void performFirstRelease(String releaseDate) throws Exception {

	    // Perform first time release
	    integrationTestHelper.setFirstTimeRelease(true);
	    integrationTestHelper.setEffectiveTime(releaseDate);
	    loadDeltaFilesToInputDirectory(releaseDate);
	    executeAndVerfiyResults(releaseDate);
	}

	private void executeAndVerfiyResults(String releaseDate) throws Exception, IOException {
	    String executionURL1 = integrationTestHelper.createExecution();
	    integrationTestHelper.triggerExecution(executionURL1);
	    integrationTestHelper.publishOutput(executionURL1);

	    // Assert first release output expectations
	    String expectedZipFilename = "SnomedCT_Release_INT_"+releaseDate+".zip";
	    String expectedZipEntries = createExpectedZipEntries(releaseDate);
	    ZipFile zipFile = integrationTestHelper.testZipNameAndEntryNames(executionURL1, 14, expectedZipFilename, expectedZipEntries, getClass());

	    integrationTestHelper.assertZipContents("expectedoutput", zipFile, getClass());
	}

	private void loadDeltaFilesToInputDirectory(String releaseDate) throws Exception {
	   
	    integrationTestHelper.uploadManifest("core_manifest_"+releaseDate+".xml", getClass());
	    integrationTestHelper.uploadDeltaInputFile("sct2_Concept_Delta_INT_"+releaseDate +".txt", getClass());
	    integrationTestHelper.uploadDeltaInputFile("sct2_Description_Delta-en_INT_"+releaseDate +".txt", getClass());
	    integrationTestHelper.uploadDeltaInputFile("sct2_StatedRelationship_Delta_INT_"+releaseDate +".txt", getClass());
	    integrationTestHelper.uploadDeltaInputFile("der2_cRefset_LanguageDelta-en_INT_" + releaseDate +".txt", getClass());
	    integrationTestHelper.setReadmeHeader("This is the readme for the first core release.\\nTable of contents:\\n");
	}
	
	private String createExpectedZipEntries(String effectiveTime) {
	    String expectedZipEntries = INTERNATIONAL_RELEASE + effectiveTime + "/\n" +
	    		INTERNATIONAL_RELEASE + effectiveTime + "/RF2Release/\n" +
	    		INTERNATIONAL_RELEASE + effectiveTime + "/RF2Release/Full/\n" +
	    		INTERNATIONAL_RELEASE + effectiveTime +  "/RF2Release/Full/Refset/\n" +
	    		INTERNATIONAL_RELEASE + effectiveTime +  "/RF2Release/Full/Refset/Language/\n" +
			INTERNATIONAL_RELEASE + effectiveTime +  "/RF2Release/Full/Refset/Language/der2_cRefset_LanguageFull-en_INT_" + effectiveTime + ".txt\n" +
	    		INTERNATIONAL_RELEASE + effectiveTime + "/RF2Release/Full/Terminology/\n" +
	    		INTERNATIONAL_RELEASE + effectiveTime + "/RF2Release/Full/Terminology/sct2_Concept_Full_INT_"+effectiveTime+".txt\n" +
	    		INTERNATIONAL_RELEASE + effectiveTime + "/RF2Release/Full/Terminology/sct2_Description_Full-en_INT_"+effectiveTime+".txt\n" +
	    		INTERNATIONAL_RELEASE + effectiveTime + "/RF2Release/Full/Terminology/sct2_StatedRelationship_Full_INT_"+effectiveTime+".txt\n" +
	    		INTERNATIONAL_RELEASE + effectiveTime + "/RF2Release/Snapshot/\n" +
	    		INTERNATIONAL_RELEASE + effectiveTime + "/RF2Release/Snapshot/Refset/\n" +
			INTERNATIONAL_RELEASE + effectiveTime + "/RF2Release/Snapshot/Refset/Language/\n" +
			INTERNATIONAL_RELEASE + effectiveTime + "/RF2Release/Snapshot/Refset/Language/der2_cRefset_LanguageSnapshot-en_INT_"+ effectiveTime +".txt\n" +
	    		INTERNATIONAL_RELEASE + effectiveTime + "/RF2Release/Snapshot/Terminology/\n" +
	    		INTERNATIONAL_RELEASE + effectiveTime + "/RF2Release/Snapshot/Terminology/sct2_Concept_Snapshot_INT_"+effectiveTime+".txt\n" +
	    		INTERNATIONAL_RELEASE + effectiveTime + "/RF2Release/Snapshot/Terminology/sct2_Description_Snapshot-en_INT_"+effectiveTime+".txt\n" +
	    		INTERNATIONAL_RELEASE + effectiveTime + "/RF2Release/Snapshot/Terminology/sct2_StatedRelationship_Snapshot_INT_"+ effectiveTime+".txt\n" +
	    		INTERNATIONAL_RELEASE + effectiveTime + "/RF2Release/Delta/\n" +
	    		INTERNATIONAL_RELEASE + effectiveTime + "/RF2Release/Delta/Refset/\n" +
			INTERNATIONAL_RELEASE + effectiveTime + "/RF2Release/Delta/Refset/Language/\n" +
			INTERNATIONAL_RELEASE + effectiveTime + "/RF2Release/Delta/Refset/Language/der2_cRefset_LanguageDelta-en_INT_"+ effectiveTime + ".txt\n" +
	    		INTERNATIONAL_RELEASE + effectiveTime + "/RF2Release/Delta/Terminology/\n" +
	    		INTERNATIONAL_RELEASE + effectiveTime + "/RF2Release/Delta/Terminology/sct2_Concept_Delta_INT_"+ effectiveTime +".txt\n" +
	    		INTERNATIONAL_RELEASE + effectiveTime + "/RF2Release/Delta/Terminology/sct2_Description_Delta-en_INT_"+ effectiveTime +".txt\n" +
	    		INTERNATIONAL_RELEASE + effectiveTime + "/RF2Release/Delta/Terminology/sct2_StatedRelationship_Delta_INT_"+ effectiveTime +".txt";
	    return expectedZipEntries;
	}
}

package org.ihtsdo.buildcloud.integration.managedservice;

import java.util.zip.ZipFile;

import org.ihtsdo.buildcloud.rest.controller.AbstractControllerTest;
import org.ihtsdo.buildcloud.rest.controller.helper.IntegrationTestHelper;
import org.ihtsdo.buildcloud.core.service.ProductService;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class ManagedServiceReleaseTestIntegration extends AbstractControllerTest {
	
	private static final String MANAGED_SERVICE_RELEASE = "SnomedCT_ManagedServiceSE1000052_";

	private IntegrationTestHelper integrationTestHelper;

	@Override
	@Before
	public void setup() throws Exception {
		super.setup();
		integrationTestHelper = new IntegrationTestHelper(mockMvc,"ManagedServiceReleaseTestIntegration");
	}

	@Test
	@Ignore
	public void testFirstRelease() throws Exception {
		
		integrationTestHelper.createTestProductStructure();
		
		//config assertion tests
		integrationTestHelper.setAssertionTestConfigProperty(ProductService.ASSERTION_GROUP_NAMES, "Test Assertion Group");
		integrationTestHelper.setAssertionTestConfigProperty(ProductService.PREVIOUS_INTERNATIONAL_RELEASE, "20160731");

		// Perform first time release
		integrationTestHelper.setFirstTimeRelease(true);
		integrationTestHelper.setCreateLegacyIds(true);
		final String effectiveTime = "20161130";
		integrationTestHelper.setEffectiveTime(effectiveTime);
		integrationTestHelper.setReadmeHeader("This is the readme for the first release © 2002-{readmeEndDate}.\\nTable of contents:\\n");
		integrationTestHelper.setReadmeEndDate("2016");
		Thread.sleep(1000);
		
		loadDeltaFilesToInputDirectory(effectiveTime);

		final String expectedZipEntries =  MANAGED_SERVICE_RELEASE + effectiveTime + "/\n" +
				 MANAGED_SERVICE_RELEASE + effectiveTime + "/Readme_en_" + effectiveTime + ".txt\n" +
				 MANAGED_SERVICE_RELEASE + effectiveTime + "/Documentation/\n" + 
				 MANAGED_SERVICE_RELEASE + effectiveTime + "/Documentation/Current/\n" +
				 MANAGED_SERVICE_RELEASE + effectiveTime + "/Delta/\n" +
				 MANAGED_SERVICE_RELEASE + effectiveTime + "/Delta/Refset/\n" +
				 MANAGED_SERVICE_RELEASE + effectiveTime + "/Delta/Refset/Content/\n" +
				 MANAGED_SERVICE_RELEASE + effectiveTime + "/Delta/Refset/Content/der2_Refset_UrvalDeltagandetyperHälso-OchSjukvårdSimpleRefsetDelta_SE1000052_" + effectiveTime + ".txt\n" +
				 MANAGED_SERVICE_RELEASE + effectiveTime + "/Delta/Refset/Content/der2_Refset_UrvalDeltagandetyperSocialtjänstSimpleRefsetDelta_SE1000052_" + effectiveTime + ".txt\n" +
				 MANAGED_SERVICE_RELEASE + effectiveTime + "/Delta/Refset/Content/der2_Refset_UrvalSambandstyperHälso-OchSjukvårdSimpleRefsetDelta_SE1000052_" + effectiveTime + ".txt\n" +
				 MANAGED_SERVICE_RELEASE + effectiveTime + "/Delta/Refset/Content/der2_Refset_UrvalSambandstyperSocialtjänstSimpleRefsetDelta_SE1000052_" + effectiveTime + ".txt\n" +
				 MANAGED_SERVICE_RELEASE + effectiveTime + "/Delta/Refset/Language/\n" +
				 MANAGED_SERVICE_RELEASE + effectiveTime + "/Delta/Refset/Metadata/\n" +
				 MANAGED_SERVICE_RELEASE + effectiveTime + "/Delta/Terminology/\n" +
				 MANAGED_SERVICE_RELEASE + effectiveTime + "/Full/\n" +
				 MANAGED_SERVICE_RELEASE + effectiveTime + "/Full/Refset/\n" +
				 MANAGED_SERVICE_RELEASE + effectiveTime + "/Full/Refset/Content/\n" +
				 MANAGED_SERVICE_RELEASE + effectiveTime + "/Full/Refset/Content/der2_Refset_UrvalDeltagandetyperHälso-OchSjukvårdSimpleRefsetFull_SE1000052_" + effectiveTime + ".txt\n" +
				 MANAGED_SERVICE_RELEASE + effectiveTime + "/Full/Refset/Content/der2_Refset_UrvalDeltagandetyperSocialtjänstSimpleRefsetFull_SE1000052_" + effectiveTime + ".txt\n" +
				 MANAGED_SERVICE_RELEASE + effectiveTime + "/Full/Refset/Content/der2_Refset_UrvalSambandstyperHälso-OchSjukvårdSimpleRefsetFull_SE1000052_" + effectiveTime + ".txt\n" +
				 MANAGED_SERVICE_RELEASE + effectiveTime + "/Full/Refset/Content/der2_Refset_UrvalSambandstyperSocialtjänstSimpleRefsetFull_SE1000052_" + effectiveTime + ".txt\n" +
				 MANAGED_SERVICE_RELEASE + effectiveTime + "/Full/Refset/Language/\n" +
				 MANAGED_SERVICE_RELEASE + effectiveTime + "/Full/Refset/Metadata/\n" +
				 MANAGED_SERVICE_RELEASE + effectiveTime + "/Full/Terminology/\n" +
				 MANAGED_SERVICE_RELEASE + effectiveTime + "/Snapshot/\n" +
				 MANAGED_SERVICE_RELEASE + effectiveTime + "/Snapshot/Refset/\n" +
				 MANAGED_SERVICE_RELEASE + effectiveTime + "/Snapshot/Refset/Content/\n" +
				 MANAGED_SERVICE_RELEASE + effectiveTime + "/Snapshot/Refset/Content/der2_Refset_UrvalDeltagandetyperHälso-OchSjukvårdSimpleRefsetSnapshot_SE1000052_" + effectiveTime + ".txt\n" +
				 MANAGED_SERVICE_RELEASE + effectiveTime + "/Snapshot/Refset/Content/der2_Refset_UrvalDeltagandetyperSocialtjänstSimpleRefsetSnapshot_SE1000052_" + effectiveTime + ".txt\n" +
				 MANAGED_SERVICE_RELEASE + effectiveTime + "/Snapshot/Refset/Content/der2_Refset_UrvalSambandstyperHälso-OchSjukvårdSimpleRefsetSnapshot_SE1000052_" + effectiveTime + ".txt\n" +
				 MANAGED_SERVICE_RELEASE + effectiveTime + "/Snapshot/Refset/Content/der2_Refset_UrvalSambandstyperSocialtjänstSimpleRefsetSnapshot_SE1000052_" + effectiveTime + ".txt\n" +
				 MANAGED_SERVICE_RELEASE + effectiveTime + "/Snapshot/Refset/Language/\n" +
				 MANAGED_SERVICE_RELEASE + effectiveTime + "/Snapshot/Refset/Metadata/\n" +
				 MANAGED_SERVICE_RELEASE + effectiveTime + "/Snapshot/Terminology/";

		// This code extremely sensitive to timing issues because IdAssignmentBIOfflineDemoImpl only has one counter being accessed
		// by multiple streams.
		executeAndVerfiyResults(effectiveTime, expectedZipEntries );
	}
	
	
	
	private void loadDeltaFilesToInputDirectory(final String releaseDate) throws Exception {
		integrationTestHelper.uploadManifest("se_manifest.xml", getClass());
		integrationTestHelper.uploadDeltaInputFile("rel2_Refset_UrvalDeltagandetyperHälso-OchSjukvårdSimpleRefsetDelta_SE1000052_" + releaseDate +".txt", getClass());
		integrationTestHelper.uploadDeltaInputFile("rel2_Refset_UrvalDeltagandetyperSocialtjänstSimpleRefsetDelta_SE1000052_" + releaseDate +".txt", getClass());
		integrationTestHelper.uploadDeltaInputFile("rel2_Refset_UrvalSambandstyperHälso-OchSjukvårdSimpleRefsetDelta_SE1000052_" + releaseDate +".txt", getClass());
		String inputFileName = "rel2_Refset_UrvalSambandstyperSocialtjänstSimpleRefsetDelta_SE1000052_" + releaseDate +".txt";
		integrationTestHelper.uploadDeltaInputFile(inputFileName, getClass());
		integrationTestHelper.getInputFile(inputFileName);
	}
	
	

	private void executeAndVerfiyResults(final String releaseDate, final String expectedZipEntries) throws Exception {
		final String buildURL1 = integrationTestHelper.createBuild();
		integrationTestHelper.scheduleBuild(buildURL1);
		integrationTestHelper.waitUntilCompleted(buildURL1);
		integrationTestHelper.publishOutput(buildURL1);

		// Assert first release output expectations
		final String expectedZipFilename = "SnomedCT_ManagedServiceSE1000052_" + releaseDate + ".zip";
		final ZipFile zipFile = integrationTestHelper.testZipNameAndEntryNames(buildURL1, expectedZipFilename, expectedZipEntries, getClass());
		integrationTestHelper.assertZipContents("expectedoutput", zipFile, getClass(), false);
	}

}

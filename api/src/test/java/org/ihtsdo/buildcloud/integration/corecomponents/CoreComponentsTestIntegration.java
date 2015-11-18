package org.ihtsdo.buildcloud.integration.corecomponents;

import java.util.zip.ZipFile;

import org.ihtsdo.buildcloud.controller.AbstractControllerTest;
import org.ihtsdo.buildcloud.controller.helper.IntegrationTestHelper;
import org.ihtsdo.buildcloud.service.ProductService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class CoreComponentsTestIntegration extends AbstractControllerTest {

	private static final String INTERNATIONAL_RELEASE = "SnomedCT_Release_INT_";

	private IntegrationTestHelper integrationTestHelper;

	@Override
	@Before
	public void setup() throws Exception {
		super.setup();
		integrationTestHelper = new IntegrationTestHelper(mockMvc,"CoreComponentsTest");
	}

	@Test
	public void testMultipleReleases() throws Exception {
		firstTimeRelease();

		Thread.sleep(1000);

		//delete previous input files
		integrationTestHelper.deletePreviousTxtInputFiles();
		integrationTestHelper.setFirstTimeRelease(false);
		
		final String effectiveTime2 = "20140731";
		integrationTestHelper.setEffectiveTime(effectiveTime2);
		integrationTestHelper.setReadmeHeader("This is the readme for the second release © 2002-{readmeEndDate}.\\nTable of contents:\\n");
		integrationTestHelper.setReadmeEndDate("2015");
		//get previous published files
		final String previousPublishedPackage = integrationTestHelper.getPreviousPublishedPackage();
		Assert.assertEquals("SnomedCT_Release_INT_20140131.zip", previousPublishedPackage);
		integrationTestHelper.setPreviousPublishedPackage(previousPublishedPackage);
		integrationTestHelper.setNewRF2InputFiles("rel2_Refset_SimpleDelta_INT_20140731.txt");
		loadDeltaFilesToInputDirectory(effectiveTime2, false);
		integrationTestHelper.uploadDeltaInputFile("rel2_Refset_SimpleDelta_INT_20140731.txt", getClass());

		final String expectedZipEntries2 =
			INTERNATIONAL_RELEASE + effectiveTime2 + "/\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/Readme_" + effectiveTime2 + ".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Full/\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Full/Refset/\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Full/Refset/Language/\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Full/Refset/Language/der2_cRefset_LanguageFull-en_INT_" + effectiveTime2 + ".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Full/Refset/Map/\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Full/Refset/Map/der2_sRefset_SimpleMapFull_INT_" + effectiveTime2 + ".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Full/Refset/Map/der2_Refset_SimpleFull_INT_" + effectiveTime2 + ".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Full/Terminology/\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Full/Terminology/sct2_Concept_Full_INT_"+ effectiveTime2 +".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Full/Terminology/sct2_Description_Full-en_INT_"+ effectiveTime2 +".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Full/Terminology/sct2_TextDefinition_Full-en_INT_"+ effectiveTime2 +".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Full/Terminology/sct2_Relationship_Full_INT_"+ effectiveTime2 +".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Full/Terminology/sct2_StatedRelationship_Full_INT_"+ effectiveTime2 +".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Snapshot/\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Snapshot/Refset/\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Snapshot/Refset/Language/\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Snapshot/Refset/Language/der2_cRefset_LanguageSnapshot-en_INT_"+ effectiveTime2 +".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Snapshot/Refset/Map/\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Snapshot/Refset/Map/der2_sRefset_SimpleMapSnapshot_INT_" + effectiveTime2 + ".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Snapshot/Refset/Map/der2_Refset_SimpleSnapshot_INT_" + effectiveTime2 + ".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Snapshot/Terminology/\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Snapshot/Terminology/sct2_Concept_Snapshot_INT_"+ effectiveTime2 +".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Snapshot/Terminology/sct2_Description_Snapshot-en_INT_"+ effectiveTime2 +".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Snapshot/Terminology/sct2_TextDefinition_Snapshot-en_INT_"+ effectiveTime2 +".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Snapshot/Terminology/sct2_Relationship_Snapshot_INT_"+ effectiveTime2 +".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Snapshot/Terminology/sct2_StatedRelationship_Snapshot_INT_"+ effectiveTime2 +".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Delta/\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Delta/Refset/\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Delta/Refset/Language/\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Delta/Refset/Language/der2_cRefset_LanguageDelta-en_INT_"+ effectiveTime2 + ".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Delta/Refset/Map/\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Delta/Refset/Map/der2_sRefset_SimpleMapDelta_INT_" + effectiveTime2 + ".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Delta/Refset/Map/der2_Refset_SimpleDelta_INT_" + effectiveTime2 + ".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Delta/Terminology/\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Delta/Terminology/sct2_Concept_Delta_INT_"+ effectiveTime2 +".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Delta/Terminology/sct2_Description_Delta-en_INT_"+ effectiveTime2 +".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Delta/Terminology/sct2_TextDefinition_Delta-en_INT_"+ effectiveTime2 +".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Delta/Terminology/sct2_Relationship_Delta_INT_"+ effectiveTime2 +".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Delta/Terminology/sct2_StatedRelationship_Delta_INT_"+ effectiveTime2 +".txt";

		// This code extremely sensitive to timing issues because IdAssignmentBIOfflineDemoImpl only has one counter being accessed
		// by multiple streams.
		executeAndVerfiyResults(effectiveTime2, expectedZipEntries2, false );

	}

	private void firstTimeRelease() throws Exception {
		integrationTestHelper.loginAsManager();
		integrationTestHelper.createTestProductStructure();
		
		//config assertion tests
		integrationTestHelper.setAssertionTestConfigProperty(ProductService.ASSERTION_GROUP_NAMES, "Test Assertion Group");
		integrationTestHelper.setAssertionTestConfigProperty(ProductService.PREVIOUS_INTERNATIONAL_RELEASE, "20140731");

		// Perform first time release
		integrationTestHelper.setFirstTimeRelease(true);
		integrationTestHelper.setCreateInferredRelationships(true);
		integrationTestHelper.setCreateLegacyIds(true);
		final String effectiveTime1 = "20140131";
		integrationTestHelper.setEffectiveTime(effectiveTime1);
		integrationTestHelper.setReadmeHeader("This is the readme for the first release © 2002-{readmeEndDate}.\\nTable of contents:\\n");
		integrationTestHelper.setReadmeEndDate("2014");
		loadDeltaFilesToInputDirectory(effectiveTime1, false);
		final String expectedZipEntries1 =
			INTERNATIONAL_RELEASE + effectiveTime1 + "/\n" +
			INTERNATIONAL_RELEASE + effectiveTime1 + "/Readme_" + effectiveTime1 + ".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime1 + "/RF2Release/\n" +
			INTERNATIONAL_RELEASE + effectiveTime1 + "/RF2Release/Full/\n" +
			INTERNATIONAL_RELEASE + effectiveTime1 + "/RF2Release/Full/Refset/\n" +
			INTERNATIONAL_RELEASE + effectiveTime1 + "/RF2Release/Full/Refset/Language/\n" +
			INTERNATIONAL_RELEASE + effectiveTime1 + "/RF2Release/Full/Refset/Language/der2_cRefset_LanguageFull-en_INT_" + effectiveTime1 + ".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime1 + "/RF2Release/Full/Refset/Map/\n" +
			INTERNATIONAL_RELEASE + effectiveTime1 + "/RF2Release/Full/Refset/Map/der2_sRefset_SimpleMapFull_INT_" + effectiveTime1 + ".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime1 + "/RF2Release/Full/Terminology/\n" +
			INTERNATIONAL_RELEASE + effectiveTime1 + "/RF2Release/Full/Terminology/sct2_Concept_Full_INT_"+ effectiveTime1 +".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime1 + "/RF2Release/Full/Terminology/sct2_Description_Full-en_INT_"+ effectiveTime1 +".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime1 + "/RF2Release/Full/Terminology/sct2_TextDefinition_Full-en_INT_"+ effectiveTime1 +".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime1 + "/RF2Release/Full/Terminology/sct2_Relationship_Full_INT_"+ effectiveTime1 +".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime1 + "/RF2Release/Full/Terminology/sct2_StatedRelationship_Full_INT_"+ effectiveTime1 +".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime1 + "/RF2Release/Snapshot/\n" +
			INTERNATIONAL_RELEASE + effectiveTime1 + "/RF2Release/Snapshot/Refset/\n" +
			INTERNATIONAL_RELEASE + effectiveTime1 + "/RF2Release/Snapshot/Refset/Language/\n" +
			INTERNATIONAL_RELEASE + effectiveTime1 + "/RF2Release/Snapshot/Refset/Language/der2_cRefset_LanguageSnapshot-en_INT_"+ effectiveTime1 +".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime1 + "/RF2Release/Snapshot/Refset/Map/\n" +
			INTERNATIONAL_RELEASE + effectiveTime1 + "/RF2Release/Snapshot/Refset/Map/der2_sRefset_SimpleMapSnapshot_INT_" + effectiveTime1 + ".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime1 + "/RF2Release/Snapshot/Terminology/\n" +
			INTERNATIONAL_RELEASE + effectiveTime1 + "/RF2Release/Snapshot/Terminology/sct2_Concept_Snapshot_INT_"+ effectiveTime1 +".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime1 + "/RF2Release/Snapshot/Terminology/sct2_Description_Snapshot-en_INT_"+ effectiveTime1 +".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime1 + "/RF2Release/Snapshot/Terminology/sct2_TextDefinition_Snapshot-en_INT_"+ effectiveTime1 +".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime1 + "/RF2Release/Snapshot/Terminology/sct2_Relationship_Snapshot_INT_"+ effectiveTime1 +".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime1 + "/RF2Release/Snapshot/Terminology/sct2_StatedRelationship_Snapshot_INT_"+ effectiveTime1 +".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime1 + "/RF2Release/Delta/\n" +
			INTERNATIONAL_RELEASE + effectiveTime1 + "/RF2Release/Delta/Refset/\n" +
			INTERNATIONAL_RELEASE + effectiveTime1 + "/RF2Release/Delta/Refset/Language/\n" +
			INTERNATIONAL_RELEASE + effectiveTime1 + "/RF2Release/Delta/Refset/Language/der2_cRefset_LanguageDelta-en_INT_"+ effectiveTime1 + ".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime1 + "/RF2Release/Delta/Refset/Map/\n" +
			INTERNATIONAL_RELEASE + effectiveTime1 + "/RF2Release/Delta/Refset/Map/der2_sRefset_SimpleMapDelta_INT_" + effectiveTime1 + ".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime1 + "/RF2Release/Delta/Terminology/\n" +
			INTERNATIONAL_RELEASE + effectiveTime1 + "/RF2Release/Delta/Terminology/sct2_Concept_Delta_INT_"+ effectiveTime1 +".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime1 + "/RF2Release/Delta/Terminology/sct2_Description_Delta-en_INT_"+ effectiveTime1 +".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime1 + "/RF2Release/Delta/Terminology/sct2_TextDefinition_Delta-en_INT_"+ effectiveTime1 +".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime1 + "/RF2Release/Delta/Terminology/sct2_Relationship_Delta_INT_"+ effectiveTime1 +".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime1 + "/RF2Release/Delta/Terminology/sct2_StatedRelationship_Delta_INT_"+ effectiveTime1 +".txt";
		executeAndVerfiyResults(effectiveTime1, expectedZipEntries1, false);
	}

	private void executeAndVerfiyResults(final String releaseDate, final String expectedZipEntries, final boolean isBeta) throws Exception {
		final String buildURL1 = integrationTestHelper.createBuild();
		integrationTestHelper.triggerBuild(buildURL1);
		integrationTestHelper.publishOutput(buildURL1);

		// Assert first release output expectations
		final String expectedZipFilename = "SnomedCT_Release_INT_"+releaseDate+".zip";
		final ZipFile zipFile = integrationTestHelper.testZipNameAndEntryNames(buildURL1, expectedZipFilename, expectedZipEntries, getClass());
		integrationTestHelper.assertZipContents("expectedoutput", zipFile, getClass(), isBeta);
	}

	private void loadDeltaFilesToInputDirectory(final String releaseDate, boolean isBeta) throws Exception {
		if (isBeta) {
			integrationTestHelper.uploadManifest("core_manifest_" + "beta_" + releaseDate+".xml", getClass());
		} else {
			integrationTestHelper.uploadManifest("core_manifest_" + releaseDate+".xml", getClass());
		}
		integrationTestHelper.uploadDeltaInputFile("rel2_Concept_Delta_INT_" + releaseDate + ".txt", getClass());
		integrationTestHelper.uploadDeltaInputFile("rel2_Description_Delta-en_INT_"+releaseDate +".txt", getClass());
		integrationTestHelper.uploadDeltaInputFile("rel2_TextDefinition_Delta-en_INT_"+releaseDate +".txt", getClass());
		integrationTestHelper.uploadDeltaInputFile("rel2_StatedRelationship_Delta_INT_"+releaseDate +".txt", getClass());
		integrationTestHelper.uploadDeltaInputFile("rel2_cRefset_LanguageDelta-en_INT_" + releaseDate +".txt", getClass());
		integrationTestHelper.uploadDeltaInputFile("rel2_sRefset_SimpleMapDelta_INT_" + releaseDate +".txt", getClass());
		integrationTestHelper.uploadDeltaInputFile("rel2_Relationship_Delta_INT_" + releaseDate +".txt", getClass());
	}
	
	@Test
	@Ignore
	public void testBetaFirstRelease() throws Exception {
		
		integrationTestHelper.loginAsManager();
		integrationTestHelper.createTestProductStructure();
		
		//config assertion tests
		integrationTestHelper.setAssertionTestConfigProperty(ProductService.ASSERTION_GROUP_NAMES, "Test Assertion Group");
		integrationTestHelper.setAssertionTestConfigProperty(ProductService.PREVIOUS_INTERNATIONAL_RELEASE, "20140731");

		// Perform first time release
		integrationTestHelper.setFirstTimeRelease(true);
		boolean isBeta = true;
		integrationTestHelper.setBetaRelease(isBeta);
		integrationTestHelper.setCreateInferredRelationships(true);
		integrationTestHelper.setCreateLegacyIds(true);
		final String effectiveTime1 = "20140131";
		integrationTestHelper.setEffectiveTime(effectiveTime1);
		integrationTestHelper.setReadmeHeader("This is the readme for the first release © 2002-{readmeEndDate}.\\nTable of contents:\\n");
		integrationTestHelper.setReadmeEndDate("2014");
		loadDeltaFilesToInputDirectory(effectiveTime1, isBeta);
		final String expectedZipEntries1 =
			INTERNATIONAL_RELEASE + effectiveTime1 + "/\n" +
			INTERNATIONAL_RELEASE + effectiveTime1 + "/Readme_" + effectiveTime1 + ".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime1 + "/RF2Release/\n" +
			INTERNATIONAL_RELEASE + effectiveTime1 + "/RF2Release/Full/\n" +
			INTERNATIONAL_RELEASE + effectiveTime1 + "/RF2Release/Full/Refset/\n" +
			INTERNATIONAL_RELEASE + effectiveTime1 + "/RF2Release/Full/Refset/Language/\n" +
			INTERNATIONAL_RELEASE + effectiveTime1 + "/RF2Release/Full/Refset/Language/xder2_cRefset_LanguageFull-en_INT_" + effectiveTime1 + ".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime1 + "/RF2Release/Full/Refset/Map/\n" +
			INTERNATIONAL_RELEASE + effectiveTime1 + "/RF2Release/Full/Refset/Map/xder2_sRefset_SimpleMapFull_INT_" + effectiveTime1 + ".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime1 + "/RF2Release/Full/Terminology/\n" +
			INTERNATIONAL_RELEASE + effectiveTime1 + "/RF2Release/Full/Terminology/xsct2_Concept_Full_INT_"+ effectiveTime1 +".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime1 + "/RF2Release/Full/Terminology/xsct2_Description_Full-en_INT_"+ effectiveTime1 +".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime1 + "/RF2Release/Full/Terminology/xsct2_TextDefinition_Full-en_INT_"+ effectiveTime1 +".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime1 + "/RF2Release/Full/Terminology/xsct2_Relationship_Full_INT_"+ effectiveTime1 +".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime1 + "/RF2Release/Full/Terminology/xsct2_StatedRelationship_Full_INT_"+ effectiveTime1 +".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime1 + "/RF2Release/Snapshot/\n" +
			INTERNATIONAL_RELEASE + effectiveTime1 + "/RF2Release/Snapshot/Refset/\n" +
			INTERNATIONAL_RELEASE + effectiveTime1 + "/RF2Release/Snapshot/Refset/Language/\n" +
			INTERNATIONAL_RELEASE + effectiveTime1 + "/RF2Release/Snapshot/Refset/Language/xder2_cRefset_LanguageSnapshot-en_INT_"+ effectiveTime1 +".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime1 + "/RF2Release/Snapshot/Refset/Map/\n" +
			INTERNATIONAL_RELEASE + effectiveTime1 + "/RF2Release/Snapshot/Refset/Map/xder2_sRefset_SimpleMapSnapshot_INT_" + effectiveTime1 + ".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime1 + "/RF2Release/Snapshot/Terminology/\n" +
			INTERNATIONAL_RELEASE + effectiveTime1 + "/RF2Release/Snapshot/Terminology/xsct2_Concept_Snapshot_INT_"+ effectiveTime1 +".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime1 + "/RF2Release/Snapshot/Terminology/xsct2_Description_Snapshot-en_INT_"+ effectiveTime1 +".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime1 + "/RF2Release/Snapshot/Terminology/xsct2_TextDefinition_Snapshot-en_INT_"+ effectiveTime1 +".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime1 + "/RF2Release/Snapshot/Terminology/xsct2_Relationship_Snapshot_INT_"+ effectiveTime1 +".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime1 + "/RF2Release/Snapshot/Terminology/xsct2_StatedRelationship_Snapshot_INT_"+ effectiveTime1 +".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime1 + "/RF2Release/Delta/\n" +
			INTERNATIONAL_RELEASE + effectiveTime1 + "/RF2Release/Delta/Refset/\n" +
			INTERNATIONAL_RELEASE + effectiveTime1 + "/RF2Release/Delta/Refset/Language/\n" +
			INTERNATIONAL_RELEASE + effectiveTime1 + "/RF2Release/Delta/Refset/Language/xder2_cRefset_LanguageDelta-en_INT_"+ effectiveTime1 + ".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime1 + "/RF2Release/Delta/Refset/Map/\n" +
			INTERNATIONAL_RELEASE + effectiveTime1 + "/RF2Release/Delta/Refset/Map/xder2_sRefset_SimpleMapDelta_INT_" + effectiveTime1 + ".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime1 + "/RF2Release/Delta/Terminology/\n" +
			INTERNATIONAL_RELEASE + effectiveTime1 + "/RF2Release/Delta/Terminology/xsct2_Concept_Delta_INT_"+ effectiveTime1 +".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime1 + "/RF2Release/Delta/Terminology/xsct2_Description_Delta-en_INT_"+ effectiveTime1 +".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime1 + "/RF2Release/Delta/Terminology/xsct2_TextDefinition_Delta-en_INT_"+ effectiveTime1 +".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime1 + "/RF2Release/Delta/Terminology/xsct2_Relationship_Delta_INT_"+ effectiveTime1 +".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime1 + "/RF2Release/Delta/Terminology/xsct2_StatedRelationship_Delta_INT_"+ effectiveTime1 +".txt";
		executeAndVerfiyResults(effectiveTime1, expectedZipEntries1, isBeta);
	}

	@Test
	@Ignore
	//TODO investigate why these tests are failing when run together. The actual results seem be get overwritten.
	public void testSubsequentBetaRelease() throws Exception {
		
		firstTimeRelease();

		Thread.sleep(1000);

		//delete previous input files
		integrationTestHelper.deletePreviousTxtInputFiles();
		integrationTestHelper.setFirstTimeRelease(false);
		boolean isBeta = true;
		integrationTestHelper.setBetaRelease(isBeta);
		final String effectiveTime2 = "20140731";
		integrationTestHelper.setEffectiveTime(effectiveTime2);
		integrationTestHelper.setReadmeHeader("This is the readme for the second release © 2002-{readmeEndDate}.\\nTable of contents:\\n");
		integrationTestHelper.setReadmeEndDate("2015");
		//get previous published files
		final String previousPublishedPackage = integrationTestHelper.getPreviousPublishedPackage();
		Assert.assertEquals("SnomedCT_Release_INT_20140131.zip", previousPublishedPackage);
		integrationTestHelper.setPreviousPublishedPackage(previousPublishedPackage);
		integrationTestHelper.setNewRF2InputFiles("rel2_Refset_SimpleDelta_INT_20140731.txt");
		loadDeltaFilesToInputDirectory(effectiveTime2, false);
		integrationTestHelper.uploadDeltaInputFile("rel2_Refset_SimpleDelta_INT_20140731.txt", getClass());
		loadDeltaFilesToInputDirectory(effectiveTime2, isBeta);

		final String expectedZipEntries2 =
			INTERNATIONAL_RELEASE + effectiveTime2 + "/\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/Readme_" + effectiveTime2 + ".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Full/\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Full/Refset/\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Full/Refset/Language/\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Full/Refset/Language/xder2_cRefset_LanguageFull-en_INT_" + effectiveTime2 + ".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Full/Refset/Map/\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Full/Refset/Map/xder2_sRefset_SimpleMapFull_INT_" + effectiveTime2 + ".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Full/Refset/Map/xder2_Refset_SimpleFull_INT_" + effectiveTime2 + ".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Full/Terminology/\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Full/Terminology/xsct2_Concept_Full_INT_"+ effectiveTime2 +".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Full/Terminology/xsct2_Description_Full-en_INT_"+ effectiveTime2 +".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Full/Terminology/xsct2_TextDefinition_Full-en_INT_"+ effectiveTime2 +".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Full/Terminology/xsct2_Relationship_Full_INT_"+ effectiveTime2 +".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Full/Terminology/xsct2_StatedRelationship_Full_INT_"+ effectiveTime2 +".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Snapshot/\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Snapshot/Refset/\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Snapshot/Refset/Language/\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Snapshot/Refset/Language/xder2_cRefset_LanguageSnapshot-en_INT_"+ effectiveTime2 +".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Snapshot/Refset/Map/\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Snapshot/Refset/Map/xder2_sRefset_SimpleMapSnapshot_INT_" + effectiveTime2 + ".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Snapshot/Refset/Map/xder2_Refset_SimpleSnapshot_INT_" + effectiveTime2 + ".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Snapshot/Terminology/\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Snapshot/Terminology/xsct2_Concept_Snapshot_INT_"+ effectiveTime2 +".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Snapshot/Terminology/xsct2_Description_Snapshot-en_INT_"+ effectiveTime2 +".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Snapshot/Terminology/xsct2_TextDefinition_Snapshot-en_INT_"+ effectiveTime2 +".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Snapshot/Terminology/xsct2_Relationship_Snapshot_INT_"+ effectiveTime2 +".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Snapshot/Terminology/xsct2_StatedRelationship_Snapshot_INT_"+ effectiveTime2 +".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Delta/\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Delta/Refset/\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Delta/Refset/Language/\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Delta/Refset/Language/xder2_cRefset_LanguageDelta-en_INT_"+ effectiveTime2 + ".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Delta/Refset/Map/\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Delta/Refset/Map/xder2_sRefset_SimpleMapDelta_INT_" + effectiveTime2 + ".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Delta/Refset/Map/xder2_Refset_SimpleDelta_INT_" + effectiveTime2 + ".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Delta/Terminology/\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Delta/Terminology/xsct2_Concept_Delta_INT_"+ effectiveTime2 +".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Delta/Terminology/xsct2_Description_Delta-en_INT_"+ effectiveTime2 +".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Delta/Terminology/xsct2_TextDefinition_Delta-en_INT_"+ effectiveTime2 +".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Delta/Terminology/xsct2_Relationship_Delta_INT_"+ effectiveTime2 +".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Delta/Terminology/xsct2_StatedRelationship_Delta_INT_"+ effectiveTime2 +".txt";

		// This code extremely sensitive to timing issues because IdAssignmentBIOfflineDemoImpl only has one counter being accessed
		// by multiple streams.
		executeAndVerfiyResults(effectiveTime2, expectedZipEntries2, isBeta );
	}
}

package org.ihtsdo.buildcloud.integration.corecomponents;

import java.util.zip.ZipFile;

import org.ihtsdo.buildcloud.core.service.manager.ReleaseBuildManager;
import org.ihtsdo.buildcloud.rest.controller.AbstractControllerTest;
import org.ihtsdo.buildcloud.rest.controller.helper.IntegrationTestHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

public class CoreComponentsTestIntegration extends AbstractControllerTest {

	private static final String INTERNATIONAL_RELEASE = "SnomedCT_Release_INT_";

	private IntegrationTestHelper integrationTestHelper;

	@Autowired
	private ReleaseBuildManager buildManager;


	@Override
	@BeforeEach
	public void setup() throws Exception {
		super.setup();
		integrationTestHelper = new IntegrationTestHelper(mockMvc,"CoreComponentsTest");
	}


	@Override
	@AfterEach
	public void tearDown() {
		super.tearDown();
	}

	@Test
	public void testCoreMultipleReleases() throws Exception {
		firstTimeRelease();

		Thread.sleep(1000);

		integrationTestHelper.setFirstTimeRelease(false);
		
		final String effectiveTime2 = "20140731";
		integrationTestHelper.setEffectiveTime(effectiveTime2);
		integrationTestHelper.setReadmeHeader("This is the readme for the second release © 2002-{readmeEndDate}.\\nTable of contents:\\n");
		integrationTestHelper.setReadmeEndDate("2014");
		//get previous published files
		final String previousPublishedPackage = integrationTestHelper.getPreviousPublishedPackage();
		assertEquals("SnomedCT_Release_INT_20140131.zip", previousPublishedPackage);
		integrationTestHelper.setPreviousPublishedPackage(previousPublishedPackage);
		
		integrationTestHelper.setNewRF2InputFiles("rel2_Refset_SimpleDelta_INT_20140731.txt|rel2_sRefset_OWLOntologyDelta_INT_20140731.txt|rel2_RelationshipConcreteValues_Delta_INT_20140731.txt");
		loadManifest(effectiveTime2, false);
		integrationTestHelper.setBetaRelease(false);

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
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Full/Refset/Content/\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Full/Refset/Content/der2_Refset_SimpleFull_INT_" + effectiveTime2 + ".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Full/Terminology/\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Full/Terminology/sct2_Concept_Full_INT_" + effectiveTime2 + ".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Full/Terminology/sct2_Description_Full-en_INT_" + effectiveTime2 + ".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Full/Terminology/sct2_TextDefinition_Full-en_INT_" + effectiveTime2 + ".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Full/Terminology/sct2_Relationship_Full_INT_" + effectiveTime2 + ".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Full/Terminology/sct2_RelationshipConcreteValues_Full_INT_"+ effectiveTime2 + ".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Full/Terminology/sct2_StatedRelationship_Full_INT_"+ effectiveTime2 + ".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Full/Terminology/sct2_sRefset_OWLOntologyFull_INT_"+ effectiveTime2 + ".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Snapshot/\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Snapshot/Refset/\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Snapshot/Refset/Language/\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Snapshot/Refset/Language/der2_cRefset_LanguageSnapshot-en_INT_"+ effectiveTime2 +".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Snapshot/Refset/Map/\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Snapshot/Refset/Map/der2_sRefset_SimpleMapSnapshot_INT_" + effectiveTime2 + ".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Snapshot/Refset/Content/\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Snapshot/Refset/Content/der2_Refset_SimpleSnapshot_INT_" + effectiveTime2 + ".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Snapshot/Terminology/\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Snapshot/Terminology/sct2_Concept_Snapshot_INT_"+ effectiveTime2 + ".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Snapshot/Terminology/sct2_Description_Snapshot-en_INT_"+ effectiveTime2 + ".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Snapshot/Terminology/sct2_TextDefinition_Snapshot-en_INT_"+ effectiveTime2 + ".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Snapshot/Terminology/sct2_Relationship_Snapshot_INT_"+ effectiveTime2 + ".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Snapshot/Terminology/sct2_RelationshipConcreteValues_Snapshot_INT_"+ effectiveTime2 + ".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Snapshot/Terminology/sct2_StatedRelationship_Snapshot_INT_"+ effectiveTime2 + ".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Snapshot/Terminology/sct2_sRefset_OWLOntologySnapshot_INT_"+ effectiveTime2 + ".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Delta/\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Delta/Refset/\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Delta/Refset/Language/\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Delta/Refset/Language/der2_cRefset_LanguageDelta-en_INT_"+ effectiveTime2 + ".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Delta/Refset/Map/\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Delta/Refset/Map/der2_sRefset_SimpleMapDelta_INT_" + effectiveTime2 + ".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Delta/Refset/Content/\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Delta/Refset/Content/der2_Refset_SimpleDelta_INT_" + effectiveTime2 + ".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Delta/Terminology/\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Delta/Terminology/sct2_Concept_Delta_INT_"+ effectiveTime2 + ".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Delta/Terminology/sct2_Description_Delta-en_INT_"+ effectiveTime2 + ".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Delta/Terminology/sct2_TextDefinition_Delta-en_INT_"+ effectiveTime2 + ".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Delta/Terminology/sct2_Relationship_Delta_INT_"+ effectiveTime2 + ".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Delta/Terminology/sct2_RelationshipConcreteValues_Delta_INT_"+ effectiveTime2 + ".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Delta/Terminology/sct2_StatedRelationship_Delta_INT_"+ effectiveTime2 + ".txt\n" +
			INTERNATIONAL_RELEASE + effectiveTime2 + "/RF2Release/Delta/Terminology/sct2_sRefset_OWLOntologyDelta_INT_"+ effectiveTime2 + ".txt"; 

		// This code extremely sensitive to timing issues because IdAssignmentBIOfflineDemoImpl only has one counter being accessed
		// by multiple streams.
		executeAndVerifyResults(effectiveTime2, expectedZipEntries2, false );

	}

	private void firstTimeRelease() throws Exception {
		integrationTestHelper.createTestProductStructure();

		// Perform first time release
		integrationTestHelper.setFirstTimeRelease(true);
		integrationTestHelper.setCreateLegacyIds(true);
		final String effectiveTime1 = "20140131";
		integrationTestHelper.setEffectiveTime(effectiveTime1);
		integrationTestHelper.setReadmeHeader("This is the readme for the first release © 2002-{readmeEndDate}.\\nTable of contents:\\n");
		integrationTestHelper.setReadmeEndDate("2014");
		// add manifest
		loadManifest(effectiveTime1, false);

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
		executeAndVerifyResults(effectiveTime1, expectedZipEntries1, false);
	}

	private void executeAndVerifyResults(final String releaseDate, final String expectedZipEntries, final boolean isBeta) throws Exception {
		// create a build
		String buildUrl = integrationTestHelper.createBuild(releaseDate);

		// load input files
		loadDeltaFilesToInputDirectory(releaseDate);
		integrationTestHelper.scheduleBuild(buildUrl);

		// wait until the build is completed
		integrationTestHelper.waitUntilCompleted(buildUrl);
		// Assert first release output expectations
		final String expectedZipFilename = "SnomedCT_Release_INT_" + releaseDate + ".zip";
		final ZipFile zipFile = integrationTestHelper.testZipNameAndEntryNames(buildUrl, expectedZipFilename, expectedZipEntries, getClass());
		integrationTestHelper.assertZipContents("expectedoutput", zipFile, getClass(), isBeta);
		integrationTestHelper.publishOutput(buildUrl);
	}


	private void loadManifest(final String releaseDate, boolean isBeta) throws Exception {
		if (isBeta) {
			integrationTestHelper.uploadManifest("core_manifest_" + "beta_" + releaseDate+".xml", getClass());
		} else {
			integrationTestHelper.uploadManifest("core_manifest_" + releaseDate+".xml", getClass());
		}
	}

	private void loadDeltaFilesToInputDirectory(final String releaseDate) throws Exception {
		integrationTestHelper.uploadDeltaInputFile("rel2_Concept_Delta_INT_" + releaseDate + ".txt", getClass());
		integrationTestHelper.uploadDeltaInputFile("rel2_Description_Delta-en_INT_"+releaseDate +".txt", getClass());
		integrationTestHelper.uploadDeltaInputFile("rel2_TextDefinition_Delta-en_INT_"+releaseDate +".txt", getClass());
		integrationTestHelper.uploadDeltaInputFile("rel2_StatedRelationship_Delta_INT_"+releaseDate +".txt", getClass());
		integrationTestHelper.uploadDeltaInputFile("rel2_cRefset_LanguageDelta-en_INT_" + releaseDate +".txt", getClass());
		integrationTestHelper.uploadDeltaInputFile("rel2_sRefset_SimpleMapDelta_INT_" + releaseDate +".txt", getClass());
		integrationTestHelper.uploadDeltaInputFile("rel2_Relationship_Delta_INT_" + releaseDate +".txt", getClass());

		// The following files are for the second release build testing
		if ("20140731".equals(releaseDate)) {
			integrationTestHelper.uploadDeltaInputFile("rel2_Refset_SimpleDelta_INT_20140731.txt", getClass());
			integrationTestHelper.uploadDeltaInputFile("rel2_sRefset_OWLOntologyDelta_INT_20140731.txt", getClass());
			integrationTestHelper.uploadDeltaInputFile("rel2_RelationshipConcreteValues_Delta_INT_20140731.txt", getClass());
		}
	}
	
}

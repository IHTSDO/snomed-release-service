package org.ihtsdo.buildcloud.core.service.validation.precondition;

import static org.junit.jupiter.api.Assertions.*;

import org.ihtsdo.buildcloud.core.entity.PreConditionCheckReport;
import org.ihtsdo.buildcloud.core.entity.PreConditionCheckReport.State;
import org.ihtsdo.otf.rest.exception.ResourceNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class InputFilesExistenceCheckTest extends PreconditionCheckTest {

	@Autowired
	private InputFilesExistenceCheck inputFilesCheck;

	@Override
	@BeforeEach
	public void setup() throws Exception {
		super.setup();
		manager = new PreconditionManager(false).preconditionChecks(inputFilesCheck);
	}

	@Test
	public void checkInputFilesNotExisting() throws Exception {
		loadManifest("valid_manifest.xml");

		addEmptyFileToInputDirectory("rel2_StatedRelationship_Delta_INT_20140731.txt");
		addEmptyFileToInputDirectory("rel2_Relationship_Delta_INT_20140731.txt");

		final PreConditionCheckReport report = runPreConditionCheck(InputFilesExistenceCheck.class);
		final State actualResult = report.getResult();
		assertEquals(State.FATAL, actualResult);
		final String expectedMsg = "The input files directory doesn't contain the following files required by the manifest.xml:"
				+ " rel2_Refset_SimpleDelta_INT_20140731.txt.";
		assertEquals(expectedMsg, report.getMessage());
	}

	@Test
	public void checkInputFilesNotMatchingManifest() throws Exception {
		loadManifest("valid_manifest.xml");

		addEmptyFileToInputDirectory("der2_Refset_SimpleDelta_INT_20140731.txt");
		addEmptyFileToInputDirectory("rel2_StatedRelationship_Delta_INT_20140731.txt");
		addEmptyFileToInputDirectory("rel2_Relationship_Delta_INT_20140731.txt");

		final PreConditionCheckReport report = runPreConditionCheck(InputFilesExistenceCheck.class);
		final State actualResult = report.getResult();
		assertEquals(State.FATAL, actualResult);
		final String expectedMsg = "The input files directory doesn't contain the following files required by the manifest.xml:"
				+ " rel2_Refset_SimpleDelta_INT_20140731.txt.";
		assertEquals(expectedMsg, report.getMessage());
	}

	@Test
	public void checkInputFilesMatchingManifest() throws Exception {
		loadManifest("august_release_manifest.xml");

		addEmptyFileToInputDirectory("rel2_Refset_SimpleDelta_INT_20140831.txt");
		addEmptyFileToInputDirectory("rel2_StatedRelationship_Delta_INT_20140731.txt");
		addEmptyFileToInputDirectory("rel2_Relationship_Delta_INT_20140731.txt");

		final PreConditionCheckReport report = runPreConditionCheck(InputFilesExistenceCheck.class);
		final State actualResult = report.getResult();
		assertEquals(State.PASS, actualResult);
	}

	@Test
	public void checkInputFilesMatchingCoreRefSetManifest() throws Exception {
		loadManifest("valid_core_manifest.xml");
		addEmptyFileToInputDirectory("rel2_cRefset_LanguageDelta-en_INT_20140731.txt");
		addEmptyFileToInputDirectory("rel2_Concept_Delta_INT_20140731.txt");
		addEmptyFileToInputDirectory("rel2_Description_Delta-en_INT_20140731.txt");
		addEmptyFileToInputDirectory("rel2_StatedRelationship_Delta_INT_20140731.txt");
		addEmptyFileToInputDirectory("rel2_Relationship_Delta_INT_20140731.txt");
		addEmptyFileToInputDirectory("rel2_TextDefinition_Delta-en_INT_20140731.txt");
		addEmptyFileToInputDirectory("zres2_icRefset_OrderedTypeFull_INT_20140731.txt");
		addEmptyFileToInputDirectory("Readme_US_EN_20140731.txt");
		addEmptyFileToInputDirectory("doc_SnomedCTReleaseNotes_Current-en-US_INT_20140731.pdf");

		final PreConditionCheckReport report = runPreConditionCheck(InputFilesExistenceCheck.class);
		System.out.println("Build report");
		System.out.println(report.getMessage());
		final State actualResult = report.getResult();
		assertEquals(State.PASS, actualResult);
	}
	
//	@Test
//	public void testMissingStatedRelationship() throws Exception {
//		product.getBuildConfiguration().setJustPackage(false);
//		loadManifest("valid_core_manifest.xml");
//
//		addEmptyFileToInputDirectory("rel2_cRefset_LanguageDelta-en_INT_20140731.txt");
//		addEmptyFileToInputDirectory("rel2_Concept_Delta_INT_20140731.txt");
//		addEmptyFileToInputDirectory("rel2_Description_Delta-en_INT_20140731.txt");
//		addEmptyFileToInputDirectory("rel2_TextDefinition_Delta-en_INT_20140731.txt");
//		addEmptyFileToInputDirectory("zres2_icRefset_OrderedTypeFull_INT_20140731.txt");
//		addEmptyFileToInputDirectory("Readme_US_EN_20140731.txt");
//		addEmptyFileToInputDirectory("doc_SnomedCTReleaseNotes_Current-en-US_INT_20140731.pdf");
//
//		final PreConditionCheckReport report = runPreConditionCheck(InputFilesExistenceCheck.class);
//		final State actualResult = report.getResult();
//		assertEquals(State.FATAL, actualResult);
//	}
	
	
	@Test
	public void testMissingRf1Files() throws Exception{
		loadManifest("manifest_with_rf1.xml");
		final PreConditionCheckReport report = runPreConditionCheck(InputFilesExistenceCheck.class);
		final State actualResult = report.getResult();
		assertEquals(State.FATAL, actualResult);
	}

	@Test
	public void checkInputFilesNotMatchingBetaManifest() throws Exception {
		product.getBuildConfiguration().setBetaRelease(true);
		loadManifest("valid_manifest_betaRelease.xml");

		addEmptyFileToInputDirectory("rel2_StatedRelationship_Delta_INT_20140731.txt");
		addEmptyFileToInputDirectory("rel2_Relationship_Delta_INT_20140731.txt");

		final PreConditionCheckReport report = runPreConditionCheck(InputFilesExistenceCheck.class);
		final State actualResult = report.getResult();
		assertEquals(State.FATAL, actualResult);
		final String expectedMsg = "The input files directory doesn't contain the following files required by the manifest.xml: rel2_Refset_SimpleDelta_INT_20140731.txt.";
		assertEquals(expectedMsg, report.getMessage());
	}

	@Test
	public void checkInputFilesMatchedBetaManifest() throws Exception {
		product.getBuildConfiguration().setBetaRelease(true);
		loadManifest("valid_manifest_betaRelease.xml");

		addEmptyFileToInputDirectory("rel2_StatedRelationship_Delta_INT_20140731.txt");
		addEmptyFileToInputDirectory("rel2_Relationship_Delta_INT_20140731.txt");
		addEmptyFileToInputDirectory("rel2_Refset_SimpleDelta_INT_20140731.txt");

		final PreConditionCheckReport report = runPreConditionCheck(InputFilesExistenceCheck.class);
		final State actualResult = report.getResult();
		assertEquals(State.PASS, actualResult);
	}
	
	@Test
	public void missingInputFilesForJustPackage() throws Exception {
		product.getBuildConfiguration().setBetaRelease(true);
		product.getBuildConfiguration().setJustPackage(true);
		loadManifest("valid_manifest_betaRelease.xml");

		addEmptyFileToInputDirectory("rel2_Refset_SimpleDelta_INT_20140731.txt");

		final PreConditionCheckReport report = runPreConditionCheck(InputFilesExistenceCheck.class);
		final State actualResult = report.getResult();
		assertEquals(State.FATAL, actualResult);
		final String expectedMsg = "The input files directory doesn't contain the following files required by the manifest.xml:"
				+ " xder2_Refset_SimpleSnapshot_INT_20140731.txt,xder2_Refset_SimpleFull_INT_20140731.txt,xder2_Refset_SimpleDelta_INT_20140731.txt.";
		assertEquals(expectedMsg, report.getMessage());
	}
	
//	@Test
//	public void testMissingStatedRelationshipWhenClassifierIsActive() throws Exception {
//		loadManifest("valid_core_manifest.xml");
//
//		addEmptyFileToInputDirectory("rel2_cRefset_LanguageDelta-en_INT_20140731.txt");
//		addEmptyFileToInputDirectory("rel2_Concept_Delta_INT_20140731.txt");
//		addEmptyFileToInputDirectory("rel2_Description_Delta-en_INT_20140731.txt");
//		addEmptyFileToInputDirectory("rel2_TextDefinition_Delta-en_INT_20140731.txt");
//		addEmptyFileToInputDirectory("zres2_icRefset_OrderedTypeFull_INT_20140731.txt");
//		addEmptyFileToInputDirectory("rel2_Relationship_Delta_INT_20140731.txt");
//		addEmptyFileToInputDirectory("Readme_US_EN_20140731.txt");
//		addEmptyFileToInputDirectory("doc_SnomedCTReleaseNotes_Current-en-US_INT_20140731.pdf");
//
//		final PreConditionCheckReport report = runPreConditionCheck(InputFilesExistenceCheck.class);
//		final State actualResult = report.getResult();
//		assertEquals(State.FATAL, actualResult);
//		final String expectedMsg = "The input files directory doesn't contain the following files required by the manifest.xml: rel2_StatedRelationship_Delta_INT_20140731.txt.";
//		assertEquals(expectedMsg, report.getMessage());
//	}

	@Test
	public void testMissingRelationshipWhenClassifierIsActive() throws Exception {
		loadManifest("valid_core_manifest.xml");

		addEmptyFileToInputDirectory("rel2_cRefset_LanguageDelta-en_INT_20140731.txt");
		addEmptyFileToInputDirectory("rel2_Concept_Delta_INT_20140731.txt");
		addEmptyFileToInputDirectory("rel2_Description_Delta-en_INT_20140731.txt");
		addEmptyFileToInputDirectory("rel2_TextDefinition_Delta-en_INT_20140731.txt");
		addEmptyFileToInputDirectory("zres2_icRefset_OrderedTypeFull_INT_20140731.txt");
		addEmptyFileToInputDirectory("rel2_StatedRelationship_Delta_INT_20140731.txt");
		addEmptyFileToInputDirectory("Readme_US_EN_20140731.txt");
		addEmptyFileToInputDirectory("doc_SnomedCTReleaseNotes_Current-en-US_INT_20140731.pdf");

		final PreConditionCheckReport report = runPreConditionCheck(InputFilesExistenceCheck.class);
		final State actualResult = report.getResult();
		assertEquals(State.FAIL, actualResult);
		final String expectedMsg = "No relationship file is found in the input file directory.";
		assertEquals(expectedMsg, report.getMessage());
	}
	
	@Test
	public void testManifestFailedToBeParsed() throws Exception {
		loadManifest("invalid_manifest.xml");
		final PreConditionCheckReport report = runPreConditionCheck(InputFilesExistenceCheck.class);
		final State actualResult = report.getResult();
		assertEquals(State.FATAL, actualResult);
	}
	@AfterEach
	public void tearDown() throws ResourceNotFoundException {
		product = null;
	}

}

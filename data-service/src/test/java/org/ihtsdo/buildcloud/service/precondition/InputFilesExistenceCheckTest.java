package org.ihtsdo.buildcloud.service.precondition;

import static org.junit.Assert.assertEquals;

import org.ihtsdo.buildcloud.entity.PreConditionCheckReport;
import org.ihtsdo.buildcloud.entity.PreConditionCheckReport.State;
import org.ihtsdo.buildcloud.service.exception.ResourceNotFoundException;
import org.ihtsdo.buildcloud.test.TestUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class InputFilesExistenceCheckTest extends PreconditionCheckTest {
	@Autowired
	private InputFilesExistenceCheck inputFilesCheck;

	@Override
	@Before
	public void setup() throws Exception {
		super.setup();
		manager = new PreconditionManager().preconditionChecks(inputFilesCheck);
		TestUtils.setTestUser();
	}

	@Test
	public void checkInputFilesNotExisting() throws Exception {
		loadManifest("valid_manifest.xml");
		final PreConditionCheckReport report = runPreConditionCheck(InputFilesExistenceCheck.class);
		final State actualResult = report.getResult();
		assertEquals(State.FAIL, actualResult);
		final String expectedMsg = "The input files directory doesn't contain the following files required by the manifest.xml:"
				+ " rel2_Refset_SimpleDelta_INT_20140731.txt";
		assertEquals(expectedMsg, report.getMessage());
	}

	@Test
	public void checkInputFilesNotMatchingManifest() throws Exception {
		addEmptyFileToInputDirectory("der2_Refset_SimpleDelta_INT_20140731.txt");
		loadManifest("valid_manifest.xml");
		final PreConditionCheckReport report = runPreConditionCheck(InputFilesExistenceCheck.class);
		final State actualResult = report.getResult();
		assertEquals(State.FAIL, actualResult);
		final String expectedMsg = "The input files directory doesn't contain the following files required by the manifest.xml:"
				+ " rel2_Refset_SimpleDelta_INT_20140731.txt";
		assertEquals(expectedMsg, report.getMessage());
	}

	@Test
	public void checkInputFilesMatchingManifest() throws Exception {
		addEmptyFileToInputDirectory("rel2_Refset_SimpleDelta_INT_20140831.txt");
		loadManifest("august_release_manifest.xml");
		final PreConditionCheckReport report = runPreConditionCheck(InputFilesExistenceCheck.class);
		final State actualResult = report.getResult();
		Assert.assertEquals(State.PASS, actualResult);
	}

	@Test
	public void checkInputFilesMatchingCoreRefSetManifest() throws Exception {
		addEmptyFileToInputDirectory("rel2_cRefset_LanguageDelta-en_INT_20140731.txt");
		addEmptyFileToInputDirectory("rel2_Concept_Delta_INT_20140731.txt");
		addEmptyFileToInputDirectory("rel2_Description_Delta-en_INT_20140731.txt");
		addEmptyFileToInputDirectory("rel2_StatedRelationship_Delta_INT_20140731.txt");
		addEmptyFileToInputDirectory("rel2_TextDefinition_Delta-en_INT_20140731.txt");
		addEmptyFileToInputDirectory("zres2_icRefset_OrderedTypeFull_INT_20140731.txt");
		addEmptyFileToInputDirectory("Readme_US_EN_20140731.txt");
		addEmptyFileToInputDirectory("doc_SnomedCTReleaseNotes_Current-en-US_INT_20140731.pdf");
		loadManifest("valid_core_manifest.xml");
		final PreConditionCheckReport report = runPreConditionCheck(InputFilesExistenceCheck.class);
		final State actualResult = report.getResult();
		assertEquals(State.PASS, actualResult);
	}
	
	@Test
	public void testMissingStatedRelationship() throws Exception {
		addEmptyFileToInputDirectory("rel2_cRefset_LanguageDelta-en_INT_20140731.txt");
		addEmptyFileToInputDirectory("rel2_Concept_Delta_INT_20140731.txt");
		addEmptyFileToInputDirectory("rel2_Description_Delta-en_INT_20140731.txt");
		addEmptyFileToInputDirectory("rel2_TextDefinition_Delta-en_INT_20140731.txt");
		addEmptyFileToInputDirectory("zres2_icRefset_OrderedTypeFull_INT_20140731.txt");
		addEmptyFileToInputDirectory("Readme_US_EN_20140731.txt");
		addEmptyFileToInputDirectory("doc_SnomedCTReleaseNotes_Current-en-US_INT_20140731.pdf");
		loadManifest("valid_core_manifest.xml");
		final PreConditionCheckReport report = runPreConditionCheck(InputFilesExistenceCheck.class);
		final State actualResult = report.getResult();
		assertEquals(State.FAIL, actualResult);
		final String expectedMsg = "The input files directory doesn't contain the following files required by the manifest.xml: rel2_StatedRelationship_Delta_INT_20140731.txt";
		assertEquals(expectedMsg, report.getMessage());
	}
	
	
	@Test
	public void testMissingRf1Files() throws Exception{
		loadManifest("manifest_with_rf1.xml");
		final PreConditionCheckReport report = runPreConditionCheck(InputFilesExistenceCheck.class);
		final State actualResult = report.getResult();
		assertEquals(State.FAIL, actualResult);
		final String expectedMsg ="The input files directory doesn't contain the following files required by the manifest.xml: der1_SubsetMembers_es_INT_20140731.txt,der1_Subsets_es_INT_20140731.txt,"
				+ "sct1_Relationships_Core_INT_20140731.txt,sct1_descriptions_es_INT_20140731.txt,sct1_Concepts_Core_INT_20140731.txt,zres_SctLoincTechnologyPreview_INT_20140731.owl";
		assertEquals(expectedMsg, report.getMessage());
	}

	@Test
	public void checkInputFilesNotMatchingBetaManifest() throws Exception {
		product.getBuildConfiguration().setBetaRelease(true);
		loadManifest("valid_manifest_betaRelease.xml");
		final PreConditionCheckReport report = runPreConditionCheck(InputFilesExistenceCheck.class);
		final State actualResult = report.getResult();
		assertEquals(State.FAIL, actualResult);
		final String expectedMsg = "The input files directory doesn't contain the following files required by the manifest.xml: rel2_Refset_SimpleDelta_INT_20140731.txt";
		assertEquals(expectedMsg, report.getMessage());
	}
	
	

	@Test
	public void checkInputFilesMatchedBetaManifest() throws Exception {
		product.getBuildConfiguration().setBetaRelease(true);
		addEmptyFileToInputDirectory("rel2_Refset_SimpleDelta_INT_20140731.txt");
		loadManifest("valid_manifest_betaRelease.xml");
		final PreConditionCheckReport report = runPreConditionCheck(InputFilesExistenceCheck.class);
		final State actualResult = report.getResult();
		assertEquals(State.PASS, actualResult);
	}
	
	@After
	public void tearDown() throws ResourceNotFoundException {
		deleteFilesFromInputFileByPattern("*.txt");
		deleteFilesFromInputFileByPattern("*.pdf");
		deleteFilesFromInputFileByPattern("*.owl");
	}

}
